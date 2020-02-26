package com.sequoiacm.om.tools.command;

import com.sequoiacm.om.tools.OmAdmin;
import com.sequoiacm.om.tools.OmCtl;
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
        if (tool.equals(OmCtl.class)) {
            OmCtl.checkHelpArgs(args);
            if (args.length >= 1) {
                OmCtl.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(OmCtl.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }
        else if (tool.equals(OmAdmin.class)) {
            OmAdmin.checkHelpArgs(args);
            if (args.length >= 1) {
                OmAdmin.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(OmAdmin.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }

        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        if (tool.equals(OmCtl.class)) {
            System.out.println(OmCtl.helpMsg);
        }
        else if (tool.equals(OmAdmin.class)) {
            System.out.println(OmAdmin.helpMsg);
        }
        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }



}
