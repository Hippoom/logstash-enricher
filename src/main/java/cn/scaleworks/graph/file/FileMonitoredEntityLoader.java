package cn.scaleworks.graph.file;

import cn.scaleworks.graph.core.MonitoredEntity;
import cn.scaleworks.graph.core.MonitoredEntityLoader;
import cn.scaleworks.graph.core.MonitoredEntityRepository;
import cn.scaleworks.graph.core.MonitoredGroupRepository;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Configuration
@ConditionalOnProperty("file.enabled")
@ConfigurationProperties("file")
@Data//otherwise we cannot get properties injected
public class FileMonitoredEntityLoader implements MonitoredEntityLoader, MonitoredGroupRepository {

    private List<MonitoredEntity> entities = new ArrayList<>();

    @Override
    public void populate(MonitoredEntityRepository monitoredEntityRepository) {
        monitoredEntityRepository.savePending(entities);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public List<JSONObject> findGroupsByHostName(String hostName) {
        return entities.stream()
                .map(e -> e.getGroups())
                .flatMap(g -> g.stream())
                .map(g -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", g);
                    return jsonObject;
                }).collect(toList());
    }
}
