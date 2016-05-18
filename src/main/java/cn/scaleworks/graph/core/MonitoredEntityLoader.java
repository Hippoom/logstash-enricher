package cn.scaleworks.graph.core;

public interface MonitoredEntityLoader {

    void populate(MonitoredEntityRepository monitoredEntityRepository);

    int getOrder();
}
