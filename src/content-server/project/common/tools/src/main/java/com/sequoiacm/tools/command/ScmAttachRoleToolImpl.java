package com.sequoiacm.tools.command;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmAttachRoleToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmAttachRoleToolImpl.class);

    private final String LONG_OP_USER = "attached-user";
    private final String SHORT_OP_ROLE = "r";
    private final String LONG_OP_ROLE = "role";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private String adminUser;
    private String adminPasswd;

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmAttachRoleToolImpl() throws ScmToolsException {
        super("attachrole");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, LONG_OP_USER, "the name of user.", true, true, false));
        ops.addOption(hp.createOpt(SHORT_OP_ROLE, LONG_OP_ROLE, "the name of role.", true, true,
                false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL, "gateway url. exam:\"host1:8080,host2:8080,host3:8080\"", true,
                true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_USER, "login username.", true, true, false));
        ops.addOption(hp
                .createOpt(null, LONG_OP_ADMIN_PASSWD, "login password.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String roleName = cl.getOptionValue(SHORT_OP_ROLE);
        String userName = cl.getOptionValue(LONG_OP_USER);

        String gatewayUrl;
        if (cl.hasOption(LONG_OP_URL)) {
            gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        }
        else {
            throw new ScmToolsException("missing gateway url:--url", ScmExitCode.INVALID_ARG);
        }

        adminPasswd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        adminUser = cl.getOptionValue(LONG_OP_ADMIN_USER);

        attachRole(userName, roleName, gatewayUrl);
        System.out.println("Attach role success:user=" + userName + ",roleName=" + roleName);
        logger.info("Attach role success:user=" + userName + ",roleName=" + roleName);
    }

    private void attachRole(String userName, String roleName, String gatewayUrl)
            throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    ScmCommandUtil.parseListUrls(gatewayUrl), adminUser, adminPasswd));
            ScmUser user = ScmFactory.User.getUser(ss, userName);
            ScmUserModifier m = new ScmUserModifier();
            m.addRole(roleName);
            ScmFactory.User.alterUser(ss, user, m);
        }
        catch (Exception e) {
            logger.error("attach role failed:url={},admin={},user={},role={}", gatewayUrl,
                    adminUser, userName, roleName, e);
            throw new ScmToolsException("attach role failed", ScmExitCode.SYSTEM_ERROR);
        }
        finally {
            if (null != ss) {
                ss.close();
            }
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
