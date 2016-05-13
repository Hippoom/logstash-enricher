package cn.scaleworks.bff4cmdb.zabbix;

import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static java.lang.String.format;

@Configuration
public class ZabbixConfiguration {

    @ConfigurationProperties("zabbix")
    @Bean
    protected ZabbixProfile zabbixProfile() {
        return new ZabbixProfile();
    }

    @Lazy//so that when the app launches, zabbix does not have to be alive
    @Bean
    protected ZabbixApi zabbixApi(ZabbixProfile profile) {
        String url = format("%s/zabbix/api_jsonrpc.php", profile.getBaseUrl());
        ZabbixApi zabbixApi = new DefaultZabbixApi(url);
        zabbixApi.init();

        boolean login = zabbixApi.login(profile.getUsername(), profile.getPassword());

        if (!login) {
            throw new IllegalStateException(format("Cannot login zabbix [%s] with [%s]", url, profile.getUsername()));
        }
        return zabbixApi;
    }
}
