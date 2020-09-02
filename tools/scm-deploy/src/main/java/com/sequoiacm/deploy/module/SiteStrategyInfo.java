package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class SiteStrategyInfo {
    private SiteStrategyType type;

    public static final ConfCoverter<SiteStrategyInfo> CONVERTER = new ConfCoverter<SiteStrategyInfo>() {
        @Override
        public SiteStrategyInfo convert(BSONObject bson) {
            return new SiteStrategyInfo(bson);
        }
    };

    public SiteStrategyInfo(BSONObject bson) {
        String typeStr = BsonUtils.getStringChecked(bson, ConfFileDefine.SITE_STRATEGY);
        type = SiteStrategyType.getEnumByString(typeStr);
    }

    public SiteStrategyType getType() {
        return type;
    }
}
