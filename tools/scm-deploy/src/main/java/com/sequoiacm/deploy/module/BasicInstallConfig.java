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
        installPath = spliceScmInstallPath(installPath);
        installUser = BsonUtils.getString(bson, ConfFileDefine.INSTALLCONFIG_USER);
        if (installUser == null || installUser.length() <= 0) {
            installUser = "scmadmin";
        }
    }

    /*
     * splice scm installPath no repeat linux file separator
     * for example:
     * ""                    => /sequoiacm
     * /opt                 => /opt/sequoiacm
     * /opt/                => /opt/sequoiacm
     * /opt////             => /opt/sequoiacm
     * /opt//sequoiacm//    => /opt/sequoiacm
     */
    public String spliceScmInstallPath(String path) {
        path = path.replaceAll("/{2,}", "/");
        if (!path.endsWith("/sequoiacm")) {
            if (path.endsWith("/sequoiacm/")) {
                path = path.substring(0, path.length() - 1);
            }
            else {
                if (path.endsWith("/")) {
                    path += "sequoiacm";
                }
                else {
                    path += "/sequoiacm";
                }
            }
        }
        return path;
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
