package cn.scaleworks.bff4cmdb.vmware;

import cn.scaleworks.bff4cmdb.graph.MonitoredEntity;
import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import cn.scaleworks.bff4cmdb.graph.MonitoredGroupRepository;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

@Configuration
@ConditionalOnProperty("vmware.enabled")
@ConfigurationProperties("vmware")
@Data
@Slf4j
public class VmwareConfiguration {

    private String baseUrl;
    private String password;
    private String username;

    @Autowired
    private MonitoredGroupRepository monitoredGroupRepository;

    @Bean
    protected InventoryNavigator vmwareInventoryNavigator() throws RemoteException, MalformedURLException {
        ServiceInstance si = new ServiceInstance(new URL(format("%s/sdk/vimService", baseUrl)),
                username, password, true);

        Folder rootFolder = si.getRootFolder();

        return new InventoryNavigator(rootFolder);
    }

    @Bean
    protected ManagedEntityHolder vmwareManagedEntityHolder() throws MalformedURLException, RemoteException {
        return new ManagedEntityHolder(vmwareInventoryNavigator());
    }

    @Lazy
    @Bean
    protected MonitoredEntityRepository monitoredEntityRepository() throws RemoteException, MalformedURLException {
        ManagedEntityHolder managedEntityHolder = vmwareManagedEntityHolder();

        List<HostSystem> hostSystems = managedEntityHolder.getHostSystems();


        List<VirtualMachine> virtualMachines = managedEntityHolder.getVirtualMachines();

        List<Datastore> datastores = managedEntityHolder.getDatastores();


        Stream<MonitoredEntity> virtualMachineStream = virtualMachines.stream()
                .map(v -> {
                    String id = v.getGuest().getHostName();
                    String host = id;
                    String text = format("%s:%s", id, v.getGuest().getIpAddress());

                    MonitoredEntity virtualMachine = new MonitoredEntity(id, host, "vm", text);

                    Set<String> dependsOn = managedEntityHolder.getDsStream(v)
                            .map(d -> d.getName())
                            .collect(toSet());

                    getHostBy(v, hostSystems).ifPresent(h -> dependsOn.add(h.getName()));

                    virtualMachine.assignDependencies(dependsOn);
                    virtualMachine.assignGroups(monitoredGroupRepository.findGroupsByHostName(id).stream()
                            .map(g -> (String) g.get("name")).collect(Collectors.toSet()));
                    return virtualMachine;
                });

        Stream<MonitoredEntity> hostSystemStream = hostSystems.stream()
                .map(h -> {
                    String id = h.getName();
                    String host = id;
                    String text = id;

                    MonitoredEntity hostSystem = new MonitoredEntity(id, host, "vh", text);
                    hostSystem.assignDependencies(managedEntityHolder.getDsStream(h)
                            .map(d -> d.getName())
                            .collect(toSet()));
                    // usually we don't categorize vh into groups
                    return hostSystem;
                });

        Stream<MonitoredEntity> datastoreStream = datastores.stream()
                .map(ds -> {
                    String id = ds.getName();
                    String host = id;
                    String text = id;

                    MonitoredEntity datastore = new MonitoredEntity(id, host, "ds", text);
                    // usually datastore does not have dependencies
                    // usually we don't categorize vh into groups
                    return datastore;
                });

        Stream<MonitoredEntity> entityStream = Stream.concat(Stream.concat(virtualMachineStream, hostSystemStream), datastoreStream);

        Map<String, MonitoredEntity> entities = new HashMap();
        entityStream.forEach(e -> {
            entities.put(e.getId(), e);
        });
        MonitoredEntityRepository monitoredEntityRepository = new MonitoredEntityRepository();
        monitoredEntityRepository.setEntities(entities);
        return monitoredEntityRepository;
    }

    private Optional<HostSystem> getHostBy(VirtualMachine v, List<HostSystem> hostSystems) {
        return getHostSystemBy(v.getRuntime().getHost(), hostSystems);
    }


    private Optional<HostSystem> getHostSystemBy(ManagedObjectReference hostRef, List<HostSystem> hostSystems) {
        return hostSystems.stream().filter(h -> h.getMOR().getVal().equals(hostRef.getVal())).findFirst();
    }


}
