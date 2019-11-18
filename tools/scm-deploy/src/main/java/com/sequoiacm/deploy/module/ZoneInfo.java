package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

public class ZoneInfo {

    public static final ConfCoverter<ZoneInfo> CONVERTER = new ConfCoverter<ZoneInfo>() {
        @Override
        public ZoneInfo convert(BSONObject bson) {
            return new ZoneInfo(bson);
        }
    };

    private String name;

    public ZoneInfo(BSONObject bson) {
        name = BsonUtils.getStringChecked(bson, ConfFileDefine.ZONE_NAME);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ZoneInfo [name=" + name + "]";
    }

}
