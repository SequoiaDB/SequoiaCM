package com.sequoiacm.config.tools.command;

import com.sequoiacm.config.tools.ConfAdmin;
import com.sequoiacm.config.tools.ConfCtl;
import com.sequoiacm.config.tools.exception.ScmExitCode;
import com.sequoiacm.config.tools.exception.ScmToolsException;

public class ScmHelpToolImpl implements ScmTool {
    private Object tool;
    private boolean isFullHelp;

    public ScmHelpToolImpl(Object obj, boolean isFullHelp) {
        this.tool = obj;
        this.isFullHelp = isFullHelp;
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (tool.equals(ConfCtl.class)) {
            ConfCtl.checkHelpArgs(args);
            if (args.length >= 1) {
                ConfCtl.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(ConfCtl.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }
        else if (tool.equals(ConfAdmin.class)) {
            ConfAdmin.checkHelpArgs(args);
            if (args.length >= 1) {
                ConfAdmin.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(ConfAdmin.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }

        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        if (tool.equals(ConfCtl.class)) {
            System.out.println(ConfCtl.helpMsg);
        }
        else if (tool.equals(ConfAdmin.class)) {
            System.out.println(ConfAdmin.helpMsg);
        }
        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }



}
