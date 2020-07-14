package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

/**
 * @author huangqiaohui
 *
 */
public class InstallConfig {
    private String installPath;
    private String installUser;
    private String installUserPassword;
    private String installUserGroup;

    public static final ConfCoverter<InstallConfig> CONVERTER = new ConfCoverter<InstallConfig>() {
        @Override
        public InstallConfig convert(BSONObject bson) {
            return new InstallConfig(bson);
        }
    };

    public InstallConfig(BSONObject bson) {
        installPath = BsonUtils.getStringChecked(bson, ConfFileDefine.INSTALLCONFIG_PATH)
                + "/sequoiacm";
        installUser = BsonUtils.getString(bson, ConfFileDefine.INSTALLCONFIG_USER);
        if (installUser == null || installUser.length() <= 0) {
            installUser = "scmadmin";
        }
        installUserPassword = BsonUtils.getStringChecked(bson,
                ConfFileDefine.INSTALLCONFIG_PASSWORD);
        installUserGroup = BsonUtils.getString(bson, ConfFileDefine.INSTALLCONFIG_USER_GROUP);
        if (installUserGroup == null || installUserGroup.length() <= 0) {
            installUserGroup = "scmadmin_group";
        }
    }

    public String getInstallUserGroup() {
        return installUserGroup;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getInstallUser() {
        return installUser;
    }

    public String getInstallUserPassword() {
        return installUserPassword;
    }

    @Override
    public String toString() {
        return "InstallConfig [installPath=" + installPath + ", installUser=" + installUser + "]";
    }

    public void resetInstallUserGroup(String installUserGroup) {
        this.installUserGroup = installUserGroup;
    }
}
