package cn.scaleworks.graph.core;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface MonitoredGroupRepository {
    List<JSONObject> findGroupsByHostName(String hostName);
}
