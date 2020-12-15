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
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ListLine;
import com.sequoiacm.tools.common.ListTable;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.printor.ScmCommonPrintor;

public class ScmListRoleImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmListRoleImpl.class);
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_USER = "user";
    private final String LONG_OP_PASSWD = "password";
    private final String LONG_OP_PASSWD_FILE = "password-file";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmListRoleImpl() throws ScmToolsException {
        super("listrole");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"host1:8080,host2:8080,host3:8080\"", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_USER, "login username.", true, true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_PASSWD, "login password.", false, true, true,
                false, false));
        ops.addOption(hp.createOpt(null, LONG_OP_PASSWD_FILE, "login password file.", false, true,
                false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        String gatewayUrl = cl.getOptionValue(LONG_OP_URL);
        ScmUserInfo userInfo = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_USER, LONG_OP_PASSWD,
                LONG_OP_PASSWD_FILE);
        listRole(gatewayUrl, userInfo);
    }

    private void listRole(String gatewayUrl, ScmUserInfo userInfo) throws ScmToolsException {
        ListTable t = new ListTable();
        ScmSession ss = null;
        int rc = ScmExitCode.SUCCESS;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            userInfo.getUsername(), userInfo.getPassword()));
            ScmCursor<ScmRole> cursor = ScmFactory.Role.listRoles(ss);
            while (cursor.hasNext()) {
                ScmRole r = cursor.getNext();
                ListLine l = new ListLine();
                l.addItem(r.getRoleName());
                l.addItem(r.getRoleId());
                l.addItem(r.getDescription());

                t.addLine(l);
            }

            if (t.size() == 0) {
                rc = ScmExitCode.EMPTY_OUT;
            }

            List<String> header = new ArrayList<>();
            header.add("Name");
            header.add("Id");
            header.add("Desc");
            ScmCommonPrintor.print(header, t);

            throw new ScmToolsException(rc);
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("list role failed:url={}", gatewayUrl, e);
            ScmCommon.throwToolException("list role failed", e);
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

    public static void main(String[] args) {
        ScmListRoleImpl listRole;
        try {
            listRole = new ScmListRoleImpl();
            listRole.process(new String[] { "--url", "192.168.20.92:8080/branchsite1" });
        }
        catch (ScmToolsException e) {
        }

    }
}
