package cn.scaleworks.bff4cmdb.vmware;

import lombok.Data;

@Data
public class VmwareProfile {
    private String dbGroupId;
    private String baseUrl;
    private String password;
    private String username;
}
