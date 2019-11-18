package com.sequoiacm.schedule.tools.element;

import java.util.HashMap;
import java.util.Map;

public class ScmNodeStatus {
    private Map<String, ScmNodeProcessInfo> conf2NodeProcessInfo = new HashMap<>();
    private Map<ScmNodeType, Map<String, ScmNodeProcessInfo>> type2NodeProcessInfo = new HashMap<>();

    public void addNode(ScmNodeProcessInfo node) {
        conf2NodeProcessInfo.put(node.getConf(), node);
        Map<String, ScmNodeProcessInfo> nodeInfos = type2NodeProcessInfo.get(node.getType());
        if (nodeInfos == null) {
            nodeInfos = new HashMap<String, ScmNodeProcessInfo>();
            type2NodeProcessInfo.put(node.getType(), nodeInfos);
        }
        nodeInfos.put(node.getConf(), node);
    }

    public Map<String, ScmNodeProcessInfo> getStatusMap() {
        return conf2NodeProcessInfo;
    }

}
