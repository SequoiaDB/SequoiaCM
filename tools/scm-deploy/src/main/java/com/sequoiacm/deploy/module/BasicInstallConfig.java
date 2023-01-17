package com.sequoiacm.deploy.module;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;
import org.bson.BSONObject;

public class BasicInstallConfig {

    private String installPath;
    private String installUser;

    public static final ConfCoverter<BasicInstallConfig> CONVERTER = new ConfCoverter<BasicInstallConfig>() {
        @Override
        public BasicInstallConfig convert(BSONObject bson) {
            return new BasicInstallConfig(bson);
        }
    };

    public BasicInstallConfig(BSONObject bson) {
        installPath = BsonUtils.getStringChecked(bson, ConfFileDefine.INSTALLCONFIG_PATH);
        if (!installPath.endsWith("/sequoiacm")) {
            installPath += "/sequoiacm";
        }
        installUser = BsonUtils.getString(bson, ConfFileDefine.INSTALLCONFIG_USER);
        if (installUser == null || installUser.length() <= 0) {
            installUser = "scmadmin";
        }
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getInstallUser() {
        return installUser;
    }

    @Override
    public String toString() {
        return "BasicInstallConfig{" + "installPath='" + installPath + '\'' + ", installUser='"
                + installUser + '\'' + '}';
    }
}
