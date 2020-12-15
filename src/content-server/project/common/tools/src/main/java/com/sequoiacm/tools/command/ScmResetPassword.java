package com.sequoiacm.tools.command;

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
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmResetPassword extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmResetPassword.class);

    private final String LONG_OP_OLD_PASSWORD = "old-password";
    private final String LONG_OP_OLD_PASSWORD_FILE = "old-password-file";
    private final String LONG_OP_NEW_PASSWORD = "new-password";
    private final String LONG_OP_RESET_USER = "reseted-user";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmResetPassword() throws ScmToolsException {
        super("resetpassword");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(null, LONG_OP_RESET_USER, "the name of user.", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_NEW_PASSWORD, "new password.", true, true, true,
                false, false));
        ops.addOption(hp.createOpt(null, LONG_OP_OLD_PASSWORD, "old password.", false, true, true,
                false, false));
        ops.addOption(hp.createOpt(null, LONG_OP_OLD_PASSWORD_FILE, "old password file.", false,
                true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"host1:8080,host2:8080,host3:8080\"", true, true, false));
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
        String gatewayUrl;
        if (cl.hasOption(LONG_OP_URL)) {
            gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        }
        else {
            throw new ScmToolsException("missing gateway url:--url", ScmExitCode.INVALID_ARG);
        }

        ScmUserInfo oldUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_RESET_USER,
                LONG_OP_OLD_PASSWORD, LONG_OP_OLD_PASSWORD_FILE, false, false);
        ScmUserInfo newUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_RESET_USER,
                LONG_OP_NEW_PASSWORD, true);
        ScmUserInfo adminUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_ADMIN_USER,
                LONG_OP_ADMIN_PASSWD, LONG_OP_ADMIN_PASSWD_FILE);

        resetPassword(oldUserInfo, newUserInfo.getPassword(), gatewayUrl, adminUserInfo);
        System.out.println("Reset password success:user=" + newUserInfo.getUsername());
        logger.info("Reset password success:user=" + newUserInfo.getUsername());
    }

    private void resetPassword(ScmUserInfo oldUser, String newPwd, String gatewayUrl,
            ScmUserInfo adminUser) throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUser.getUsername(), adminUser.getPassword()));
            ScmUser user = ScmFactory.User.getUser(ss, oldUser.getUsername());
            ScmUserModifier m = new ScmUserModifier();
            m.setPassword(oldUser.getPassword(), newPwd);
            ScmFactory.User.alterUser(ss, user, m);
        }
        catch (Exception e) {
            logger.error("reset password failed:url={},admin={},user={}", gatewayUrl,
                    adminUser.getUsername(), oldUser.getUsername(), e);
            ScmCommon.throwToolException("reset password failed", e);
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
