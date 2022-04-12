package com.sequoiacm.deploy.module;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class S3NodeInfo extends NodeInfo {
    private String bindingSite;
    private String serviceName;

    public static final ConfCoverter<NodeInfo> CONVERTER = new ConfCoverter<NodeInfo>() {
        @Override
        public NodeInfo convert(BSONObject bson) {
            return new S3NodeInfo(bson);
        }
    };

    public String getBindingSite() {
        return bindingSite;
    }

    public String getServiceName() {
        return serviceName;
    }

    public S3NodeInfo(BSONObject bson) {
        setZone(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_ZONE));
        setServiceType(ServiceType.S3_SERVER);
        setHostName(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_HOSTR_NAME));
        setPort(Integer.parseInt(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_PORT)));
        setCustomNodeConf((BSONObject) JSON
                .parse(BsonUtils.getString(bson, ConfFileDefine.NODE_CUSTOM_CONF)));
        this.bindingSite = BsonUtils.getStringChecked(bson, ConfFileDefine.S3_NODE_BINDING_SITE);
        this.serviceName = BsonUtils.getStringOrElse(bson, ConfFileDefine.S3_NODE_SERVICE_NAME,
                bindingSite + "-s3");
        String managementPortStr = BsonUtils.getString(bson, ConfFileDefine.NODE_MANAGEMENT_PORT);
        if (managementPortStr != null && !managementPortStr.isEmpty()) {
            setManagementPort(Integer.parseInt(managementPortStr));
        }
    }

    @Override
    public String toString() {
        return "S3NodeInfo{" + "bindingSite='" + bindingSite + '\'' + ", serviceName='"
                + serviceName + '\'' + '}';
    }
}
