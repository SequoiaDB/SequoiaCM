package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmCreateUserToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmCreateUserToolImpl.class);

    private final String LONG_OP_USER = "new-user";
    private final String LONG_OP_PASSWD = "new-password";
    private final String LONG_OP_PASSWD_TYPE = "password-type";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

    private ScmUserPasswordType passwdType = ScmUserPasswordType.LOCAL;

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmCreateUserToolImpl() throws ScmToolsException {
        super("createuser");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, LONG_OP_USER, "the name of new user.", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_PASSWD, "the password of new user.", true,
                true, true, false, false));
        ops.addOption(hp.createOpt(null, LONG_OP_PASSWD_TYPE,
                "password's type, default:LOCAL.\n" + "all supported type:'LOCAL', 'LDAP', 'TOKEN'",
                false, true, false));
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
        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        ScmUserInfo newUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_USER, LONG_OP_PASSWD,
                true);
        ScmUserInfo adminUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_ADMIN_USER,
                LONG_OP_ADMIN_PASSWD, LONG_OP_ADMIN_PASSWD_FILE);

        if (cl.hasOption(LONG_OP_PASSWD_TYPE)) {
            String tmpType = cl.getOptionValue(LONG_OP_PASSWD_TYPE);
            if (tmpType.equals("LDAP")) {
                passwdType = ScmUserPasswordType.LDAP;
            }
            else if (tmpType.equals("TOKEN")) {
                passwdType = ScmUserPasswordType.TOKEN;
            }
            else if (!tmpType.equals("LOCAL")) {
                throw new ScmToolsException("unreconigzed password type:type=" + tmpType,
                        ScmExitCode.INVALID_ARG);
            }
        }

        createUser(gatewayUrl, adminUserInfo, newUserInfo, passwdType);
        System.out.println("Create user success:" + newUserInfo.getUsername());
        logger.info("Create user success:" + newUserInfo.getUsername());
    }

    private void createUser(String gatewayUrl, ScmUserInfo adminUserInfo, ScmUserInfo newUserInfo,
            ScmUserPasswordType type) throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUserInfo.getUsername(), adminUserInfo.getPassword()));
            ScmFactory.User.createUser(ss, newUserInfo.getUsername(), type,
                    newUserInfo.getPassword());
        }
        catch (Exception e) {
            logger.error("create user failed:url={},admin={},newUser={}", gatewayUrl,
                    adminUserInfo.getUsername(), newUserInfo.getUsername(), e);
            throw new ScmToolsException("create user failed", ScmExitCode.SYSTEM_ERROR);
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
