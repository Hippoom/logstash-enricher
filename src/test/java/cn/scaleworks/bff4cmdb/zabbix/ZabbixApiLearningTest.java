package cn.scaleworks.bff4cmdb.zabbix;

import com.alibaba.fastjson.JSONObject;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import org.junit.Before;
import org.junit.Test;

public class ZabbixApiLearningTest {

    private ZabbixApi zabbixApi;
    private String url = "http://10.202.128.121/zabbix/api_jsonrpc.php";

    @Before
    public void login() {
        zabbixApi = new DefaultZabbixApi(url);
        zabbixApi.init();

        boolean login = zabbixApi.login("ygzhou", "1qaz@WSX");
    }

    @Test
    public void shouldReturnGroups_givenHost() {

        String host = "Zabbix server";

        JSONObject filter = new JSONObject();

        filter.put("host", new String[]{host});


        Request getRequest = RequestBuilder.newBuilder()
                .method("host.get").paramEntry("selectGroups", "extend").paramEntry("filter", filter)
                .build();
        JSONObject getResponse = zabbixApi.call(getRequest);

        getResponse.getJSONArray("result")
                .getJSONObject(0).getJSONArray("groups")
                .stream()
                .map(g -> (JSONObject) g)
                .forEach(g -> System.err.println(g.getString("groupid") + "@" + g.getString("name")));

    }

    @Test
    public void shouldReturnHosts_givenGroups() {

        String host = "Zabbix server";

        JSONObject filter = new JSONObject();

        filter.put("groupids", new String[]{"2", "4"});


        Request getRequest = RequestBuilder.newBuilder()
                .method("host.get").paramEntry("filter", filter)
                .build();
        JSONObject getResponse = zabbixApi.call(getRequest);

        System.err.println(getResponse.getJSONArray("result")
                .getJSONObject(0).getString("name"));

    }
}
