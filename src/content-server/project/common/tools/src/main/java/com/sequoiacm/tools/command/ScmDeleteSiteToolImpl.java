package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.RestDispatcher;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;

public class ScmDeleteSiteToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeleteSiteToolImpl.class);

    private final String OPT_LONG_NAME = "name";
    private final String OPT_LONG_URL = "url";
    private final String OPT_LONG_USER = "user";
    private final String OPT_LONG_PASSWD = "password";
    private final String OPT_LONG_PASSWD_FILE = "password-file";

    private final String OPT_SHORT_NAME = "n";
    private ScmHelpGenerator hp;
    private Options ops;

    public ScmDeleteSiteToolImpl() throws ScmToolsException {
        super("deletesite");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "site name.", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_URL,
                "gateway url, eg:'host1:port,host2:port,host3:port'.", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_USER, "login admin username", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_PASSWD, "login admin password.", false, true,
                true, false, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_PASSWD_FILE, "login admin password file.", false,
                true, true, false, false));

    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        String siteName = cl.getOptionValue(OPT_LONG_NAME);
        String gatewayUrl = cl.getOptionValue(OPT_LONG_URL);
        ScmUserInfo adminUser = ScmCommandUtil.checkAndGetUser(cl, OPT_LONG_USER, OPT_LONG_PASSWD,
                OPT_LONG_PASSWD_FILE);
        ScmContentCommandUtil.checkArgInUriPath("siteName", siteName);

        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUser.getUsername(), adminUser.getPassword()));
            RestDispatcher.getInstance().deleteSite(ss, siteName);
            System.out.println("delete site success: siteName=" + siteName);
            logger.info("delete site success: siteName={}", siteName);
        }
        catch (Exception e) {
            logger.error("delete site failed: siteName={}", siteName, e);
            ScmCommon.throwToolException("delete site failed", e);
        }
        finally {
            ScmContentCommon.closeResource(ss);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }

}
