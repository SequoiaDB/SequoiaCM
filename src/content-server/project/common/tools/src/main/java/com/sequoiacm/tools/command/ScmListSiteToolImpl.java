package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmMetaMgr;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.printor.SiteInfoPrinter;

public class ScmListSiteToolImpl extends ScmTool {
    private Options options;
    private ScmHelpGenerator hp;

    public static void main(String[] args) throws ScmToolsException {
        String[] arg = { "--mdsurl", "192.168.31.32:11810" };
        new ScmListSiteToolImpl().process(arg);
    }

    public ScmListSiteToolImpl() throws ScmToolsException {
        super("listsite");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt("n", "name", "site name, default:list all site.", false, true, false));
        ScmContentCommandUtil.addDsOption(options, hp);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, options);
        String name = null;
        if (cl.hasOption("n")) {
            name = cl.getOptionValue("n");
        }

        ScmSdbInfo mainSiteSdb = ScmContentCommandUtil.parseDsOption(cl);

        ScmMetaMgr mg = new ScmMetaMgr(mainSiteSdb.getSdbUrl(), mainSiteSdb.getSdbUser(),
                mainSiteSdb.getSdbPasswd());

        List<ScmSiteInfo> list = new ArrayList<>();
        try {
            if (name != null) {
                ScmSiteInfo siteInfo = mg.getSiteInfoByName(name);
                if (siteInfo != null) {
                    list.add(siteInfo);
                }
            }
            else {
                list.addAll(mg.getSiteList());
            }
        }
        finally {
            mg.close();
        }

        SiteInfoPrinter printer = new SiteInfoPrinter(list);
        printer.print();
        if (list.size() == 0) {
            throw new ScmToolsException(ScmExitCode.EMPTY_OUT);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
