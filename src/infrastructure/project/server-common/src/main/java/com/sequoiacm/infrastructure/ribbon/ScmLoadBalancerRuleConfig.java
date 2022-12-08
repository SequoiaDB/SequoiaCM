package com.sequoiacm.infrastructure.ribbon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

public class ScmLoadBalancerRuleConfig {

    public static final String GROUP_ACCESS_MODE_ACROSS = "across";
    public static final String GROUP_ACCESS_MODE_ALONG = "along";
    public static final String DEFAULT_GROUP_ACCESS_MODE = GROUP_ACCESS_MODE_ACROSS;

    private static final Map<String, String> INNER_GROUP_ACCESS_MODE_MAP = new HashMap<>();
    static {
        INNER_GROUP_ACCESS_MODE_MAP.put("online", GROUP_ACCESS_MODE_ACROSS);
        INNER_GROUP_ACCESS_MODE_MAP.put("batch", GROUP_ACCESS_MODE_ALONG);
    }

    public static final String METADATA_KEY_NODE_GROUP = "nodeGroup";

    @Value("${scm.ribbon.localPreferred:true}")
    private boolean localPreferred = true;

    @Value("${eureka.instance.metadata-map.nodeGroup:#{null}}")
    private String nodeGroup;

    @Value("${eureka.instance.metadata-map.zone:#{null}}")
    private String zone;

    @Value("${eureka.instance.metadata-map.groupAccessMode:#{null}}")
    private String groupAccessMode;

    private String localHostName;

    public ScmLoadBalancerRuleConfig(EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        this.localHostName = eurekaInstanceConfigBean.getHostname();
    }

    @PostConstruct
    public void init() {
        if (nodeGroup != null && groupAccessMode == null) {
            this.groupAccessMode = INNER_GROUP_ACCESS_MODE_MAP.get(nodeGroup);
            if (this.groupAccessMode == null) {
                this.groupAccessMode = DEFAULT_GROUP_ACCESS_MODE;
            }
        }
        if (groupAccessMode != null && !GROUP_ACCESS_MODE_ACROSS.equals(groupAccessMode)
                && !GROUP_ACCESS_MODE_ALONG.equals(groupAccessMode)) {
            throw new IllegalArgumentException("unrecognized groupAccessMode:" + groupAccessMode);
        }
    }

    public boolean isLocalPreferred() {
        return localPreferred;
    }

    public void setLocalPreferred(boolean localPreferred) {
        this.localPreferred = localPreferred;
    }

    public String getNodeGroup() {
        return nodeGroup;
    }

    public void setNodeGroup(String nodeGroup) {
        this.nodeGroup = nodeGroup;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getGroupAccessMode() {
        return groupAccessMode;
    }

    public void setGroupAccessMode(String groupAccessMode) {
        this.groupAccessMode = groupAccessMode;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }

    @Override
    public String toString() {
        return "ScmLoadBalancerRuleConfig{" + "localPreferred=" + localPreferred + ", nodeGroup='"
                + nodeGroup + '\'' + ", zone='" + zone + '\'' + ", groupAccessMode='"
                + groupAccessMode + '\'' + ", localHostName='" + localHostName + '\'' + '}';
    }
}
