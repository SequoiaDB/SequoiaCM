package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class SiteInfo {
    private String name;
    private String datasourceName;
    private boolean isRoot;

    public static final ConfCoverter<SiteInfo> CONVERTER = new ConfCoverter<SiteInfo>() {
        @Override
        public SiteInfo convert(BSONObject bson) {
            return new SiteInfo(bson);
        }
    };

    public SiteInfo(BSONObject bson) {
        name = BsonUtils.getStringChecked(bson, ConfFileDefine.SITE_NAME);
        datasourceName = BsonUtils.getStringChecked(bson, ConfFileDefine.SITE_DATASOURCE_NAME);
        isRoot = Boolean
                .valueOf(BsonUtils.getStringOrElse(bson, ConfFileDefine.SITE_IS_ROOTSITE, "false"));
    }

    public String getName() {
        return name;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public boolean isRoot() {
        return isRoot;
    }

    @Override
    public String toString() {
        return "SiteInfo [name=" + name + ", datasourceName=" + datasourceName + ", isRoot="
                + isRoot + "]";
    }

}
