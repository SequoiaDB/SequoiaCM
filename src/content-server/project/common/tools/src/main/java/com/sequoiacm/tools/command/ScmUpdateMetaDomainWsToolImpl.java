package com.sequoiacm.tools.command;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmUpdateMetaDomainWsToolImpl extends ScmTool {
    private Logger logger = LoggerFactory.getLogger(ScmUpdateMetaDomainWsToolImpl.class);
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_META_DOMAIN = "metadomain";
    private final String OPT_SHORT_META_DOMAIN = "md";

    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmUpdateMetaDomainWsToolImpl() throws ScmToolsException {
        super("update-metadomain");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "workspace name.", true, true, false));

        ops.addOption(hp.createOpt(OPT_SHORT_META_DOMAIN, OPT_LONG_META_DOMAIN, "new domain name",
                true, true, false));

        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"localhost:8080/sitename\"", true, true, false));
        ops.addOption(
                hp.createOpt(null, LONG_OP_ADMIN_USER, "login admin username.", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_PASSWD, "login admin password.", false, true,
                true, false, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_PASSWD_FILE, "login admin password file.",
                false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        String urls = cl.getOptionValue(LONG_OP_URL);
        String wsName = cl.getOptionValue(OPT_SHORT_NAME);
        ScmUserInfo adminUser = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_ADMIN_USER,
                LONG_OP_ADMIN_PASSWD, LONG_OP_ADMIN_PASSWD_FILE);

        ScmSession ss = null;
        try {
            ss = ScmFactory.Session
                    .createSession(new ScmConfigOption(ScmContentCommandUtil.parseListUrls(urls),
                            adminUser.getUsername(), adminUser.getPassword()));

            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
            String metaDomain = cl.getOptionValue(OPT_LONG_META_DOMAIN);
            ws.updateMetaDomain(metaDomain);
            System.out.println("update workspace meta domain success: workspace=" + wsName
                    + ", newDomain=" + metaDomain);
        }
        catch (ScmException e) {
            logger.error("alter workspace failed:wsName={}, error={}", wsName, e.getError(), e);
            ScmCommon.throwToolException("alter workspace failed", e);
        }
        finally {
            if (ss != null) {
                ss.close();
            }
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) throws ScmToolsException {
        hp.printHelp(isFullHelp);
    }
}
