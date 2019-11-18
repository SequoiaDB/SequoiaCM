package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ListLine;
import com.sequoiacm.tools.common.ListTable;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;
import com.sequoiacm.tools.printor.ScmCommonPrintor;

public class ScmListPrivilege implements ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmListPrivilege.class);
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ROLE = "role";
    private final String SHORT_OP_ROLE = "r";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private String adminUser;
    private String adminPasswd;

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmListPrivilege() throws ScmToolsException {
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(SHORT_OP_ROLE, LONG_OP_ROLE, "role name.", true, true, false));

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
        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        String roleName = cl.getOptionValue(LONG_OP_ROLE);
        adminUser = cl.getOptionValue(LONG_OP_ADMIN_USER);
        adminPasswd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        listPrivilege(gatewayUrl, roleName);
    }

    private void listPrivilege(String gatewayUrl, String roleName) throws ScmToolsException {
        ListTable t = new ListTable();
        ScmSession ss = null;
        int rc = ScmExitCode.SUCCESS;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    ScmCommandUtil.parseListUrls(gatewayUrl), adminUser, adminPasswd));
            ScmRole role = ScmFactory.Role.getRole(ss, roleName);

            ScmCursor<ScmPrivilege> cursor = ScmFactory.Privilege.listPrivileges(ss, role);
            while (cursor.hasNext()) {
                ScmPrivilege p = cursor.getNext();
                ListLine l = new ListLine();
                l.addItem(role.getRoleName());
                l.addItem(p.getId());

                ScmResource resource = p.getResource();
                l.addItem(resource.getType());
                l.addItem(resource.toStringFormat());

                l.addItem(p.getPrivilege());

                t.addLine(l);
            }

            if (t.size() == 0) {
                rc = ScmExitCode.EMPTY_OUT;
            }

            List<String> header = new ArrayList<>();
            header.add("RoleName");
            header.add("PrivId");
            header.add("ResourceType");
            header.add("Resource");
            header.add("Privilege");
            ScmCommonPrintor.print(header, t);

            throw new ScmToolsException(rc);
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("list privilege failed:url={}", gatewayUrl, e);
            throw new ScmToolsException("list privilege failed", ScmExitCode.SYSTEM_ERROR);
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