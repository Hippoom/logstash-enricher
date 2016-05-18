package cn.scaleworks.graph.vmware;

import cn.scaleworks.graph.core.MonitoredGroupRepository;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import static java.lang.String.format;

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
    protected ManagedEntityLoader vmwareManagedEntityHolder() throws MalformedURLException, RemoteException {
        ManagedEntityLoader managedEntityLoader = new ManagedEntityLoader();
        managedEntityLoader.setInventoryNavigator(vmwareInventoryNavigator());
        managedEntityLoader.setMonitoredGroupRepository(monitoredGroupRepository);
        return managedEntityLoader;
    }


}
