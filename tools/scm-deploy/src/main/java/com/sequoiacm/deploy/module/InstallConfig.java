package com.sequoiacm.deploy.module;

import org.bson.BSONObject;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.ConfFileDefine;
import com.sequoiacm.deploy.parser.ConfCoverter;

/**
 * @author huangqiaohui
 *
 */
public class InstallConfig extends BasicInstallConfig {
    private String installUserPassword;
    private String installUserGroup;

    private boolean hasReset;

    public static final ConfCoverter<InstallConfig> CONVERTER = new ConfCoverter<InstallConfig>() {
        @Override
        public InstallConfig convert(BSONObject bson) {
            return new InstallConfig(bson);
        }
    };

    public InstallConfig(BSONObject bson) {
        super(bson);
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

    public String getInstallUserPassword() {
        return installUserPassword;
    }

    @Override
    public String toString() {
        return "InstallConfig [installPath=" + getInstallPath() + ", installUser="
                + getInstallUser() + ", installUserGroup=" + this.installUserGroup + "]";
    }

    public void resetInstallUserGroup(String installUserGroup) throws Exception {
        if (!hasReset) {
            this.installUserGroup = installUserGroup;
            hasReset = true;
            return;
        }
        if (!this.installUserGroup.equals(installUserGroup)) {
            throw new Exception(
                    "the user is already exist, but in the group with different name in different hosts,"
                            + " please set the user in the group with the same name, user="
                            + getInstallUser() + ", group=" + installUserGroup
                            + ", another host group=" + this.installUserGroup);
        }
    }
}
