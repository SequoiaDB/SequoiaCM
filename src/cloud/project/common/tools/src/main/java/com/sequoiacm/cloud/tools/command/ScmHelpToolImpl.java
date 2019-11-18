package com.sequoiacm.cloud.tools.command;

import com.sequoiacm.cloud.tools.ScmAdmin;
import com.sequoiacm.cloud.tools.ScmCtl;
import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.cloud.tools.exception.ScmToolsException;

public class ScmHelpToolImpl implements ScmTool {
    private Object tool;
    private boolean isFullHelp;

    public ScmHelpToolImpl(Object obj, boolean isFullHelp) {
        this.tool = obj;
        this.isFullHelp = isFullHelp;
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (tool.equals(ScmCtl.class)) {
            ScmCtl.checkHelpArgs(args);
            if (args.length >= 1) {
                ScmCtl.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(ScmCtl.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }
        else if (tool.equals(ScmAdmin.class)) {
            ScmAdmin.checkHelpArgs(args);
            if (args.length >= 1) {
                ScmAdmin.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(ScmAdmin.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }

        else {
            throw new ScmToolsException("Unknown tool's class", ScmExitCode.COMMON_UNKNOWN_ERROR);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        if (tool.equals(ScmCtl.class)) {
            System.out.println(ScmCtl.helpMsg);
        }
        else if (tool.equals(ScmAdmin.class)) {
            System.out.println(ScmAdmin.helpMsg);
        }
        else {
            throw new ScmToolsException("Unknown tool's class", ScmExitCode.COMMON_UNKNOWN_ERROR);
        }
    }



}
