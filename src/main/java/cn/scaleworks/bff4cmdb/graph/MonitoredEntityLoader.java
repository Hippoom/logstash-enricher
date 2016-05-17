package cn.scaleworks.bff4cmdb.graph;

public interface MonitoredEntityLoader {

    void populate(MonitoredEntityRepository monitoredEntityRepository);

    int getOrder();
}
