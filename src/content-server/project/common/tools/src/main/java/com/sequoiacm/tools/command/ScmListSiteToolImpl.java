package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.common.ScmMetaMgr;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;
import com.sequoiacm.tools.printor.SiteInfoPrinter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class ScmListSiteToolImpl implements ScmTool {
    private Options options;
    private ScmHelpGenerator hp;

    public static void main(String[] args) throws ScmToolsException {
        String[] arg = { "--mdsurl", "192.168.31.32:11810" };
        new ScmListSiteToolImpl().process(arg);
    }

    public ScmListSiteToolImpl() throws ScmToolsException {
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt("n", "name", "site name, default:list all site.", false,
                true, false));
        ScmCommandUtil.addDsOption(options, hp);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);

        CommandLine cl = ScmCommandUtil.parseArgs(args, options);
        String name = null;
        if (cl.hasOption("n")) {
            name = cl.getOptionValue("n");
        }

        ScmSdbInfo mainSiteSdb = ScmCommandUtil.parseDsOption(cl);

        ScmMetaMgr mg = new ScmMetaMgr(mainSiteSdb.getSdbUrl(), mainSiteSdb.getSdbUser(),
                mainSiteSdb.getSdbPasswd());

        int rc = ScmExitCode.SUCCESS;
        List<ScmSiteInfo> list = new ArrayList<>();
        try {
            if (name != null) {
                ScmSiteInfo siteInfo = mg.getSiteInfoByName(name);
                if (siteInfo != null) {
                    list.add(siteInfo);
                }
                else {
                    rc = ScmExitCode.EMPTY_OUT;
                }
            }
            else {
                list.addAll(mg.getSiteList());
                if (list.size() == 0) {
                    rc = ScmExitCode.EMPTY_OUT;
                }
            }
        }
        finally {
            mg.close();
        }

        SiteInfoPrinter printer = new SiteInfoPrinter(list);
        printer.print();
        throw new ScmToolsException(rc);
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
