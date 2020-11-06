package com.sequoiacm.tools;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.command.ScmAlterWorkspaceToolImpl;
import com.sequoiacm.tools.command.ScmAttachRoleToolImpl;
import com.sequoiacm.tools.command.ScmCreateNodeToolImpl;
import com.sequoiacm.tools.command.ScmCreateRoleToolImpl;
import com.sequoiacm.tools.command.ScmCreateSiteToolImpl;
import com.sequoiacm.tools.command.ScmCreateUserToolImpl;
import com.sequoiacm.tools.command.ScmCreateWsToolImpl;
import com.sequoiacm.tools.command.ScmDeleteNodeToolImpl;
import com.sequoiacm.tools.command.ScmDeleteRoleToolImpl;
import com.sequoiacm.tools.command.ScmDeleteSiteToolImpl;
import com.sequoiacm.tools.command.ScmDeleteUserToolImpl;
import com.sequoiacm.tools.command.ScmDeleteWorkspaceToolImpl;
import com.sequoiacm.tools.command.ScmGrantRoleToolImpl;
import com.sequoiacm.tools.command.ScmListPrivilege;
import com.sequoiacm.tools.command.ScmListRoleImpl;
import com.sequoiacm.tools.command.ScmListSiteToolImpl;
import com.sequoiacm.tools.command.ScmListUserImpl;
import com.sequoiacm.tools.command.ScmListWorkspaceToolImpl;
import com.sequoiacm.tools.command.ScmPasswordEncryptor;
import com.sequoiacm.tools.command.ScmResetPassword;
import com.sequoiacm.tools.command.ScmRevokeRoleToolImpl;

public class ScmAdmin {
    // deleteuser deleterole revokerole

    public static void main(String[] args) throws ScmToolsException {
        CommandManager cmd = new CommandManager("scmadmin");
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
            cmd.addTool(new ScmCreateNodeToolImpl());
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
