package cn.scaleworks.graph.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
public class MonitoredEntityRepository {
    @Autowired
    private List<MonitoredEntityLoader> loaders = new ArrayList<>();

    private Map<String, MonitoredEntity> entities = new HashMap();

    private Map<String, MonitoredEntity> toBeProcessing = new HashMap<>();

    public Map<String, MonitoredEntity> findAll() {
        return entities;
    }

    @Scheduled(initialDelayString = "${scheduler.monitored.entity.refresh.initial.delay.seconds:15}000",
               fixedDelayString = "${scheduler.monitored.entity.refresh.fixed.delay.seconds:900}000")
    protected void reload() {
        log.debug("Begin to reload monitored entities with {}", loaders);
        this.loaders.sort((l1, l2) -> l1.getOrder() - l2.getOrder());
        this.toBeProcessing.clear();

        for (MonitoredEntityLoader loader : loaders) {
            loader.populate(this);
        }

        for (MonitoredEntity pending : toBeProcessing.values()) {

            Stream<String> upstreams = toBeProcessing.values().stream()
                    .filter(e -> {
                        List<String> dependsOn = e.getDependsOn();
                        return dependsOn.stream()
                                .filter(n -> n.equals(pending.getId())).count() > 0;
                    })
                    .map(u -> u.getId());
            pending.markAsDependencyOf(upstreams.collect(toSet()));
        }
        this.entities = toBeProcessing;
        log.debug("Finish to reload monitored entities");
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
