package cn.scaleworks.bff4cmdb.zabbix;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZabbixConfiguration {

    @ConfigurationProperties("zabbix")
    @Bean
    protected ZabbixProfile zabbixProfile() {
        return new ZabbixProfile();
    }
}
