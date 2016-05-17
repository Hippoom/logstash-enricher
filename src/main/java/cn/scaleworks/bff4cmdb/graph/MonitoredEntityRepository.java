package cn.scaleworks.bff4cmdb.graph;

import lombok.Setter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Component
public class MonitoredEntityRepository {
    @Setter
    private List<MonitoredEntityLoader> loaders = new ArrayList<>();

    private Map<String, MonitoredEntity> entities = new HashMap();

    private Map<String, MonitoredEntity> toBeProcessing = new HashMap<>();

    public Map<String, MonitoredEntity> findAll() {
        return entities;
    }

    @Scheduled(fixedRate = 60000)
    protected void reload() {
        this.loaders.sort((l1, l2) -> l1.getOrder() - l2.getOrder());
        this.toBeProcessing.clear();

        for (MonitoredEntityLoader loader : loaders) {
            loader.populate(this);
        }

        for (MonitoredEntity pending : toBeProcessing.values()) {

            Stream<String> upstreams = toBeProcessing.values().stream()
                    .filter(e -> {
                        Set<String> dependsOn = e.getDependsOn();
                        return dependsOn.stream()
                                .filter(n -> n.equals(pending.getId())).count() > 0;
                    })
                    .map(u -> u.getId());
            pending.markAsDependencyOf(upstreams.collect(toSet()));
        }
        this.entities = toBeProcessing;

    }

    public MonitoredEntity findById(String id) {
        MonitoredEntity entity = entities.get(id);
        return entity;
    }

    public void savePending(List<MonitoredEntity> pendings) {
        for (MonitoredEntity p : pendings) {
            this.toBeProcessing.put(p.getId(), p);
        }
    }

    public void register(MonitoredEntityLoader loader) {
        this.loaders.add(loader);
    }
}
