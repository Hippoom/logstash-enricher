package cn.scaleworks.bff4cmdb.file;

import cn.scaleworks.bff4cmdb.graph.MonitoredEntity;
import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnProperty("file.enabled")
@ConfigurationProperties("file")
@Data//otherwise we cannot get properties injected
public class PopulateMonitoredEntityRepositoryFromFile {

    private Map<String, MonitoredEntity> entities;

    @Bean
    protected MonitoredEntityRepository monitoredEntityRepository() {
        MonitoredEntityRepository monitoredEntityRepository = new MonitoredEntityRepository();
        monitoredEntityRepository.setEntities(entities);
        return monitoredEntityRepository;
    }
}
