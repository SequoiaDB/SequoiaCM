package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.RestDispatcher;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class ScmDeleteSiteToolImpl implements ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeleteSiteToolImpl.class);

    private final String OPT_LONG_NAME = "name";
    private final String OPT_LONG_URL = "url";
    private final String OPT_LONG_USER = "user";
    private final String OPT_LONG_PASSWD = "password";

    private final String OPT_SHORT_NAME = "n";
    private ScmHelpGenerator hp;
    private Options ops;

    public ScmDeleteSiteToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "site name.", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_URL,
                "gateway url, eg:'host1:port,host2:port,host3:port'.", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_USER, "login admin username", true, true, false));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_PASSWD, "login admin username.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String siteName = cl.getOptionValue(OPT_LONG_NAME);
        String gatewayUrl = cl.getOptionValue(OPT_LONG_URL);
        String user = cl.getOptionValue(OPT_LONG_USER);
        String passwd = cl.getOptionValue(OPT_LONG_PASSWD);

        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(
                    new ScmConfigOption(ScmCommandUtil.parseListUrls(gatewayUrl), user, passwd));
            RestDispatcher.getInstance().deleteSite(ss, siteName);
            System.out.println("delete site success: siteName=" + siteName);
            logger.info("delete site success: siteName={}", siteName);
        }
        catch (Exception e) {
            logger.error("delete site failed: siteName={}", siteName, e);
            throw new ScmToolsException("delete site failed: siteName= " + siteName,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(ss);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

}
