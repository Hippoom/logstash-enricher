package cn.scaleworks.bff4cmdb.vmware;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import static java.lang.String.format;

@Configuration
public class VmwareConfiguration {

    @ConfigurationProperties("vmware")
    @Bean
    protected VmwareProfile vmwareProfile() {
        return new VmwareProfile();
    }

    @Lazy//so that when the app launches, zabbix does not have to be alive
    @Bean
    protected InventoryNavigator vmwareInventoryNavigator(VmwareProfile vmwareProfile) throws RemoteException, MalformedURLException {
        ServiceInstance si = new ServiceInstance(new URL(format("%s/sdk/vimService", vmwareProfile.getBaseUrl())),
                vmwareProfile.getUsername(), vmwareProfile.getPassword(), true);

        Folder rootFolder = si.getRootFolder();

        return new InventoryNavigator(rootFolder);
    }


}
