package com.sequoiacm.om.tools.command;

import com.sequoiacm.om.tools.SchAdmin;
import com.sequoiacm.om.tools.SchCtl;
import com.sequoiacm.om.tools.exception.ScmExitCode;
import com.sequoiacm.om.tools.exception.ScmToolsException;

public class ScmHelpToolImpl implements ScmTool {
    private Object tool;
    private boolean isFullHelp;

    public ScmHelpToolImpl(Object obj, boolean isFullHelp) {
        this.tool = obj;
        this.isFullHelp = isFullHelp;
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (tool.equals(SchCtl.class)) {
            SchCtl.checkHelpArgs(args);
            if (args.length >= 1) {
                SchCtl.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(SchCtl.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }
        else if (tool.equals(SchAdmin.class)) {
            SchAdmin.checkHelpArgs(args);
            if (args.length >= 1) {
                SchAdmin.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(SchAdmin.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }

        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        if (tool.equals(SchCtl.class)) {
            System.out.println(SchCtl.helpMsg);
        }
        else if (tool.equals(SchAdmin.class)) {
            System.out.println(SchAdmin.helpMsg);
        }
        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }



}
