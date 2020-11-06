package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmDeleteUserToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeleteUserToolImpl.class);

    private final String LONG_OP_USER = "delete-user";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmDeleteUserToolImpl() throws ScmToolsException {
        super("deleteuser");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, LONG_OP_USER, "the name of user to be deleted.", true,
                true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"host1:8080,host2:8080,host3:8080\"", true, true, false));
        ops.addOption(
                hp.createOpt(null, LONG_OP_ADMIN_USER, "login admin username.", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_PASSWD, "login admin password.", false,
                true, true, false, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_PASSWD_FILE, "login admin password file.",
                false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(LONG_OP_USER);
        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        ScmUserInfo adminUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_ADMIN_USER,
                LONG_OP_ADMIN_PASSWD, LONG_OP_ADMIN_PASSWD_FILE);

        deleteUser(user, gatewayUrl, adminUserInfo);
        System.out.println("Delete user success:" + user);
        logger.info("Delete user success:" + user);
    }

    private void deleteUser(String user, String gatewayUrl, ScmUserInfo adminUserInfo)
            throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUserInfo.getUsername(), adminUserInfo.getPassword()));
            ScmFactory.User.deleteUser(ss, user);
        }
        catch (Exception e) {
            logger.error("delete user failed:url={},admin={},user={}", gatewayUrl,
                    adminUserInfo.getUsername(), user, e);
            throw new ScmToolsException("delete user failed", ScmExitCode.SYSTEM_ERROR);
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
