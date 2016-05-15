package cn.scaleworks.bff4cmdb.sample;

import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

@Configuration
//@ConditionalOnProperty("test")
@ConfigurationProperties("test")
@Data//otherwise we cannot get properties injected
public class SampleConfiguration {

    private Map<String, JSONObject> entities;

    @Autowired
    private MonitoredEntityRepository monitoredEntityRepository;

    @PostConstruct
    protected void populate() {
        monitoredEntityRepository.setEntities(entities);
    }
}
