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
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmCreateUserToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmCreateUserToolImpl.class);

    private final String LONG_OP_USER = "new-user";
    private final String LONG_OP_PASSWD = "new-password";
    private final String LONG_OP_PASSWD_TYPE = "password-type";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private String adminUser;
    private String adminPasswd;
    private ScmUserPasswordType passwdType = ScmUserPasswordType.LOCAL;

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmCreateUserToolImpl() throws ScmToolsException {
        super("createuser");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, LONG_OP_USER, "the name of new user.", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_PASSWD, "the password of new user.", true, true,
                false));
        ops.addOption(hp.createOpt(null, LONG_OP_PASSWD_TYPE, "password's type, default:LOCAL.\n"
                + "all supported type:'LOCAL', 'LDAP', 'TOKEN'", false, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_URL, "gateway url. exam:\"host1:8080,host2:8080,host3:8080\"", true,
                true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_USER, "login username.", true, true, false));
        ops.addOption(hp
                .createOpt(null, LONG_OP_ADMIN_PASSWD, "login password.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String user = cl.getOptionValue(LONG_OP_USER);
        String passwd = cl.getOptionValue(LONG_OP_PASSWD);

        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);

        adminPasswd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        adminUser = cl.getOptionValue(LONG_OP_ADMIN_USER);

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

        createUser(user, passwd, passwdType, gatewayUrl);
        System.out.println("Create user success:" + user);
        logger.info("Create user success:" + user);
    }

    private void createUser(String user, String passwd, ScmUserPasswordType type, String gatewayUrl)
            throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    ScmCommandUtil.parseListUrls(gatewayUrl), adminUser, adminPasswd));
            ScmFactory.User.createUser(ss, user, type, passwd);
        }
        catch (Exception e) {
            logger.error("create user failed:url={},admin={},newUser={}", gatewayUrl, adminUser,
                    user, e);
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
