package cn.scaleworks.bff4cmdb;

import cn.scaleworks.bff4cmdb.zabbix.ZabbixProfile;
import com.alibaba.fastjson.JSONObject;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Slf4j
@RestController
@SpringBootApplication
public class Application {

    @Autowired
    private ZabbixProfile zabbixProfile;

    @Autowired
    private ZabbixApi zabbixApi;

    @Autowired
    private InventoryNavigator vmwareInventoryNavigator;

    @RequestMapping(value = "/host/{name}")
    private Map<String, Object> findHostGiven(@PathVariable String name) throws MalformedURLException, RemoteException {

        List<Map<String, String>> groups = findGroupsGivenHostName(name);
        List<Map<String, String>> dependencies = findDependenciesGivenHostName(name);

        return new HashMap() {
            {
                put("groups", groups);
                put("dependencies", dependencies);
            }
        };
    }

    private List<Map<String, String>> findGroupsGivenHostName(String name) {
        try {
            ZabbixApi zabbixApi = getZabbixApi();

            JSONObject filter = new JSONObject();
            filter.put("host", new String[]{name});

            Request getRequest = RequestBuilder.newBuilder()
                    .method("host.get")
                    .paramEntry("selectGroups", "extend")//so that we get groups
                    .paramEntry("filter", filter)
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
                                    if (g.getString("name").startsWith("[BIZ]")) {
                                        put("type", "BIZ");
                                    }
                                }
                            }
                        )
                    .collect(toList());
        } catch (Exception e) {
            log.info("Cannot get groups by host {}", name);
            return Collections.emptyList();
        }
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
        return zabbixApi;
    }


    private List<Map<String, String>> findDependenciesGivenHostName(String hostName) throws MalformedURLException, RemoteException {
        Stream<Map<String, String>> databasesBelongToSameGroups = databasesBelongToSameGroups(hostName);


        ManagedEntity[] virtualMachines = vmwareInventoryNavigator.searchManagedEntities("VirtualMachine");


        VirtualMachine vm = stream(virtualMachines).map(v -> (VirtualMachine) v)
                .filter(v -> hostName.equals(v.getGuest().getHostName()))
                .findFirst().get();


        Stream<Map<String, String>> datastores = stream(vm.getDatastores()).map(d -> new HashMap<String, String>() {
            {
                put("name", d.getName());
            }
        });

        return Stream.concat(databasesBelongToSameGroups, datastores).collect(Collectors.toSet()).stream().collect(toList());
    }

    private Stream<Map<String, String>> databasesBelongToSameGroups(String hostName) {
        List<Map<String, String>> groups = findGroupsGivenHostName(hostName);


        Stream<Map<String, String>> vmsBelongToSameBizGroups = groups.stream()
                .filter(g -> g.get("name").startsWith("[BIZ]"))
                .map(g -> g.get("id"))
                //.map(id -> new String[]{id})
                .map(filter -> findHostsGivenGroups(filter))
                .flatMap(h -> h.stream());

        Stream<Map<String, String>> allDatabases = findHostsGivenGroups(zabbixProfile.getDbGroupId()).stream();

        List<Map<String, String>> vms = Stream.concat(vmsBelongToSameBizGroups, allDatabases).collect(Collectors.toList());

        return vms.stream().filter(i -> Collections.frequency(vms, i) > 1);
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
