package com.sequoiacm.deploy.module;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;
import org.bson.BSONObject;

public class DaemonInfo {
    private boolean enableDaemon;

    public static final ConfCoverter<DaemonInfo> CONVERTER = new ConfCoverter<DaemonInfo>() {
        @Override
        public DaemonInfo convert(BSONObject bson) {
            return new DaemonInfo(bson);
        }
    };

    public DaemonInfo(BSONObject bson) {
        enableDaemon = Boolean
                .valueOf(BsonUtils.getStringOrElse(bson, ConfFileDefine.DAEMON_ENABLE, "true"));
    }

    public boolean isEnableDaemon() {
        return enableDaemon;
    }

    @Override
    public String toString() {
        return "Daemon [enableDaemon=" + enableDaemon + "]";
    }
}
