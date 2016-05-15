package cn.scaleworks.bff4cmdb.graph;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface MonitoredGroupRepository {
    List<JSONObject> findGroupsByHostName(String hostName);
}
