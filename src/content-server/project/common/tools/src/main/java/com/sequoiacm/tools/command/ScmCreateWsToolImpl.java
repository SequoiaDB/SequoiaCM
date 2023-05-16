package com.sequoiacm.tools.command;

import java.util.Map;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.common.ScmSiteCacheStrategy;
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
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmCreateWsToolImpl extends ScmTool {
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_META = "m";
    private final String OPT_LONG_META = "meta";
    private final String OPT_SHORT_DATA = "d";
    private final String OPT_LONG_DATA = "data";
    private final String OPT_LONG_DESC = "description";
    private final String OPT_LONG_BATCH_SHARDING_TYPE = "batch-sharding-type";
    private final String OPT_LONG_BATCH_ID_TIME_REGEX = "batch-id-time-regex";
    private final String OPT_LONG_BATCH_ID_TIME_PATTERN = "batch-id-time-pattern";
    private final String OPT_LONG_BATCH_FILE_NAME_UNIQUE = "batch-file-name-unique";
    private final String OPT_LONG_DISABLE_DIRECTORY = "disable-directory";
    private final String OPT_LONG_ENABLE_DIRECTORY = "enable-directory";
    private final String OPT_LONG_PREFERRED = "preferred";
    private final String OPT_SHORT_PREFERRED = "p";
    private final String OPT_LONG_SITE_CACHE_STRATEGY = "site-cache-strategy";
    private final String OPT_LONG_ENABLE_TAG_RETRIEVAL = "enable-tag-retrieval";

    private final String LONG_OP_URL = "url";
    private final String LONG_OP_ADMIN_USER = "user";
    private final String LONG_OP_ADMIN_PASSWD = "password";
    private final String LONG_OP_ADMIN_PASSWD_FILE = "password-file";

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
                ScmContentCommandUtil.getMetaOptionDesc(), true, true, false));

        // data
        ops.addOption(hp.createOpt(OPT_SHORT_DATA, OPT_LONG_DATA,
                ScmContentCommandUtil.getDataOptionDesc(), true, true, false));

        // desc
        ops.addOption(
                hp.createOpt(null, OPT_LONG_DESC, "workspace description.", false, true, false));

        ops.addOption(hp.createOpt(OPT_SHORT_PREFERRED, OPT_LONG_PREFERRED,
                "strategy to choose site, only support to specify a site name now.", false, true,
                false));

        ops.addOption(hp.createOpt(null, OPT_LONG_BATCH_SHARDING_TYPE,
                "batch sharding type, default is none, all available sharding type: none, year, month, quarter.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_BATCH_ID_TIME_REGEX, "batch id time regex.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_BATCH_ID_TIME_PATTERN, "batch id time pattern.",
                false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_BATCH_FILE_NAME_UNIQUE,
                "set the file name is unique in the same batch.", false, false, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_DISABLE_DIRECTORY, "disable directory feature.",
                false, false, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ENABLE_DIRECTORY, "enable directory feature.",
                false, false, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_SITE_CACHE_STRATEGY,
                "workspace site cache strategy, all available strategy: always, never.", false,
                true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_ENABLE_TAG_RETRIEVAL,
                "enable tag retrieval feature.", false, false, false));

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
        if (cl.hasOption(OPT_LONG_DISABLE_DIRECTORY) && cl.hasOption(OPT_LONG_ENABLE_DIRECTORY)) {
            throw new ScmToolsException("do not specify --" + OPT_LONG_DISABLE_DIRECTORY + " and " + "--"
                    + OPT_LONG_ENABLE_DIRECTORY + " at the same time", ScmBaseExitCode.INVALID_ARG);
        }
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session
                    .createSession(new ScmConfigOption(ScmContentCommandUtil.parseListUrls(urls),
                            adminUser.getUsername(), adminUser.getPassword()));
            Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap = ScmContentCommon
                    .querySite(ss);

            ScmWorkspaceConf conf = new ScmWorkspaceConf();

            // name
            conf.setName(wsName);

            // desc
            if (cl.hasOption(OPT_LONG_DESC)) {
                conf.setDescription(cl.getOptionValue(OPT_LONG_DESC));
            }

            if (cl.hasOption(OPT_LONG_PREFERRED)) {
                conf.setPreferred(cl.getOptionValue(OPT_LONG_PREFERRED));
            }

            if (cl.hasOption(OPT_LONG_SITE_CACHE_STRATEGY)) {
                conf.setSiteCacheStrategy(ScmSiteCacheStrategy
                        .getStrategy(cl.getOptionValue(OPT_LONG_SITE_CACHE_STRATEGY)));
            }

            // meta
            BSONObject metaLocationBSON = getValueAsBSON(cl, OPT_SHORT_META);
            conf.setMetaLocation(createMetaLocation(metaLocationBSON, siteMap));

            // data
            BasicBSONList datalocationsBSON = (BasicBSONList) getValueAsBSON(cl, OPT_SHORT_DATA);
            for (Object datalocationBSON : datalocationsBSON) {
                conf.addDataLocation(ScmContentCommon
                        .createDataLocation((BSONObject) datalocationBSON, siteMap));
            }
            conf.setBatchFileNameUnique(cl.hasOption(OPT_LONG_BATCH_FILE_NAME_UNIQUE));
            conf.setBatchIdTimePattern(cl.getOptionValue(OPT_LONG_BATCH_ID_TIME_PATTERN));
            conf.setBatchIdTimeRegex(cl.getOptionValue(OPT_LONG_BATCH_ID_TIME_REGEX));
            String shardingTypeStr = cl.getOptionValue(OPT_LONG_BATCH_SHARDING_TYPE,
                    ScmShardingType.NONE.getName());
            ScmShardingType shardingType = ScmShardingType.getShardingType(shardingTypeStr);
            if (shardingType == null) {
                throw new ScmToolsException("invalid batch sharding type:" + shardingTypeStr,
                        ScmExitCode.INVALID_ARG);
            }
            conf.setBatchShardingType(shardingType);
            if (cl.hasOption(OPT_LONG_ENABLE_DIRECTORY)) {
                conf.setEnableDirectory(true);
            }
            if (cl.hasOption(OPT_LONG_DISABLE_DIRECTORY)) {
                conf.setEnableDirectory(false);
            }
            if (cl.hasOption(OPT_LONG_ENABLE_TAG_RETRIEVAL)) {
                conf.setEnableTagRetrieval(true);
            }

            ScmFactory.Workspace.createWorkspace(ss, conf);
            logger.info("create workspace success:wsName={}", wsName);
            System.out.println("Create workspace success:" + wsName);
        }
        catch (ScmException e) {
            logger.error("create workspace failed:wsName={}, error=", wsName, e.getError(), e);
            ScmCommon.throwToolException("create workspace failed", e);
        }
        finally {
            ScmContentCommon.closeResource(ss);
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
