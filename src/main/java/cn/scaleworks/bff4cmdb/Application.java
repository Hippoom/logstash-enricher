package cn.scaleworks.bff4cmdb;

import com.alibaba.fastjson.JSONObject;
import com.vmware.vim25.mo.*;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@RestController
@SpringBootApplication
public class Application {

    public static final String GROUP_DB_ID = "9";

    @RequestMapping(value = "/host/{name}/groups")
    protected List<Map<String, String>> findGroupsGivenHostName(@PathVariable String name) {
        //String url = "http://10.202.128.121/zabbix/api_jsonrpc.php";
        ZabbixApi zabbixApi = getZabbixApi();


        JSONObject filter = new JSONObject();

        filter.put("host", new String[]{name});


        Request getRequest = RequestBuilder.newBuilder()
                .method("host.get").paramEntry("selectGroups", "extend").paramEntry("filter", filter)
                .build();
        JSONObject getResponse = zabbixApi.call(getRequest);

        return getResponse.getJSONArray("result")
                .getJSONObject(0).getJSONArray("groups")
                .stream()
                .map(g -> (JSONObject) g)
                .map(g ->
                        new HashMap<String, String>() {
                            {
                                put("id", g.getString("groupid"));
                                put("name", g.getString("name"));
                            }
                        }
                    )
                .collect(toList());
    }

    private List<Map<String, String>> findHostsGivenGroups(String filter) {
        ZabbixApi zabbixApi = getZabbixApi();


        Request getRequest = RequestBuilder.newBuilder()
                .method("host.get").paramEntry("groupids", filter)
                .build();
        JSONObject getResponse = zabbixApi.call(getRequest);

        return getResponse.getJSONArray("result")
                .stream()
                .map(h -> (JSONObject) h)
                .map(h -> new HashMap<String, String>() {
                    {
                        put("id", h.getString("hostid"));
                        put("name", h.getString("name"));
                    }
                })
                .collect(toList());
    }

    private ZabbixApi getZabbixApi() {
        String url = "http://192.168.11.145/zabbix/api_jsonrpc.php";
        ZabbixApi zabbixApi = new DefaultZabbixApi(url);
        zabbixApi.init();

        boolean login = zabbixApi.login("Admin", "zabbix");
        return zabbixApi;
    }


    @RequestMapping(value = "/host/{name}/dependencies")
    protected List<Map<String, String>> findDependenciesGivenHostName(@PathVariable String name) throws MalformedURLException, RemoteException {
        ServiceInstance si = new ServiceInstance(new URL("https://192.168.11.105/sdk/vimService"), "administrator@thoughtworks.cn", "1qaz@WSX", true);

        Folder rootFolder = si.getRootFolder();

        InventoryNavigator inventoryNavigator = new InventoryNavigator(rootFolder);

        ManagedEntity[] virtualMachines = inventoryNavigator.searchManagedEntities("VirtualMachine");

        VirtualMachine vm = stream(virtualMachines).map(v -> (VirtualMachine) v)
                .filter(v -> name.equals(v.getGuest().getHostName()))
                .findFirst().get();
        List<Map<String, String>> groups = findGroupsGivenHostName(name);


        Stream<Map<String, String>> vmsBelongToSameBizGroups = groups.stream().filter(g -> g.get("name").startsWith("[BIZ]"))
                .map(g -> g.get("id"))
                //.map(id -> new String[]{id})
                .map(filter -> findHostsGivenGroups(filter))
                .flatMap(h -> h.stream());

        Stream<Map<String, String>> allDatabases = findHostsGivenGroups(GROUP_DB_ID).stream();

        List<Map<String, String>> vms = Stream.concat(vmsBelongToSameBizGroups, allDatabases).collect(Collectors.toList());

        Stream<Map<String, String>> databasesBelongToSameGroups = vms.stream().filter(i -> Collections.frequency(vms, i) > 1);

        Stream<Map<String, String>> datastores = stream(vm.getDatastores()).map(d -> new HashMap<String, String>() {
            {
                put("name", d.getName());
            }
        });

        return Stream.concat(databasesBelongToSameGroups, datastores).collect(Collectors.toSet()).stream().collect(toList());
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
