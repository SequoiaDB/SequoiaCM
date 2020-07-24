package com.sequoiacm.tools.command;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmDeleteWorkspaceToolImpl extends ScmTool {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_FORCE = "f";
    private final String OPT_LONG_FORCE = "force";

    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private Options ops;
    private ScmHelpGenerator hp;

    private Logger logger = LoggerFactory.getLogger(ScmCreateWsToolImpl.class);

    public ScmDeleteWorkspaceToolImpl() throws ScmToolsException {
        super("deletews");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "workspace name.", true, true, false));
        ops.addOption(
                hp.createOpt(OPT_SHORT_FORCE, OPT_LONG_FORCE, "delete ws isenforced.", false, false, false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"localhost:8080/sitename\"", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_USER, "login username.", true, true, false));
        ops.addOption(
                hp.createOpt(null, LONG_OP_ADMIN_PASSWD, "login password.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String urls = cl.getOptionValue(LONG_OP_URL);
        String user = cl.getOptionValue(LONG_OP_ADMIN_USER);
        String pwd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        String wsName = cl.getOptionValue(OPT_SHORT_NAME);

        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(
                    new ScmConfigOption(ScmCommandUtil.parseListUrls(urls), user, pwd));
            if (cl.hasOption(OPT_SHORT_FORCE)) {
                ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
            }
            else {
                ScmFactory.Workspace.deleteWorkspace(ss, wsName);
            }
            logger.info("delete workspace success:wsName={}", wsName);
            System.out.println("delete workspace success:" + wsName);
        }
        catch (ScmException e) {
            logger.error("delete workspace failed:wsName={}, error=", wsName, e.getError(), e);
            throw new ScmToolsException("delete workspace failed:error=" + e.getError(),
                    ScmExitCode.SYSTEM_ERROR, e);
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
