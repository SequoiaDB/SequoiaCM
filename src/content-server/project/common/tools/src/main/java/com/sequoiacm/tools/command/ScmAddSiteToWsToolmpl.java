package com.sequoiacm.tools.command;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;

public class ScmAddSiteToWsToolmpl extends ScmTool {
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_DATA = "data";
    private final String OPT_SHORT_DATA = "d";

    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

    private Logger logger = LoggerFactory.getLogger(ScmAddSiteToWsToolmpl.class);

    private Options ops;
    private ScmHelpGenerator hp;

    public ScmAddSiteToWsToolmpl() throws ScmToolsException {
        super("addsitetows");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "workspace name.", true, true, false));

        ops.addOption(hp.createOpt(OPT_SHORT_DATA, OPT_LONG_DATA,
                ScmContentCommandUtil.getDataOptionDesc(), true, true, false));

        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"localhost:8080/sitename\"", true, true, false));
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
        String urls = cl.getOptionValue(LONG_OP_URL);
        String wsName = cl.getOptionValue(OPT_SHORT_NAME);
        ScmUserInfo adminUser = ScmCommandUtil.checkAndGetUser(cl, LONG_OP_ADMIN_USER,
                LONG_OP_ADMIN_PASSWD, LONG_OP_ADMIN_PASSWD_FILE);

        ScmSession ss = null;
        try {
            ss = ScmFactory.Session
                    .createSession(new ScmConfigOption(ScmContentCommandUtil.parseListUrls(urls),
                            adminUser.getUsername(), adminUser.getPassword()));
            Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap = ScmContentCommon
                    .querySite(ss);

            BasicBSONList dataLocationList = ScmContentCommandUtil
                    .parseDataLocation(cl.getOptionValue(OPT_LONG_DATA));

            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);

            for (Object location : dataLocationList) {
                ScmDataLocation scmDataLocation = ScmContentCommon
                        .createDataLocation((BSONObject) location, siteMap);
                ws.addDataLocation(scmDataLocation);
                System.out.println("Add site to workspace success,workspace:" + wsName
                        + ",siteName:" + scmDataLocation.getSiteName());
            }
        }
        catch (ScmException e) {
            logger.error("alter workspace failed:wsName={}, error=", wsName, e.getError(), e);
            ScmCommon.throwToolException("alter workspace failed", e);
        }
        finally {
            if (ss != null) {
                ss.close();
            }
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
