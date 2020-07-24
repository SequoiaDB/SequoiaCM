package com.sequoiacm.tools.command;

import java.util.Map;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
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
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmAddSiteToWsToolmpl extends ScmTool {
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_DATA = "data";
    private final String OPT_SHORT_DATA = "d";

    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

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
                ScmCommandUtil.getDataOptionDesc(), true, true, false));

        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"localhost:8080/sitename\"", true,
                true, false));
        ops.addOption(hp.createOpt(null, LONG_OP_ADMIN_USER, "login username.", true, true, false));
        ops.addOption(
                hp.createOpt(null, LONG_OP_ADMIN_PASSWD, "login password.", true, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {

        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);

        String urls = cl.getOptionValue(LONG_OP_URL);
        String user = cl.getOptionValue(LONG_OP_ADMIN_USER);
        String pwd = cl.getOptionValue(LONG_OP_ADMIN_PASSWD);
        String wsName = cl.getOptionValue(OPT_SHORT_NAME);

        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(
                    new ScmConfigOption(ScmCommandUtil.parseListUrls(urls), user, pwd));
            Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap = ScmCommon.querySite(ss);

            BasicBSONList dataLocationList = ScmCommandUtil.parseDataLocation(
                    cl.getOptionValue(OPT_LONG_DATA));

            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);

            for (Object location : dataLocationList) {
                ScmDataLocation scmDataLocation = ScmCommon.createDataLocation(
                        (BSONObject) location,
                        siteMap);
                ws.addDataLocation(scmDataLocation);
                System.out.println("Add site to workspace success,workspace:" + wsName
                        + ",siteName:" + scmDataLocation.getSiteName());
            }
        }
        catch (ScmException e) {
            logger.error("alter workspace failed:wsName={}, error=", wsName, e.getError(), e);
            throw new ScmToolsException("create workspace failed:error=" + e.getError(),
                    ScmExitCode.SYSTEM_ERROR, e);
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
