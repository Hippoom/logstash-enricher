package cn.scaleworks.graph.ansible;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class AnsibleVarsLearningTest {

    @Test
    public void shouldExtractMonitoredEntities() throws IOException {
        //"hostvars['vm2']": {

        URL file = Resources.getResource("ansible_host_vars.json");
        String text = Resources.toString(file, Charsets.UTF_8);

//        Filter hasFilebeat = filter(
//                where("filebeat_config").exists(true));

        String vm = JsonPath.read(text, "$.inventory_hostname");
        List<String> entities = JsonPath.read(text, "$.filebeat_config.filebeat.prospectors[*].fields.object_id");

        System.out.println(vm);

        //JSONArray monitored_entities = JsonPath.read(text, "$.vm2.filebeat_config.filebeat.prospectors[*].document_type");

        System.out.println(entities.stream()
                .map(e -> {
                    Map entity = new HashMap();
                    entity.put("id", e);
                    entity.put("dependsOn", asList(vm));
                    return entity;
                })
                .collect(toList()));
    }

    @Test
    public void shouldSkip_givenNonMonitoredEntities() throws IOException {
        URL file = Resources.getResource("ansible_host_vars_without_filebeat.json");
        String text = Resources.toString(file, Charsets.UTF_8);

//        Filter hasFilebeatConfig = filter(
//                where("filebeat_config")..exists(false));

        String vm = JsonPath.read(text, "$.inventory_hostname");
        Object entities = JsonPath.read(text, "$.[?(@.filebeat_config)]");

        System.out.println(entities);
    }
}