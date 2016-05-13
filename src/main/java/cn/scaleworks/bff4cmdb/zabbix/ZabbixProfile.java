package cn.scaleworks.bff4cmdb.zabbix;

import lombok.Data;

@Data
public class ZabbixProfile {
    private String dbGroupId;
    private String baseUrl;
    private String password;
    private String username;
}
