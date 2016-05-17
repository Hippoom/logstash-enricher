package cn.scaleworks.bff4cmdb.vmware;

import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import cn.scaleworks.bff4cmdb.graph.MonitoredGroupRepository;
import com.alibaba.fastjson.JSONObject;
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
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

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
    protected MonitoredEntityRepository monitoredEntityRepository(InventoryNavigator inventoryNavigator) throws RemoteException {
        List<HostSystem> hostSystems = Arrays.stream(inventoryNavigator.searchManagedEntities("HostSystem"))
                .map(h -> (HostSystem) h).collect(toList());


        List<VirtualMachine> virtualMachines = hostSystems.stream()
                .map(h -> getVmStream(h))
                .collect(() -> new ArrayList<>(),
                        (list, item) -> list.addAll(item.collect(toList())),
                        (list1, list2) -> list1.addAll(list2));

        List<Datastore> datastores = hostSystems.stream()
                .map(h -> {
                    try {
                        return Arrays.stream(h.getDatastores());
                    } catch (RemoteException e) {
                        log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
                        return Stream.<Datastore>empty();
                    }
                })
                .collect(() -> new ArrayList<>(),
                        (list, item) -> list.addAll(item.collect(toList())),
                        (list1, list2) -> list1.addAll(list2));


        Stream<JSONObject> virtualMachineStream = virtualMachines.stream()
                .map(v -> {
                    JSONObject virtualMachine = new JSONObject();
                    String id = v.getGuest().getHostName();
                    virtualMachine.put("id", id);
                    virtualMachine.put("type", "vm");
                    virtualMachine.put("text", format("%s:%s", id, v.getGuest().getIpAddress()));
                    try {
                        Stream<String> datastoreStream = Arrays.stream(v.getDatastores()).map(d -> d.getName());
                        List<String> dependsOn = datastoreStream.collect(toList());
                        getHostBy(v, hostSystems).ifPresent(h -> dependsOn.add(h.getName()));
                        virtualMachine.put("dependsOn", dependsOn);
                        virtualMachine.put("groups", monitoredGroupRepository.findGroupsByHostName(id).stream()
                                .map(g -> g.get("name")));
                    } catch (RemoteException e) {
                        log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
                    }
                    return virtualMachine;
                });

        Stream<JSONObject> hostSystemStream = hostSystems.stream()
                .map(h -> {
                    JSONObject hostSystem = new JSONObject();
                    hostSystem.put("id", h.getName());
                    hostSystem.put("type", "vh");
                    hostSystem.put("text", h.getName());
                    try {
                        hostSystem.put("dependsOn", Arrays.stream(h.getDatastores()).map(d -> d.getName()).collect(toList()));
                    } catch (RemoteException e) {
                        log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
                    }
                    return hostSystem;
                });

        Stream<JSONObject> datastoreStream = datastores.stream()
                .map(ds -> {
                    JSONObject datastore = new JSONObject();
                    datastore.put("id", ds.getName());
                    datastore.put("type", "ds");
                    datastore.put("text", ds.getName());
                    return datastore;
                });

        Stream<JSONObject> entityStream = Stream.concat(Stream.concat(virtualMachineStream, hostSystemStream), datastoreStream);

        Map<String, JSONObject> entities = new HashMap();
        entityStream.forEach(e -> {
            entities.put((String) e.get("id"), e);
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

    private Stream<VirtualMachine> getVmStream(HostSystem h) {
        try {
            return Arrays.stream(h.getVms());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

}
