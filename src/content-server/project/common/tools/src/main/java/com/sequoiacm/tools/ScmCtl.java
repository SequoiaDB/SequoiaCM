package com.sequoiacm.tools;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.command.ScmListToolImpl;
import com.sequoiacm.tools.command.ScmReloadBizConfToolImpl;
import com.sequoiacm.tools.command.ScmStartToolImpl;
import com.sequoiacm.tools.command.ScmStopToolImpl;

public class ScmCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("scmctl");
        try {
            cmd.addTool(new ScmStartToolImpl());
            cmd.addTool(new ScmStopToolImpl());
            cmd.addTool(new ScmListToolImpl());
            cmd.addTool(new ScmReloadBizConfToolImpl());
        }
        catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
