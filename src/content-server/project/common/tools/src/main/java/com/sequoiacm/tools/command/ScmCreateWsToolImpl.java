package com.sequoiacm.tools.command;

import java.util.Map;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmCreateWsToolImpl extends ScmTool {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_META = "m";
    private final String OPT_LONG_META = "meta";
    private final String OPT_SHORT_DATA = "d";
    private final String OPT_LONG_DATA = "data";
    private final String OPT_LONG_DESC = "description";

    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";

    private Options ops;
    private ScmHelpGenerator hp;

    private Logger logger = LoggerFactory.getLogger(ScmCreateWsToolImpl.class);

    public ScmCreateWsToolImpl() throws ScmToolsException {
        super("createws");
        ops = new Options();
        hp = new ScmHelpGenerator();
        // name
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "workspace name.", true, true, false));
        // meta
        ops.addOption(hp.createOpt(OPT_SHORT_META, OPT_LONG_META,
                ScmCommandUtil.getMetaOptionDesc(), true, true, false));

        // data
        ops.addOption(hp.createOpt(OPT_SHORT_DATA, OPT_LONG_DATA,
                ScmCommandUtil.getDataOptionDesc(), true, true, false));

        // desc
        ops.addOption(
                hp.createOpt(null, OPT_LONG_DESC, "workspace description.", false, true, false));

        ops.addOption(hp.createOpt(null, LONG_OP_URL,
                "gateway url. exam:\"localhost:8080/sitename\"", true, true, false));
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

            ScmWorkspaceConf conf = new ScmWorkspaceConf();

            // name
            conf.setName(wsName);

            // desc
            if (cl.hasOption(OPT_LONG_DESC)) {
                conf.setDescription(cl.getOptionValue(OPT_LONG_DESC));
            }

            // meta
            BSONObject metaLocationBSON = getValueAsBSON(cl, OPT_SHORT_META);
            conf.setMetaLocation(createMetaLocation(metaLocationBSON, siteMap));

            // data
            BasicBSONList datalocationsBSON = (BasicBSONList) getValueAsBSON(cl, OPT_SHORT_DATA);
            for (Object datalocationBSON : datalocationsBSON) {
                conf.addDataLocation(
                        ScmCommon.createDataLocation((BSONObject) datalocationBSON, siteMap));
            }

            ScmFactory.Workspace.createWorkspace(ss, conf);
            logger.info("create workspace success:wsName={}", wsName);
            System.out.println("Create workspace success:" + wsName);
        }
        catch (ScmException e) {
            logger.error("create workspace failed:wsName={}, error=", wsName, e.getError(), e);
            throw new ScmToolsException("create workspace failed:error=" + e.getError(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(ss);
        }

    }

    private ScmMetaLocation createMetaLocation(BSONObject metalocationBSON,
            Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap)
            throws ScmToolsException, ScmInvalidArgumentException {
        String siteName = (String) metalocationBSON
                .get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        com.sequoiacm.client.element.ScmSiteInfo site = siteMap.get(siteName);
        if (site == null) {
            throw new ScmToolsException("unknown location:" + metalocationBSON,
                    ScmExitCode.INVALID_ARG);
        }
        DatasourceType metaType = site.getMetaType();
        switch (metaType) {
            case SEQUOIADB:
                return new ScmSdbMetaLocation(metalocationBSON);
            default:
                throw new ScmToolsException(
                        "unknown siteType:siteName=" + siteName + ",type=" + metaType,
                        ScmExitCode.INVALID_ARG);
        }
    }

    private BSONObject getValueAsBSON(CommandLine cl, String opt) throws ScmToolsException {
        String location = cl.getOptionValue(opt);
        try {
            BSONObject ret = (BSONObject) JSON.parse(location);
            return ret;
        }
        catch (Exception e) {
            throw new ScmToolsException("formate failed:location=" + location,
                    ScmExitCode.INVALID_ARG);
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
