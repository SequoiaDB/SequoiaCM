package com.sequoiacm.mq.tools.command;

import com.sequoiacm.mq.tools.MqAdmin;
import com.sequoiacm.mq.tools.MqCtl;
import com.sequoiacm.mq.tools.exception.ScmExitCode;
import com.sequoiacm.mq.tools.exception.ScmToolsException;

public class ScmHelpToolImpl implements ScmTool {
    private Object tool;
    private boolean isFullHelp;

    public ScmHelpToolImpl(Object obj, boolean isFullHelp) {
        this.tool = obj;
        this.isFullHelp = isFullHelp;
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (tool.equals(MqCtl.class)) {
            MqCtl.checkHelpArgs(args);
            if (args.length >= 1) {
                MqCtl.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(MqCtl.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }
        else if (tool.equals(MqAdmin.class)) {
            MqAdmin.checkHelpArgs(args);
            if (args.length >= 1) {
                MqAdmin.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(MqAdmin.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }

        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        if (tool.equals(MqCtl.class)) {
            System.out.println(MqCtl.helpMsg);
        }
        else if (tool.equals(MqAdmin.class)) {
            System.out.println(MqAdmin.helpMsg);
        }
        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }



}
