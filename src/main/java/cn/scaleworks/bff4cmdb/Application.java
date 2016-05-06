package cn.scaleworks.bff4cmdb;

import com.alibaba.fastjson.JSONObject;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@SpringBootApplication
public class Application {

    @RequestMapping(value = "/host/{name}/groups")
    protected List<String> findGroupsGivenHostName(@PathVariable String name) {
        String url = "http://10.202.128.121/zabbix/api_jsonrpc.php";
        ZabbixApi zabbixApi = new DefaultZabbixApi(url);
        zabbixApi.init();

        boolean login = zabbixApi.login("ygzhou", "1qaz@WSX");


        JSONObject filter = new JSONObject();

        filter.put("host", new String[]{name});


        Request getRequest = RequestBuilder.newBuilder()
                .method("host.get").paramEntry("selectGroups", "extend").paramEntry("filter", filter)
                .build();
        JSONObject getResponse = zabbixApi.call(getRequest);

        return getResponse.getJSONArray("result")
                .getJSONObject(0).getJSONArray("groups")
                .stream()
                .map(g -> (JSONObject) g)
                .map(g -> g.getString("name"))
                .collect(Collectors.toList());
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
