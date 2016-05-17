package cn.scaleworks.bff4cmdb.vmware;


import cn.scaleworks.bff4cmdb.graph.MonitoredEntity;
import cn.scaleworks.bff4cmdb.graph.MonitoredEntityLoader;
import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import cn.scaleworks.bff4cmdb.graph.MonitoredGroupRepository;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.VirtualMachine;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class ManagedEntityLoader implements MonitoredEntityLoader {
    @Getter
    private List<HostSystem> hostSystems;

    @Getter
    private List<VirtualMachine> virtualMachines;

    @Getter
    private List<Datastore> datastores;

    @Setter
    private InventoryNavigator inventoryNavigator;

    @Setter
    private MonitoredGroupRepository monitoredGroupRepository;

    @Autowired
    private MonitoredEntityRepository monitoredEntityRepository;

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void populate(MonitoredEntityRepository monitoredEntityRepository) {
        log.debug("Begin to reload Managed Entities");

        try {
            this.hostSystems = stream(inventoryNavigator.searchManagedEntities("HostSystem"))
                    .map(h -> (HostSystem) h).collect(toList());
        } catch (RemoteException e) {
            throw new IllegalStateException(format("Cannot connect to vmware due to %s", e.getMessage()), e);
        }

        this.virtualMachines = hostSystems.stream()
                .map(h -> getVmStream(h))
                .collect(() -> new ArrayList<>(),
                        (list, item) -> list.addAll(item.collect(toList())),
                        (list1, list2) -> list1.addAll(list2));

        this.datastores = hostSystems.stream()
                .map(h -> getDsStream(h))
                .collect(() -> new ArrayList<>(),
                        (list, item) -> list.addAll(item.collect(toList())),
                        (list1, list2) -> list1.addAll(list2));

        Stream<MonitoredEntity> virtualMachineStream = virtualMachines.stream()
                .map(v -> {
                    String id = v.getGuest().getHostName();
                    String host = id;
                    String text = format("%s:%s", id, v.getGuest().getIpAddress());

                    MonitoredEntity virtualMachine = new MonitoredEntity(id, host, "vm", text);

                    Set<String> dependsOn = getDsStream(v)
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
                    hostSystem.assignDependencies(getDsStream(h)
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

        monitoredEntityRepository.savePending(entityStream.collect(Collectors.toList()));
    }

    private Stream<VirtualMachine> getVmStream(HostSystem h) {
        try {
            return stream(h.getVms());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    private Stream<Datastore> getDsStream(HostSystem h) {
        try {
            return stream(h.getDatastores());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    private Stream<Datastore> getDsStream(VirtualMachine h) {
        try {
            return stream(h.getDatastores());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    private Optional<HostSystem> getHostBy(VirtualMachine v, List<HostSystem> hostSystems) {
        return getHostSystemBy(v.getRuntime().getHost(), hostSystems);
    }


    private Optional<HostSystem> getHostSystemBy(ManagedObjectReference hostRef, List<HostSystem> hostSystems) {
        return hostSystems.stream().filter(h -> h.getMOR().getVal().equals(hostRef.getVal())).findFirst();
    }

    @PostConstruct
    protected void register() {
        monitoredEntityRepository.register(this);
    }

}
