package cn.scaleworks.bff4cmdb.http;

import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(value = "/api/monitored-entities")
public class MonitoredEntityResource {

    @Autowired
    private MonitoredEntityRepository monitoredEntityRepository;


    @RequestMapping(value = "/_search")
    protected JSONObject search(@RequestParam String name) throws MalformedURLException, RemoteException {
        return monitoredEntityRepository.find(name);
    }

    @RequestMapping(value = "/_graph")
    protected JSONObject graph() {

        Map<String, JSONObject> entities = monitoredEntityRepository.findAll();

        JSONObject graph = new JSONObject();

        JSONArray nodes = new JSONArray();
        nodes.addAll(entities.entrySet().stream()
                .map(e -> e.getValue())
                .collect(toList()));

        List<JSONObject> links = entities.entrySet().stream()
                .filter(e -> e.getValue().get("dependsOn") != null)
                .map(e -> {
                    List<String> dependsOn = (List<String>) e.getValue().get("dependsOn");
                    return dependsOn.stream()
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
}