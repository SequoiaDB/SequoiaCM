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

public class ScmResetPassword extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmResetPassword.class);

    private final String LONG_OP_OLD_PASSWORD = "old-password";
    private final String LONG_OP_NEW_PASSWORD = "new-password";
    private final String LONG_OP_RESET_USER = "reseted-user";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private String adminUser;
    private String adminPasswd;

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmResetPassword() throws ScmToolsException {
        super("resetpassword");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(null, LONG_OP_RESET_USER, "the name of user.", false, true, false));
        ops.addOption(
                hp.createOpt(null, LONG_OP_NEW_PASSWORD, "new password.", true, true, false));
        ops.addOption(
                hp.createOpt(null, LONG_OP_OLD_PASSWORD, "old password.", false, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"host1:8080,host2:8080,host3:8080\"", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_USER, "login username.", true, true, false));
        ops.addOption(
                hp.createOpt(null, LONG_OP_ADMIN_PASSWD, "login password.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String oldPwd = cl.getOptionValue(LONG_OP_OLD_PASSWORD);
        String newPwd = cl.getOptionValue(LONG_OP_NEW_PASSWORD);

        String gatewayUrl;
        if (cl.hasOption(LONG_OP_URL)) {
            gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        }
        else {
            throw new ScmToolsException("missing gateway url:--url", ScmExitCode.INVALID_ARG);
        }

        adminPasswd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        adminUser = cl.getOptionValue(LONG_OP_ADMIN_USER);

        String resetUser = cl.getOptionValue(LONG_OP_RESET_USER);

        resetPassword(oldPwd, newPwd, resetUser, gatewayUrl);
        System.out.println("Reset password success:user=" + resetUser);
        logger.info("Reset password success:user=" + resetUser);
    }

    private void resetPassword(String oldPwd, String newPwd, String resetUser, String gatewayUrl)
            throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    ScmCommandUtil.parseListUrls(gatewayUrl), adminUser, adminPasswd));
            ScmUser user = ScmFactory.User.getUser(ss, resetUser);
            ScmUserModifier m = new ScmUserModifier();
            m.setPassword(oldPwd, newPwd);
            ScmFactory.User.alterUser(ss, user, m);
        }
        catch (Exception e) {
            logger.error("reset password failed:url={},admin={},user={}", gatewayUrl, adminUser,
                    resetUser, e);
            throw new ScmToolsException("reset password failed", ScmExitCode.SYSTEM_ERROR);
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
