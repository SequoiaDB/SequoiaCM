package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.common.printor.ListLine;
import com.sequoiacm.infrastructure.common.printor.ListTable;
import com.sequoiacm.infrastructure.common.printor.ScmCommonPrintor;
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
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmListUserImpl extends ScmTool {
    private static final Logger logger = LoggerFactory.getLogger(ScmListUserImpl.class);
    private final String LONG_OP_URL = "url";
    private final String LONG_OP_USER = "user";
    private final String LONG_OP_PASSWD = "password";
    private final String LONG_OP_PASSWD_FILE = "password-file";

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmListUserImpl() throws ScmToolsException {
        super("listuser");
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
        listUser(gatewayUrl, userInfo);
    }

    private void listUser(String gatewayUrl, ScmUserInfo adminUserInfo) throws ScmToolsException {
        ListTable t = new ListTable();
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUserInfo.getUsername(), adminUserInfo.getPassword()));
            ScmCursor<ScmUser> cursor = ScmFactory.User.listUsers(ss);
            while (cursor.hasNext()) {
                ScmUser u = cursor.getNext();
                ListLine l = new ListLine();
                l.addItem(u.getUsername());
                l.addItem(u.getUserId());

                StringBuilder sb = new StringBuilder();
                boolean isFirst = true;
                for (ScmRole role : u.getRoles()) {
                    if (!isFirst) {
                        sb.append(",").append(role.getRoleName());
                    }
                    else {
                        sb.append(role.getRoleName());
                        isFirst = false;
                    }
                }

                l.addItem(sb.toString());

                t.addLine(l);
            }

            List<String> header = new ArrayList<>();
            header.add("Name");
            header.add("Id");
            header.add("Roles");
            ScmCommonPrintor.print(header, t);

            if (t.size() == 0) {
                throw new ScmToolsException(ScmExitCode.EMPTY_OUT);
            }
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("list user failed:url={}", gatewayUrl, e);
            ScmCommon.throwToolException("list user failed", e);
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
