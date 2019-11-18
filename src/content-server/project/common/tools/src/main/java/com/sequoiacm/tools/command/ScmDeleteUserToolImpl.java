package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class ScmDeleteUserToolImpl implements ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeleteUserToolImpl.class);

    private final String LONG_OP_USER = "delete-user";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private String adminUser;
    private String adminPasswd;

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmDeleteUserToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, LONG_OP_USER, "the name of user to be deleted.", true,
                true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL, "gateway url. exam:\"host1:8080,host2:8080,host3:8080\"", true,
                true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_USER, "login username.", true, true, false));
        ops.addOption(hp
                .createOpt(null, LONG_OP_ADMIN_PASSWD, "login password.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(LONG_OP_USER);

        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);

        adminPasswd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        adminUser = cl.getOptionValue(LONG_OP_ADMIN_USER);

        deleteUser(user, gatewayUrl);
        System.out.println("Delete user success:" + user);
        logger.info("Delete user success:" + user);
    }

    private void deleteUser(String user, String gatewayUrl) throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    ScmCommandUtil.parseListUrls(gatewayUrl), adminUser, adminPasswd));
            ScmFactory.User.deleteUser(ss, user);
        }
        catch (Exception e) {
            logger.error("delete user failed:url={},admin={},newUser={}", gatewayUrl, adminUser,
                    user, e);
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
