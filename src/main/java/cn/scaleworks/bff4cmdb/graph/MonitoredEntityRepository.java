package cn.scaleworks.bff4cmdb.graph;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MonitoredEntityRepository {
    @Setter
    private Map<String, JSONObject> entities = new HashMap();

    public Map<String, JSONObject> findAll() {
        return entities;
    }
}
