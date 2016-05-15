package cn.scaleworks.bff4cmdb.graph;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MonitoredEntityRepository {
    @Setter
    private Map<String, JSONObject> entities = new HashMap();

    public Map<String, JSONObject> findAll() {
        return entities;
    }

    public JSONObject find(String name) {
        JSONObject entity = entities.get(name);
        Stream<String> upstreams = entities.values().stream()
                .filter(e -> e.get("dependsOn") != null)
                .filter(e -> {
                    Map<String, String> dependsOn = (Map<String, String>) e.get("dependsOn");
                    return dependsOn.values().stream()
                            .filter(n -> n.equals(name)).count() > 0;
                })
                .map(u -> (String) u.get("id"));
        entity.put("upstreams", upstreams.collect(toList()));
        return entity;
    }
}
