package com.sequoiacm.tools.command;

import java.util.Arrays;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmAlterWorkspaceToolImpl extends ScmTool {

    public ScmAlterWorkspaceToolImpl() {
        super("alterws");
    }
    @Override
    public void process(String[] args) throws ScmToolsException {
        if (args.length > 0) {
            ScmTool tool = getInstanceByToolName(args[0]);
            if (tool != null) {
                String[] toolsArgs = Arrays.copyOfRange(args, 1, args.length);
                tool.process(toolsArgs);
                return;
            }
            else {
                System.out.println("No such subcommand");
                printHelp(false);
            }
        }
        else {
            printHelp(false);
        }
        throw new ScmToolsException(ScmExitCode.INVALID_ARG);
    }

    private static ScmTool getInstanceByToolName(String toolName) throws ScmToolsException {
        ScmTool instance = null;
        if (toolName.equals("addsite")) {
            instance = new ScmAddSiteToWsToolmpl();
        }
        return instance;
    }

    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        System.out.println("usage: scmadmin alterws <subcommand> [args]");
        System.out.println("Available subcommands:");
        System.out.println("addsite");
        new ScmAddSiteToWsToolmpl().printHelp(isFullHelp);
    }
}
