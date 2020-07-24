package com.sequoiacm.tools;

import java.util.Arrays;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.tools.command.ScmListToolImpl;
import com.sequoiacm.tools.command.ScmReloadBizConfToolImpl;
import com.sequoiacm.tools.command.ScmStartToolImpl;
import com.sequoiacm.tools.command.ScmStopToolImpl;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmCtl {
    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("scmctl");
        try {
            cmd.addTool(new ScmStartToolImpl());
            cmd.addTool(new ScmStopToolImpl());
            cmd.addTool(new ScmListToolImpl());
            cmd.addTool(new ScmReloadBizConfToolImpl());
        } catch (ScmToolsException e) {
            e.printStackTrace();
            System.exit(e.getExitCode());
        }
        cmd.execute(args);
    }
}
