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

public class ScmDeleteRoleToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmDeleteRoleToolImpl.class);

    private final String SHORT_OP_ROLE = "r";
    private final String LONG_OP_ROLE = "role";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmDeleteRoleToolImpl() throws ScmToolsException {
        super("deleterole");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(SHORT_OP_ROLE, LONG_OP_ROLE, "the name of role to be deleted.",
                true, true, false));
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
        String roleName = cl.getOptionValue(SHORT_OP_ROLE);
        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        ScmUserInfo adminUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_ADMIN_USER,
                LONG_OP_ADMIN_PASSWD, LONG_OP_ADMIN_PASSWD_FILE);

        deleteRole(roleName, gatewayUrl, adminUserInfo);
        System.out.println("Delete role success:" + roleName);
        logger.info("delete role sucess:role={}", roleName);
    }

    private void deleteRole(String roleName, String gatewayUrl, ScmUserInfo adminUser)
            throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUser.getUsername(), adminUser.getPassword()));
            ScmFactory.Role.deleteRole(ss, roleName);
        }
        catch (Exception e) {
            logger.error("delete role failed:url={},admin={},newRole={}", gatewayUrl,
                    adminUser.getPassword(), roleName, e);
            throw new ScmToolsException("delete role failed", ScmExitCode.SYSTEM_ERROR);
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