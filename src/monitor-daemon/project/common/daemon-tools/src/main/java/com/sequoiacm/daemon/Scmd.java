package com.sequoiacm.daemon;

import com.sequoiacm.daemon.command.*;
import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scmd {
    private static final Logger logger = LoggerFactory.getLogger(Scmd.class);

    public static void main(String[] args) {
        CommandManager cmd = new CommandManager("scmd");
        try {
            ScmCommon.configToolsLog(ScmCommon.LOG_FILE_ADMIN);
            cmd.addTool(new ScmStartToolImpl());
            cmd.addTool(new ScmStopToolImpl());
            cmd.addTool(new ScmAddToolImpl());
            cmd.addTool(new ScmListToolImpl());
            cmd.addTool(new ScmChStatusToolImpl());
            cmd.addTool(new ScmCronToolImpl());
        }
        catch (ScmToolsException e) {
            logger.error(e.getMessage() + ",please try exec the command again", e.getExitCode(),e);
            System.out.println(e.getMessage() + ",please try exec the command again");
            System.exit(e.getExitCode());
        }

        cmd.execute(args);
    }
}
