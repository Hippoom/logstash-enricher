package cn.scaleworks.bff4cmdb.vmware;


import com.vmware.vim25.mo.InventoryNavigator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class ManagedEntityHolder {
    private InventoryNavigator inventoryNavigator;

    public ManagedEntityHolder(InventoryNavigator inventoryNavigator) {
        this.inventoryNavigator = inventoryNavigator;
    }

    @Scheduled(fixedRate = 5000)
    protected void reload() {
        log.debug("Begin to reload Managed Entities");
    }
}
