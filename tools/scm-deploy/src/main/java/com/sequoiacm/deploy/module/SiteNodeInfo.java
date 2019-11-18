package com.sequoiacm.deploy.module;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class SiteNodeInfo extends NodeInfo {

    private String siteName;

    public static final ConfCoverter<NodeInfo> CONVERTER = new ConfCoverter<NodeInfo>() {
        @Override
        public NodeInfo convert(BSONObject bson) {
            return new SiteNodeInfo(bson);
        }
    };

    public SiteNodeInfo(BSONObject bson) {
        setZone(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_ZONE));
        setServiceType(ServiceType.CONTENT_SERVER);
        setHostName(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_HOSTR_NAME));
        setPort(Integer.valueOf(BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_PORT)));
        setCustomNodeConf((BSONObject) JSON
                .parse(BsonUtils.getString(bson, ConfFileDefine.NODE_CUSTOM_CONF)));
        this.siteName = BsonUtils.getStringChecked(bson, ConfFileDefine.NODE_SITE_NAME);
    }

    public String getSiteName() {
        return siteName;
    }

    @Override
    public String toString() {
        return "SiteNodeInfo [siteName=" + siteName + ", getZone()=" + getZone()
                + ", getHostName()=" + getHostName() + ", getPort()=" + getPort()
                + ", getCustomNodeConf()=" + getCustomNodeConf() + ", getServiceType()="
                + getServiceType() + "]";
    }

}
