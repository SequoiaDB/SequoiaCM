package com.sequoiacm.deploy.module;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class NodeInfo {
    private String zone;
    private String hostName;
    private int port;
    private BSONObject customNodeConf;
    private ServiceType serviceType;

    public static final ConfCoverter<NodeInfo> CONVERTER = new ConfCoverter<NodeInfo>() {
        @Override
        public NodeInfo convert(BSONObject bson) {
            return new NodeInfo(bson);
        }
    };

    protected NodeInfo() {
    }

    public NodeInfo(String hostName, ServiceType serviceType){
        this.hostName = hostName;
        this.serviceType = serviceType;
    }

    public NodeInfo(BSONObject bson) {
        zone = BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_ZONE);
        String serviceTypeStr = BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_SERVICE_TYPE);
        serviceType = ServiceType.getType(serviceTypeStr);
        if (serviceType == null) {
            throw new IllegalArgumentException("unknown service type:" + serviceTypeStr);
        }
        hostName = BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_HOSTR_NAME);
        port = Integer.valueOf(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_PORT));
        String customNodeConfStr = BsonUtils.getString(bson, ConfFileDefine.NODE_CUSTOM_CONF);
        customNodeConf = (BSONObject) JSON.parse(customNodeConfStr);
    }

    public String getZone() {
        return zone;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public BSONObject getCustomNodeConf() {
        return customNodeConf;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    protected void setZone(String zone) {
        this.zone = zone;
    }

    protected void setHostName(String hostName) {
        this.hostName = hostName;
    }

    protected void setPort(int port) {
        this.port = port;
    }

    protected void setCustomNodeConf(BSONObject customNodeConf) {
        this.customNodeConf = customNodeConf;
    }

    protected void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String toString() {
        return "NodeInfo [zone=" + zone + ", hostName=" + hostName + ", port=" + port
                + ", customNodeConf=" + customNodeConf + ", serviceType=" + serviceType + "]";
    }

}
