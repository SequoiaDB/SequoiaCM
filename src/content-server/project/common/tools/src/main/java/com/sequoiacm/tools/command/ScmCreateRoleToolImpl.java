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

public class ScmCreateRoleToolImpl implements ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmCreateRoleToolImpl.class);

    private final String SHORT_OP_ROLE = "r";
    private final String LONG_OP_ROLE = "role";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private String adminUser;
    private String adminPasswd;

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmCreateRoleToolImpl() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(SHORT_OP_ROLE, LONG_OP_ROLE, "the name of new role.", true,
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
        String roleName = cl.getOptionValue(SHORT_OP_ROLE);

        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        adminPasswd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        adminUser = cl.getOptionValue(LONG_OP_ADMIN_USER);

        createRole(roleName, gatewayUrl);
        System.out.println("Create role success:" + roleName);
        logger.info("create role sucess:role={}", roleName);
    }

    private void createRole(String roleName, String gatewayUrl) throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    ScmCommandUtil.parseListUrls(gatewayUrl), adminUser, adminPasswd));
            ScmFactory.Role.createRole(ss, roleName, "");
        }
        catch (Exception e) {
            logger.error("create role failed:url={},admin={},newRole={}", gatewayUrl, adminUser,
                    roleName, e);
            throw new ScmToolsException("create role failed", ScmExitCode.SYSTEM_ERROR);
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
