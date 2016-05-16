package cn.scaleworks.bff4cmdb.ansible;

import cn.scaleworks.bff4cmdb.graph.MonitoredEntityRepository;
import cn.scaleworks.bff4cmdb.graph.MonitoredGroupRepository;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Configuration
@ConfigurationProperties("ansible")
@Data
@Slf4j
public class AnsibleConfiguration implements ApplicationContextAware {

    private String hostVarsDumpPath;

    @Autowired
    private MonitoredEntityRepository monitoredEntityRepository;

    @Autowired
    private MonitoredGroupRepository monitoredGroupRepository;
    private ApplicationContext applicationContext;

    @PostConstruct
    protected void populateApplicationLevelEntities() throws IOException {
        File directory = applicationContext.getResource(hostVarsDumpPath).getFile();

        String[] dumps = directory.list();

        Stream<Map> applicationLevelEntityStream = Arrays.stream(dumps)
                .map(d -> {
                    try {
                        return applicationContext.getResource(format("%s/%s", hostVarsDumpPath, d)).getURL();
                    } catch (IOException e) {
                        throw new IllegalStateException(format("Cannot read ansible dump [%s] due to %s", d, e.getMessage()), e);
                    }
                })
                .map(d -> {
                    try {
                        return Resources.toString(d, Charsets.UTF_8);
                    } catch (IOException e) {
                        throw new IllegalStateException(format("Cannot read ansible dump [%s] due to %s", d, e.getMessage()), e);
                    }
                })
                .filter(json -> {
                    net.minidev.json.JSONArray exists = JsonPath.read(json, "$.[?(@.filebeat_config)]");
                    return !exists.isEmpty();
                })
                .map(json -> {

                    Filter hasFilebeatConfig = filter(
                            where("filebeat_config").exists(true));

                    String vm = JsonPath.read(json, "$.inventory_hostname");
                    List<String> entities = JsonPath.read(json, "$.filebeat_config.filebeat.prospectors[*].document_type", hasFilebeatConfig);


                    return entities.stream()
                            .map(e -> {
                                Map entity = new HashMap();
                                entity.put("id", e);
                                entity.put("host", vm);
                                entity.put("text", format("%s:%s", e, vm));
                                entity.put("type", "app");
                                entity.put("dependsOn", asList(vm));
                                List<JSONObject> groups = monitoredGroupRepository.findGroupsByHostName(vm);
                                entity.put("groups", groups);
                                return entity;
                            });
                })
                .flatMap(e -> e);


        monitoredEntityRepository.saveOrUpdate(applicationLevelEntityStream.collect(toList()));
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
