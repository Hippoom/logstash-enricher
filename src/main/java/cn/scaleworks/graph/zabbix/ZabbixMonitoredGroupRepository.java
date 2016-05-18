package cn.scaleworks.graph.zabbix;

import cn.scaleworks.graph.core.MonitoredGroupRepository;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Configuration
@ConditionalOnProperty("zabbix.enabled")
@ConfigurationProperties("zabbix")
@Data
@Slf4j
public class ZabbixMonitoredGroupRepository implements MonitoredGroupRepository {
    private String dbGroupId;
    private String baseUrl;
    private String password;
    private String username;

    @Lazy
    @Autowired
    private ZabbixApi zabbixApi;

    @Lazy//so that when the app launches, zabbix does not have to be alive
    @Bean
    protected ZabbixApi zabbixApi() {
        String url = format("%s/zabbix/api_jsonrpc.php", baseUrl);
        ZabbixApi zabbixApi = new DefaultZabbixApi(url);
        zabbixApi.init();

        boolean login = zabbixApi.login(username, password);

        if (!login) {
            throw new IllegalStateException(format("Cannot login zabbix [%s] with [%s]", url, username));
        }
        return zabbixApi;
    }

    @Setter
    private Map<String, JSONObject> entities = new HashMap();

    @Override
    public List<JSONObject> findGroupsByHostName(String hostName) {
        JSONObject filter = new JSONObject();
        filter.put("host", new String[]{hostName});

        Request getRequest = RequestBuilder.newBuilder()
                .method("host.get")
                .paramEntry("selectGroups", "extend")//so that we get groups
                .paramEntry("filter", filter)
                .build();
        JSONObject getResponse = zabbixApi.call(getRequest);

        JSONArray hostsMaybe = getResponse.getJSONArray("result");
        return hostsMaybe.isEmpty() ? Collections.emptyList() :
                hostsMaybe
                        .getJSONObject(0).getJSONArray("groups")
                        .stream()
                        .map(g -> (JSONObject) g)
                        .map(g -> {
                                    JSONObject group = new JSONObject();
                                    group.put("name", g.getString("name"));
                                    if (g.getString("name").startsWith("[BIZ]")) {
                                        group.put("type", "BIZ");

                                    }
                                    return group;
                                }
                            )
                        .collect(toList());

    }
}
