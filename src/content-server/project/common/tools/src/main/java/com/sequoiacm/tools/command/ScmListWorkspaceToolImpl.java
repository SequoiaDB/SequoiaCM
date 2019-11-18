package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.common.ScmMetaMgr;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmWorkspaceInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;
import com.sequoiacm.tools.printor.ScmWorkspaceInfoPrinter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class ScmListWorkspaceToolImpl implements ScmTool {
    private Options ops;
    private ScmHelpGenerator hp;

    public ScmListWorkspaceToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt("n", "name", "workspace name, default:list all workspaces.",
                false, true, false));
        ScmCommandUtil.addDsOption(ops, hp);
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String name = null;
        if (cl.hasOption("n")) {
            name = cl.getOptionValue("n");
        }
        ScmSdbInfo mainSiteSdb = ScmCommandUtil.parseDsOption(cl);

        ScmMetaMgr mg = new ScmMetaMgr(mainSiteSdb.getSdbUrl(), mainSiteSdb.getSdbUser(),
                mainSiteSdb.getSdbPasswd());

        List<ScmWorkspaceInfo> list = new ArrayList<>();
        try {
            if (name != null) {
                ScmWorkspaceInfo siteInfo = mg.getWorkspaceInfoByName(name);
                if (siteInfo != null) {
                    list.add(siteInfo);
                }
            }
            else {
                list.addAll(mg.listWorkspace());

            }
            ScmWorkspaceInfoPrinter printer = new ScmWorkspaceInfoPrinter(list);
            printer.print();
            if (list.size() == 0) {
                throw new ScmToolsException(ScmExitCode.EMPTY_OUT);
            }
        }
        finally {
            mg.close();
        }

    }
}
