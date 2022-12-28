package com.sequoiacm.deploy.module;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;
import org.bson.BSONObject;

import java.util.ArrayList;
import java.util.List;

public class StatusInfo {
    private String hostName;
    private ServiceType serviceType;
    private String backupPath;
    private List<NodeStatus> nodeStatusList;
    private String oldVersion;
    private String newVersion;
    private String currentVersion;

    public static final ConfCoverter<StatusInfo> CONVERTER = new ConfCoverter<StatusInfo>() {
        @Override
        public StatusInfo convert(BSONObject bson) {
            return new StatusInfo(bson);
        }
    };

    public StatusInfo(String hostname, ServiceType serviceType, List<NodeStatus> nodeStatusList,
                      String backupPath, String oldVersion, String newVersion) {
        this.hostName = hostname;
        this.serviceType = serviceType;
        this.backupPath = backupPath;
        this.nodeStatusList = nodeStatusList;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public StatusInfo(BSONObject bson) {
        this.hostName = BsonUtils.getStringChecked(bson, ConfFileDefine.STATUS_HOSTNAME);
        String serviceStr = BsonUtils.getStringChecked(bson, ConfFileDefine.STATUS_SERVICE);
        ServiceType service = ServiceType.getType(serviceStr);
        if (service == null) {
            throw new IllegalArgumentException("upgrade status:unknown service type:" + serviceStr);
        }
        this.serviceType = service;
        List<NodeStatus> nodeStatusList = new ArrayList<>();
        String statusStr = BsonUtils.getStringChecked(bson, ConfFileDefine.STATUS_NODE_STATUS);
        if (!"".equals(statusStr)) {
            for (String nodeStatusStr : statusStr.split(";")) {
                String[] nodeStatusArray = nodeStatusStr.split(":");
                String portStr = nodeStatusArray[0];
                String isStartStr = nodeStatusArray[1];
                NodeStatus nodeStatus = new NodeStatus(portStr);
                if ("start".equals(isStartStr)) {
                    nodeStatus.setStart(true);
                }
                nodeStatusList.add(nodeStatus);
            }
        }
        this.nodeStatusList = nodeStatusList;
        this.backupPath = BsonUtils.getStringChecked(bson, ConfFileDefine.STATUS_BACKUP);
        this.oldVersion = BsonUtils.getStringChecked(bson, ConfFileDefine.STATUS_OLD_VERSION);
        this.newVersion = BsonUtils.getStringChecked(bson, ConfFileDefine.STATUS_NEW_VERSION);
    }

    public String getHostName() {
        return hostName;
    }

    public ServiceType getType() {
        return serviceType;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public List<NodeStatus> getNodeStatus() {
        return nodeStatusList;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }
}
