package cn.scaleworks.bff4cmdb;

import cn.scaleworks.bff4cmdb.zabbix.ZabbixProfile;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.*;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

@Slf4j
@RestController
@SpringBootApplication
public class Application {

    @Autowired
    private ZabbixProfile zabbixProfile;

    @Lazy
    @Autowired
    private ZabbixApi zabbixApi;

    @Lazy
    @Autowired
    private InventoryNavigator vmwareInventoryNavigator;

    private Map<String, List<String>> hostSystemUpstreams;
    private Map<String, List<String>> virtualMachineUpstreams;
    private Map<String, List<String>> datastoreUpstreams;
    private List<HostSystem> hostSystems;
    private List<VirtualMachine> virtualMachines;
    private List<Datastore> datastores;

    @Setter
    private Map<String, JSONObject> entities = new HashMap();


    //@Lazy
    //@PostConstruct
    protected void populateDatastores() throws RemoteException {
        hostSystems = Arrays.stream(vmwareInventoryNavigator.searchManagedEntities("HostSystem"))
                .map(h -> (HostSystem) h).collect(toList());


        virtualMachines = hostSystems.stream()
                .map(h -> getVmStream(h))
                .collect(() -> new ArrayList<>(),
                        (list, item) -> list.addAll(item.collect(toList())),
                        (list1, list2) -> list1.addAll(list2));

        datastores = hostSystems.stream()
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

        this.hostSystemUpstreams = hostSystems.stream()
                .map(h -> new HashMap<String, List<String>>() {
                    {
                        put(h.getName(), getVmStream(h)
                                .map(v -> v.getGuest().getHostName()).collect(toList()));
                    }
                })
                .collect(() -> new HashMap<>(),
                        (map, item) -> map.putAll(item),
                        (map1, map2) -> map1.putAll(map2));

        this.virtualMachineUpstreams = virtualMachines.stream()
                .map(h -> new HashMap<String, List<String>>() {
                    {
                        put(h.getName(), upstreamsOf(h));
                    }
                })
                .collect(() -> new HashMap<>(),
                        (map, item) -> map.putAll(item),
                        (map1, map2) -> map1.putAll(map2));
        this.datastoreUpstreams = datastores.stream()
                .map(d -> new HashMap() {
                    {
                        put(d.getName(), upstreamsOf(d));
                    }
                })
                .collect(() -> new HashMap<>(),
                        (map, item) -> map.putAll(item),
                        (map1, map2) -> map1.putAll(map2));
    }

    private List<String> upstreamsOf(Datastore d) {
        Stream<String> hostStream = Arrays.stream(d.getHost())
                .map(h -> getHostSystemBy(h.getKey()))
                .filter(h -> h.isPresent())
                .map(h -> h.get().getName());
        Stream<String> vmStream = getVmStream(d)
                .map(v -> v.getGuest().getHostName());
        return concat(hostStream, vmStream).collect(toList());
    }

    private List<String> upstreamsOf(VirtualMachine h) {
        try {
            List<String> upstreams = Arrays.stream(h.getDatastores())
                    .map(d -> d.getName()).collect(toList());
            getHostSystemBy(h.getRuntime().getHost()).ifPresent(h1 -> upstreams.add(h1.getName()));
            return upstreams;
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Optional<HostSystem> getHostSystemBy(ManagedObjectReference hostRef) {
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

    private Stream<VirtualMachine> getVmStream(Datastore h) {
        return Arrays.stream(h.getVms());
    }

    private List<String> getVmHostNames(HostSystem h) {
        try {
            return Arrays.stream(h.getVms()).map(v -> v.getGuest().getHostName()).collect(toList());
        } catch (RemoteException e) {
            log.warn("Cannot connect to vmware due to {}", e.getMessage(), e);
            return null; //should I, will NPE be thrown?
        }
    }

    @RequestMapping(value = "/host")
    private Map<String, Object> findHostGiven(@RequestParam String name,
                                              @RequestParam(required = false) String type) throws MalformedURLException, RemoteException {

        List<Map<String, String>> groups = findGroupsGivenHostName(name);
        //List<Map<String, String>> dependencies = findDependenciesGivenHostName(name);
        List<Map<String, String>> upstreams = findUpstreamsGivenHostName(name, type);


        return new HashMap() {
            {
                put("groups", groups);
                put("upstreams", upstreams);
//                put("dependencies", dependencies);
            }
        };
    }

    @RequestMapping(value = "/api/graph")
    protected JSONObject graph() {
        JSONObject graph = new JSONObject();

        JSONArray nodes = new JSONArray();
        nodes.addAll(entities.entrySet().stream()
                .map(e -> e.getValue())
                .collect(toList()));

        List<JSONObject> links = entities.entrySet().stream()
                .filter(e -> e.getValue().get("dependsOn") != null)
                .map(e -> {
                    Map<String, String> dependsOn = (Map<String, String>) e.getValue().get("dependsOn");
                    return dependsOn.values().stream()
                            .map(d -> {
                                JSONObject link = new JSONObject();
                                link.put("source", e.getKey());
                                link.put("target", d);
                                return link;
                            });
                })
                .flatMap(l -> l)
                .collect(toList());


        graph.put("nodes", nodes);
        graph.put("links", links);
        return graph;
    }

    private List<Map<String, String>> findUpstreamsGivenHostName(String hostName, String type) throws RemoteException {
        if (asList("mw", "db").contains(type)) {
            return databasesBelongToSameGroups(hostName).collect(toList());
        } else if ("vm".equals(type)) {
            return virtualMachineUpstreams.get(hostName).stream()
                    .map(d -> new HashMap<String, String>() {
                        {
                            put("name", d);
                        }
                    }).collect(toList());
        } else if ("vh".equals(type)) {
            return hostSystemUpstreams.get(hostName).stream()
                    .map(d -> new HashMap<String, String>() {
                        {
                            put("name", d);
                        }
                    }).collect(toList());
        } else if ("ds".equals(type)) {
            return datastoreUpstreams.get(hostName).stream()
                    .map(d -> new HashMap<String, String>() {
                        {
                            put("name", d);
                        }
                    }).collect(toList());
        } else {
            return emptyList();
        }
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
            return emptyList();
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


        ManagedEntity[] virtualMachines = vmwareInventoryNavigator.searchManagedEntities("ManagedEntity");


        VirtualMachine vm = stream(virtualMachines).map(v -> (VirtualMachine) v)
                .filter(v -> hostName.equals(v.getGuest().getHostName()))
                .findFirst().get();


        Stream<Map<String, String>> datastores = stream(vm.getDatastores()).map(d -> new HashMap<String, String>() {
            {
                put("name", d.getName());
            }
        });

        return concat(databasesBelongToSameGroups, datastores).collect(Collectors.toSet()).stream().collect(toList());
    }

    private Stream<Map<String, String>> databasesBelongToSameGroups(String hostName) {
        List<Map<String, String>> groups = findGroupsGivenHostName(hostName);


        Stream<Map<String, String>> vmsBelongToSameBizGroups = groups.stream()
                .filter(g -> g.get("name").startsWith("[BIZ]"))
                .map(g -> g.get("id"))
                .map(filter -> findHostsGivenGroups(filter))
                .flatMap(h -> h.stream());

        Stream<Map<String, String>> allDatabases = findHostsGivenGroups(zabbixProfile.getDbGroupId()).stream();

        List<Map<String, String>> vms = concat(vmsBelongToSameBizGroups, allDatabases).collect(toList());

        return vms.stream().filter(i -> Collections.frequency(vms, i) > 1);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
