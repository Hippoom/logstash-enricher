package cn.scaleworks.bff4cmdb.vmware;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.*;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class VSphereApiLearningTest {

    @Test
    public void given_vm_host() throws MalformedURLException, RemoteException {

        String name = "vm1";

        ServiceInstance si = new ServiceInstance(new URL("https://192.168.11.105/sdk/vimService"), "administrator@thoughtworks.cn", "1qaz@WSX", true);

        Folder rootFolder = si.getRootFolder();

        InventoryNavigator inventoryNavigator = new InventoryNavigator(rootFolder);

        ManagedEntity[] virtualMachines = inventoryNavigator.searchManagedEntities("VirtualMachine");

        VirtualMachine vm = stream(virtualMachines).map(v -> (VirtualMachine) v)
                .filter(v -> name.equals(v.getGuest().getHostName()))
                .findFirst().get();

        List<String> datastores = stream(vm.getDatastores()).map(d -> d.getName()).collect(toList());



        ManagedEntity[] hostSystem = inventoryNavigator.searchManagedEntities("HostSystem");



        //TODO get host name by vm
        //
    }
}
