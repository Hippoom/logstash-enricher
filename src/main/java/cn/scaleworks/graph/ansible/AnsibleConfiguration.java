package cn.scaleworks.graph.ansible;

import cn.scaleworks.graph.core.MonitoredEntity;
import cn.scaleworks.graph.core.MonitoredEntityLoader;
import cn.scaleworks.graph.core.MonitoredEntityRepository;
import cn.scaleworks.graph.core.MonitoredGroupRepository;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Configuration
@ConditionalOnProperty("ansible.enabled")
@ConfigurationProperties("ansible")
@Data
@Slf4j
public class AnsibleConfiguration implements ApplicationContextAware, MonitoredEntityLoader {

    private String hostVarsDumpPath;


    @Autowired
    private MonitoredGroupRepository monitoredGroupRepository;

    @Autowired
    private MonitoredEntityRepository monitoredEntityRepository;

    private ApplicationContext applicationContext;

    private void populateApplicationLevelEntities(MonitoredEntityRepository monitoredEntityRepository) {
        File directory = null;
        try {
            directory = applicationContext.getResource(hostVarsDumpPath).getFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(format("Cannot load ansible dump from %s", hostVarsDumpPath));
        }

        String[] dumps = directory.list();

        Stream<MonitoredEntity> applicationLevelEntityStream = Arrays.stream(dumps)
                .filter(d -> !d.startsWith("."))
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
                    List<String> entities = JsonPath.read(json, "$.filebeat_config.filebeat.prospectors[*].fields.object_id", hasFilebeatConfig);


                    return entities.stream()
                            .map(e -> {
                                String id = e;
                                String host = vm;
                                String type = e.split("_")[0];
                                String text = format("%s:%s", e, vm);
                                MonitoredEntity entity = new MonitoredEntity(id, host, type, text);
                                entity.assignDependency(vm);
                                List<JSONObject> groups = monitoredGroupRepository.findGroupsByHostName(vm);
                                entity.assignGroups(groups.stream().map(g -> (String) g.get("name")).collect(toSet()));
                                return entity;
                            });
                })
                .flatMap(e -> e);


        List<MonitoredEntity> entities = applicationLevelEntityStream.collect(toList());

        List<MonitoredEntity> databases = entities.stream()
                .filter(app -> app.getType().equals("db"))
                .collect(toList());


        Stream<MonitoredEntity> appStream = entities.stream()
                //.filter(app -> app.get("type").equals("app"))
                .map(e -> {
                    List<String> groups = e.getGroups();
                    List<String> dependsOn = e.getDependsOn();

                    List<String> dbBelongToSameBizGroup = groups.stream()
                            .filter(g -> g.startsWith("[BIZ]"))
                            .map(g -> {
                                return databases.stream()
                                        .filter(db -> {
                                            return db.belongToGroup(g);
                                        })
                                        .map(db -> db.getId()).collect(toList());
                            })
                            .flatMap(db -> db.stream())
                            .collect(toList());

                    dependsOn.addAll(dbBelongToSameBizGroup);
                    return e;
                });


        monitoredEntityRepository.savePending(appStream.collect(toList()));
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void populate(MonitoredEntityRepository monitoredEntityRepository) {
        populateApplicationLevelEntities(monitoredEntityRepository);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
