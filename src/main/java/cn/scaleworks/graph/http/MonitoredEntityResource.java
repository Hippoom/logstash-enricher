package cn.scaleworks.graph.http;

import cn.scaleworks.graph.core.MonitoredEntity;
import cn.scaleworks.graph.core.MonitoredEntityRepository;
import cn.scaleworks.graph.core.MonitoredGroupRepository;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(value = "/api/monitored-entities")
public class MonitoredEntityResource {

    @Autowired
    private MonitoredEntityRepository monitoredEntityRepository;

    @Autowired
    private MonitoredGroupRepository monitoredGroupRepository;


    @RequestMapping(value = "/_groups")
    protected List<JSONObject> groups(@RequestParam String id) {
        return monitoredGroupRepository.findGroupsByHostName(id);
    }

    @RequestMapping(value = "/_search")
    protected MonitoredEntity search(@RequestParam(name = "id", required = false) String id,
                                     @RequestParam(name = "vendorSpecificId", required = false) String vendorSpecificId) {
        if (StringUtils.isNotBlank(id)) {
            return monitoredEntityRepository.findById(id);
        } else if (StringUtils.isNotBlank(vendorSpecificId)) {
            return monitoredEntityRepository.findByVendorSpecificId(vendorSpecificId);
        }
        return null;
    }

    @RequestMapping(value = "/_graph")
    protected JSONObject graph() {

        Map<String, MonitoredEntity> entities = monitoredEntityRepository.findAll();

        JSONObject graph = new JSONObject();

        JSONArray nodes = new JSONArray();
        nodes.addAll(entities.entrySet().stream()
                .map(e -> e.getValue())
                .collect(toList()));

        List<JSONObject> links = entities.entrySet().stream()
                .map(e -> e.getValue().getDependsOn().stream()
                        .map(d -> {
                            JSONObject link = new JSONObject();
                            link.put("source", e.getKey());
                            link.put("target", d);
                            return link;
                        }))
                .flatMap(l -> l)
                .collect(toList());


        graph.put("nodes", nodes);
        graph.put("links", links);
        return graph;
    }
}
