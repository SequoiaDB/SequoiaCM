package com.sequoiacm.diagnose.common;

import java.util.Map;

public class ScmNodeInfo implements Comparable<ScmNodeInfo> {
    private String serviceName;
    private String ip_addr;
    private Integer port;

    private String hostName;
    private String region;
    private String zone;
    private Integer pid = -1;
    private String nodeGroup;
    private boolean isManualStopped;
    private boolean isContentServer;
    private boolean isS3Server;

    public ScmNodeInfo(Map obj) {
        Object temp = null;
        temp = obj.get("service_name");
        if (null != temp) {
            this.serviceName = temp.toString();
        }
        temp = obj.get("ip_addr");
        if (null != temp) {
            this.ip_addr = temp.toString();
        }

        temp = obj.get("host_name");
        if (null != temp) {
            this.hostName = temp.toString();
        }

        temp = obj.get("port");
        if (null != temp) {
            this.port = Integer.parseInt(temp.toString());
        }

        temp = obj.get("region");
        if (null != temp) {
            this.region = temp.toString();
        }

        temp = obj.get("zone");
        if (null != temp) {
            this.zone = temp.toString();
        }

        temp = obj.get("is_manual_stopped");
        if (null != temp) {
            this.isManualStopped = Boolean.parseBoolean(temp.toString());
        }
        Map metadata = (Map) obj.get("metadata");
        if (metadata != null) {
            Object nodeGroup = metadata.get("nodeGroup");
            if (nodeGroup != null) {
                this.nodeGroup = nodeGroup.toString();
            }
            this.isS3Server = Boolean.parseBoolean((String) metadata.get("isS3Server"));
            this.isContentServer = Boolean.parseBoolean((String) metadata.get("isContentServer"));
        }
    }

    public boolean isContentServer() {
        return isContentServer;
    }

    public boolean isS3Server() {
        return isS3Server;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getIp_addr() {
        return ip_addr;
    }

    public Integer getPort() {
        return port;
    }

    public String getHostName() {
        return hostName;
    }

    public String getRegion() {
        return region;
    }

    public String getZone() {
        return zone;
    }

    public Integer getPid() {
        return pid;
    }

    public String getNodeGroup() {
        return nodeGroup;
    }

    public boolean isManualStopped() {
        return isManualStopped;
    }

    @Override
    public int compareTo(ScmNodeInfo o) {
        if (this.hostName.equals(o.getHostName())) {
            if (o.getServiceName().equals(this.serviceName)) {
                return this.port - o.getPort();
            }
            return this.serviceName.compareTo(o.getServiceName());
        }
        return this.hostName.compareTo(o.getHostName());
    }
}
