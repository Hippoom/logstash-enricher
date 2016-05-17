package cn.scaleworks.bff4cmdb.http;

import cn.scaleworks.bff4cmdb.graph.MonitoredEntity;
import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(value = "/api/monitored-entities")
public class MonitoredEntityResource {

    @Autowired
    private MonitoredEntityRepository monitoredEntityRepository;


    @RequestMapping(value = "/_search")
    protected MonitoredEntity search(@RequestParam String id) {
        return monitoredEntityRepository.findById(id);
    }

    @RequestMapping(value = "/_graph")
    protected JSONObject graph() {

        Map<String, MonitoredEntity> entities = monitoredEntityRepository.findAll();

        JSONObject graph = new JSONObject();

        JSONArray nodes = new JSONArray();
        nodes.addAll(entities.entrySet().stream()
                .map(e -> e.getValue())
                .collect(toList()));

        List<JSONObject> links = entities.entrySet().stream()
                .map(e -> ((Set<String>) e.getValue().getDependsOn()).stream()
                        .map(d -> {
                            JSONObject link = new JSONObject();
                            link.put("source", e.getKey());
                            link.put("target", d);
                            return link;
                        }))
                .flatMap(l -> l)
                .collect(toList());


        graph.put("nodes", nodes);
        graph.put("links", links);
        return graph;
    }
}
