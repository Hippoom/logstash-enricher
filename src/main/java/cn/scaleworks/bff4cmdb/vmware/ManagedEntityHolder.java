package cn.scaleworks.bff4cmdb.vmware;


import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.VirtualMachine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Slf4j
public class ManagedEntityHolder {
    @Getter
    private List<HostSystem> hostSystems;

    @Getter
    private List<VirtualMachine> virtualMachines;

    @Getter
    private List<Datastore> datastores;

    private InventoryNavigator inventoryNavigator;

    public ManagedEntityHolder(InventoryNavigator inventoryNavigator) {
        this.inventoryNavigator = inventoryNavigator;
        reload();
    }

    @Scheduled(fixedRate = 5000)
    protected void reload() {
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
    }

    protected Stream<VirtualMachine> getVmStream(HostSystem h) {
        try {
            return stream(h.getVms());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    protected Stream<Datastore> getDsStream(HostSystem h) {
        try {
            return stream(h.getDatastores());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    protected Stream<Datastore> getDsStream(VirtualMachine h) {
        try {
            return stream(h.getDatastores());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Stream.empty();
        }
    }
}
