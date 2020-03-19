package com.sequoiacm.s3.tools.command;

import com.sequoiacm.s3.tools.S3Admin;
import com.sequoiacm.s3.tools.S3Ctl;
import com.sequoiacm.s3.tools.exception.ScmExitCode;
import com.sequoiacm.s3.tools.exception.ScmToolsException;

public class ScmHelpToolImpl implements ScmTool {
    private Object tool;
    private boolean isFullHelp;

    public ScmHelpToolImpl(Object obj, boolean isFullHelp) {
        this.tool = obj;
        this.isFullHelp = isFullHelp;
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (tool.equals(S3Ctl.class)) {
            S3Ctl.checkHelpArgs(args);
            if (args.length >= 1) {
                S3Ctl.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(S3Ctl.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }
        else if (tool.equals(S3Admin.class)) {
            S3Admin.checkHelpArgs(args);
            if (args.length >= 1) {
                S3Admin.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(S3Admin.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }

        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        if (tool.equals(S3Ctl.class)) {
            System.out.println(S3Ctl.helpMsg);
        }
        else if (tool.equals(S3Admin.class)) {
            System.out.println(S3Admin.helpMsg);
        }
        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }



}
