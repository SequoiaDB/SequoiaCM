package com.sequoiacm.infrastructure.tool.command;

import com.sequoiacm.infrastructure.tool.CommandManager;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmHelpToolImpl extends ScmTool {
    private CommandManager cmd;

    public ScmHelpToolImpl(CommandManager cmd) {
        super("help");
        this.cmd = cmd;
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        this.cmd.checkHelpArgs(args);
        if (args.length >= 1) {
            this.cmd.printHelp(args[0], false);
        }
        else {
            System.out.println(this.cmd.getHelpMsg(false));
            System.exit(ScmBaseExitCode.SUCCESS);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        System.out.println(this.cmd.getHelpMsg(isFullHelp));
    }
}
