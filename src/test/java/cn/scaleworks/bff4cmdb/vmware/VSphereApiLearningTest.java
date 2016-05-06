package cn.scaleworks.bff4cmdb.vmware;

import com.vmware.vim25.mo.*;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.stream.Stream;

public class VSphereApiLearningTest {

    @Test
    public void given_vm_host() throws MalformedURLException, RemoteException {
        ServiceInstance si = new ServiceInstance(new URL("https://192.168.11.105/sdk/vimService"), "administrator@thoughtworks.cn", "1qaz@WSX", true);

        Folder rootFolder = si.getRootFolder();

        InventoryNavigator inventoryNavigator = new InventoryNavigator(rootFolder);

        VirtualMachine vm = (VirtualMachine) inventoryNavigator.searchManagedEntity("VirtualMachine", "New Virtual Machine");

        System.err.println("vm=" + vm.getName());
        System.err.println("vm host=" + vm.getGuest().getHostName());
        Stream<Datastore> datastoreStream = Arrays.stream(vm.getDatastores());
        datastoreStream.forEach(ds -> System.err.println("has data store which name is " + ds.getName()));


        //TODO get host name by vm
        //
    }
}
