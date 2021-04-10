package com.sequoiacm.tools;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.command.*;

public class ScmAdmin {
    // deleteuser deleterole revokerole

    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("scmadmin");
        ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = ScmNodeRequiredParamGroup.newBuilder()
                .addCloudParam().addZkParam().get();
        try {
            cmd.addTool(new ScmListPrivilege());
            cmd.addTool(new ScmRevokeRoleToolImpl());
            cmd.addTool(new ScmGrantRoleToolImpl());
            cmd.addTool(new ScmAttachRoleToolImpl());
            cmd.addTool(new ScmListRoleImpl());
            cmd.addTool(new ScmDeleteRoleToolImpl());
            cmd.addTool(new ScmCreateRoleToolImpl());
            cmd.addTool(new ScmListUserImpl());
            cmd.addTool(new ScmDeleteUserToolImpl());
            cmd.addTool(new ScmCreateUserToolImpl());
            cmd.addTool(new ScmDeleteNodeToolImpl());
            cmd.addTool(new ScmCreateNodeToolImpl(scmNodeRequiredParamGroup));
            cmd.addTool(new ScmAlterWorkspaceToolImpl());
            cmd.addTool(new ScmListWorkspaceToolImpl());
            cmd.addTool(new ScmDeleteWorkspaceToolImpl());
            cmd.addTool(new ScmCreateWsToolImpl());
            cmd.addTool(new ScmListSiteToolImpl());
            cmd.addTool(new ScmDeleteSiteToolImpl());
            cmd.addTool(new ScmCreateSiteToolImpl());
            cmd.addTool(new ScmPasswordEncryptor());
            cmd.addTool(new ScmResetPassword());
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.ADMIN_LOG_CONF);
        cmd.execute(args);
    }
}
