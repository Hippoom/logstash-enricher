package cn.scaleworks.bff4cmdb.graph;

import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class MonitoredEntityRepository {
    @Setter
    private Map<String, MonitoredEntity> entities = new HashMap();

    public Map<String, MonitoredEntity> findAll() {
        return entities;
    }

    public MonitoredEntity findById(String id) {
        MonitoredEntity entity = entities.get(id);
        Stream<String> upstreams = entities.values().stream()
                .filter(e -> {
                    Set<String> dependsOn = e.getDependsOn();
                    return dependsOn.stream()
                            .filter(n -> n.equals(id)).count() > 0;
                })
                .map(u -> u.getId());
        entity.markAsDependencyOf(upstreams.collect(toSet()));
        return entity;
    }

    public void saveOrUpdate(List<MonitoredEntity> entities) {
        entities.stream()
                .forEach(e -> {
                    this.entities.put(e.getId(), e);
                });
    }
}
