package com.sequoiacm.fulltext.tools.command;

import com.sequoiacm.fulltext.tools.FulltextAdmin;
import com.sequoiacm.fulltext.tools.FulltextCtl;
import com.sequoiacm.fulltext.tools.exception.ScmExitCode;
import com.sequoiacm.fulltext.tools.exception.ScmToolsException;

public class ScmHelpToolImpl implements ScmTool {
    private Object tool;
    private boolean isFullHelp;

    public ScmHelpToolImpl(Object obj, boolean isFullHelp) {
        this.tool = obj;
        this.isFullHelp = isFullHelp;
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        if (tool.equals(FulltextCtl.class)) {
            FulltextCtl.checkHelpArgs(args);
            if (args.length >= 1) {
                FulltextCtl.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(FulltextCtl.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }
        else if (tool.equals(FulltextAdmin.class)) {
            FulltextAdmin.checkHelpArgs(args);
            if (args.length >= 1) {
                FulltextAdmin.printHelp(args[0], isFullHelp);
            }
            else {
                System.out.println(FulltextAdmin.helpMsg);
                System.exit(ScmExitCode.SUCCESS);
            }
        }

        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }

    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        if (tool.equals(FulltextCtl.class)) {
            System.out.println(FulltextCtl.helpMsg);
        }
        else if (tool.equals(FulltextAdmin.class)) {
            System.out.println(FulltextAdmin.helpMsg);
        }
        else {
            throw new ScmToolsException("Unkonw tool's class", ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }



}
