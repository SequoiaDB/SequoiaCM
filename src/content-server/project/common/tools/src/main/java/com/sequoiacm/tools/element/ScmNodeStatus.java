package com.sequoiacm.tools.element;

import java.util.HashMap;
import java.util.Map;

public class ScmNodeStatus {
    private Map<String, Integer> conf2PidMap = new HashMap<>();
    public void addNode(String conf, int pid) {
        conf2PidMap.put(conf, pid);
    }

    public Map<String, Integer> getStatusMap() {
        return conf2PidMap;
    }

}
