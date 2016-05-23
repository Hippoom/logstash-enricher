package cn.scaleworks.graph.file;

import cn.scaleworks.graph.core.MonitoredEntity;
import cn.scaleworks.graph.core.MonitoredEntityLoader;
import cn.scaleworks.graph.core.MonitoredEntityRepository;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty("file.enabled")
@ConfigurationProperties("file")
@Data//otherwise we cannot get properties injected
public class FileMonitoredEntityLoader implements MonitoredEntityLoader {

    private List<MonitoredEntity> entities;

    @Override
    public void populate(MonitoredEntityRepository monitoredEntityRepository) {
        monitoredEntityRepository.savePending(entities);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
