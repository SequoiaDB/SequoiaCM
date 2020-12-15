package com.sequoiacm.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;

public class ScmGrantRoleToolImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmGrantRoleToolImpl.class);

    private final String SHORT_OP_ROLE = "r";
    private final String LONG_OP_ROLE = "role";
    private final String LONG_OP_RESOURCE_TYPE = "type";
    private final String LONG_OP_RESOURCE = "resource";
    private final String LONG_OP_PRIVILEGE = "privilege";
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

    private String resourceType = "workspace";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmGrantRoleToolImpl() throws ScmToolsException {
        super("grantrole");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(SHORT_OP_ROLE, LONG_OP_ROLE, "the name of role.", true, true, false));

        ops.addOption(
                hp.createOpt(null, LONG_OP_RESOURCE_TYPE,
                        "the type of resource, default:workspace. \n"
                                + "all supported types:'workspace','directory'.",
                        false, true, false));

        ops.addOption(hp.createOpt(null, LONG_OP_RESOURCE,
                "the resource to be granted. exam:'wsName' for workspace type \n"
                        + "or 'wsName:/root/dir1' for directory type",
                true, true, false));

        ops.addOption(
                hp.createOpt(null, LONG_OP_PRIVILEGE, "granted privilege. all supported value:\n"
                        + "'READ','CREATE', 'UPDATE', 'DELETE', 'ALL'", true, true, false));

        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"host1:8080/rootsite,host2:8080/rootsite,host3:8080/rootsite\", \"rootsite\" is root site's service name",
                true, true, false));
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
        String roleName = cl.getOptionValue(SHORT_OP_ROLE);
        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        String resource = cl.getOptionValue(LONG_OP_RESOURCE);
        String privilege = cl.getOptionValue(LONG_OP_PRIVILEGE);
        ScmUserInfo adminUserInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_ADMIN_USER,
                LONG_OP_ADMIN_PASSWD, LONG_OP_ADMIN_PASSWD_FILE);

        if (cl.hasOption(LONG_OP_RESOURCE_TYPE)) {
            resourceType = cl.getOptionValue(LONG_OP_RESOURCE_TYPE);
        }

        grantRole(roleName, resourceType, resource, privilege, gatewayUrl, adminUserInfo);
        System.out.println("Grant role success:role=" + roleName + ",resource=" + resource
                + ",privilege=" + privilege);
        logger.info("Grant role success:role=" + roleName + ",resource=" + resource + ",privilege="
                + privilege);
    }

    private void grantRole(String roleName, String resourceType, String resource, String privilege,
            String gatewayUrl, ScmUserInfo adminUser) throws ScmToolsException {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUser.getUsername(), adminUser.getPassword()));

            ScmResource r = ScmResourceFactory.createResource(resourceType, resource);
            ScmRole role = ScmFactory.Role.getRole(ss, roleName);
            ScmFactory.Role.grantPrivilege(ss, role, r, ScmPrivilegeType.getType(privilege));
        }
        catch (Exception e) {
            logger.error("grant role failed:url={},admin={},newRole={}", gatewayUrl,
                    adminUser.getUsername(), roleName, e);
            ScmCommon.throwToolException("grant role failed", e);
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