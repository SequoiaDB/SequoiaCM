package com.sequoiacm.deploy.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.common.RestTools;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.tools.common.ScmContentCommon;

public class WorkspaceOperater {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceOperater.class);

    private CommonConfig commonConfig = CommonConfig.getInstance();

    public void grantAllPriv(boolean dryrun) throws Exception {
        logger.info("Granting user with privileges{}...", dryrun ? "(Dry Run Mode)" : "");
        BSONObject bson = CommonUtils.parseJsonFile(commonConfig.getWorkspaceConfigFilePath());
        BasicBSONList wsBsons = BsonUtils.getArrayChecked(bson, "workspaces");

        String gatewayUrl = BsonUtils.getStringChecked(bson, "url");
        String user = BsonUtils.getStringChecked(bson, "userName");
        String password = BsonUtils.getStringChecked(bson, "password");

        if (dryrun) {
            for (Object wsBSON : wsBsons) {
                String ws = (String) ((BSONObject) wsBSON).get("name");
                logger.info("Grant privilege:ws={}, user={} privilege={}", ws, user,
                        ScmPrivilegeType.ALL);
            }
            logger.info("Grant user with privileges success");
            return;
        }

        waitDependentServicesReady(gatewayUrl);

        ScmSession ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(gatewayUrl, user, password));
        try {
            List<String> wsNames = new ArrayList<>();
            for (Object wsBSON : wsBsons) {
                wsNames.add((String) ((BSONObject) wsBSON).get("name"));
            }
            grantWsAllPriToUser(ss, wsNames);
        }
        finally {
            ss.close();
        }
        logger.info("Grant user with privileges success");

    }

    public void createWorkspace(boolean dryrun) throws Exception {
        logger.info("Creating workspace{}...", dryrun ? "(Dry Run Mode)" : "");
        BSONObject bson = CommonUtils.parseJsonFile(commonConfig.getWorkspaceConfigFilePath());
        BasicBSONList wsBsons = BsonUtils.getArrayChecked(bson, "workspaces");
        if (dryrun) {
            for (Object wsBSON : wsBsons) {
                String ws = (String) ((BSONObject) wsBSON).get("name");
                logger.info("Workspace will be create:" + ws);
            }
            logger.info("Create workspace success");
            return;
        }

        String gatewayUrl = BsonUtils.getStringChecked(bson, "url");
        String user = BsonUtils.getStringChecked(bson, "userName");
        String password = BsonUtils.getStringChecked(bson, "password");

        waitDependentServicesReady(gatewayUrl);

        ScmSession ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(gatewayUrl, user, password));
        try {
            List<String> wsNames = new ArrayList<>();
            for (Object wsBSON : wsBsons) {
                String ws = createWorkspace(ss, (BSONObject) wsBSON);
                wsNames.add(ws);
            }

        }
        finally {
            ss.close();
        }
        logger.info("Create workspace success");
    }

    private void waitDependentServicesReady(String gatewayUrl) throws Exception {
        String[] urlArr = gatewayUrl.split("/");
        if (urlArr.length != 2) {
            throw new IllegalArgumentException("invalid url, missing site name:" + gatewayUrl);
        }
        String siteName = urlArr[urlArr.length - 1];
        RestTools.waitDependentServiceReady(gatewayUrl, commonConfig.getWaitServiceReadyTimeout(),
                "config-server", "auth-server", siteName.toLowerCase());

    }

    private String createWorkspace(ScmSession ss, BSONObject wsBSON) throws Exception {
        logger.info("Creating workspace:{}", BsonUtils.getStringChecked(wsBSON, "name"));
        Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap = ScmContentCommon.querySite(ss);
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        // name
        conf.setName(BsonUtils.getStringChecked(wsBSON, "name"));
        conf.setDescription((String) wsBSON.get("description"));
        // meta
        conf.setMetaLocation(
                createMetaLocation(BsonUtils.getBSONObjectChecked(wsBSON, "meta"), siteMap));
        // data
        BasicBSONList datalocationsBSON = BsonUtils.getArrayChecked(wsBSON, "data");
        for (Object datalocationBSON : datalocationsBSON) {
            conf.addDataLocation(
                    ScmContentCommon.createDataLocation((BSONObject) datalocationBSON, siteMap));
        }

        conf.setBatchFileNameUnique(BsonUtils.getBooleanOrElse(wsBSON,
                FieldName.FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE, false));
        conf.setBatchIdTimePattern(
                BsonUtils.getString(wsBSON, FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN));
        conf.setBatchIdTimeRegex(
                BsonUtils.getString(wsBSON, FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX));
        String shardingTypeStr = BsonUtils.getStringOrElse(wsBSON,
                FieldName.FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE, ScmShardingType.NONE.getName());
        ScmShardingType shardingType = ScmShardingType.getShardingType(shardingTypeStr);
        if (shardingType == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "unknown batch sharding type:ws="
                    + conf.getName() + ", shardingType=" + shardingTypeStr);
        }
        conf.setBatchShardingType(shardingType);
        conf.setEnableDirectory(BsonUtils.getBooleanOrElse(wsBSON,
                FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, false));
        conf.setPreferred(BsonUtils.getString(wsBSON, FieldName.FIELD_CLWORKSPACE_PREFERRED));
        ScmSiteCacheStrategy siteCacheStrategy = ScmSiteCacheStrategy.getStrategy(
                BsonUtils.getStringOrElse(wsBSON, FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY,
                        ScmSiteCacheStrategy.ALWAYS.name()));
        conf.setSiteCacheStrategy(siteCacheStrategy);

        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        return ws.getName();
    }

    private void grantWsAllPriToUser(ScmSession ss, List<String> wss) throws Exception {
        for (String wsName : wss) {
            ScmRole role = null;
            try {
                role = ScmFactory.Role.createRole(ss, "ROLE_" + wsName, "");
            }
            catch (ScmException e1) {
                if (e1.getError() == ScmError.NETWORK_IO) {
                    throw e1;
                }
                try {
                    role = ScmFactory.Role.getRole(ss, "ROLE_" + wsName);
                }
                catch (Exception e2) {
                    logger.debug("create role failed, get role failed:{}", "ROLE_" + wsName, e2);
                    throw e1;
                }
            }
            grantWsAllPrivWithRetry(ss, role, wsName);
            ScmUser user = ScmFactory.User.getUser(ss, ss.getUser());
            ScmFactory.User.alterUser(ss, user, new ScmUserModifier().addRole(role));
            logger.info("Grant privilege success:ws={}, user={} privilege={}", wsName, ss.getUser(),
                    ScmPrivilegeType.ALL);
        }
    }

    private void grantWsAllPrivWithRetry(ScmSession ss, ScmRole role, String wsName)
            throws Exception {
        logger.info("Waiting for workspace ready:wsName={}, timeout={}ms", wsName,
                commonConfig.getWaitServiceReadyTimeout());
        long startTime = System.currentTimeMillis();
        Exception lastException;
        while (true) {
            try {
                ScmFactory.Role.grantPrivilege(ss, role,
                        ScmResourceFactory.createWorkspaceResource(wsName), ScmPrivilegeType.ALL);
                return;
            }
            catch (ScmException e) {
                if (e.getError() != ScmError.WORKSPACE_NOT_EXIST) {
                    throw e;
                }
                lastException = e;
            }

            if (System.currentTimeMillis() - startTime > commonConfig
                    .getWaitServiceReadyTimeout()) {
                throw new Exception("Failed to get workspace, timeout:ws=" + wsName + ", timeout="
                        + commonConfig.getWaitServiceReadyTimeout(), lastException);
            }
            Thread.sleep(5000);
        }
    }

    private ScmMetaLocation createMetaLocation(BSONObject metalocationBSON,
            Map<String, com.sequoiacm.client.element.ScmSiteInfo> siteMap) throws Exception {
        String siteName = (String) metalocationBSON
                .get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        com.sequoiacm.client.element.ScmSiteInfo site = siteMap.get(siteName);
        if (site == null) {
            throw new Exception("unknown location:" + metalocationBSON);
        }
        DatasourceType metaType = site.getMetaType();
        switch (metaType) {
            case SEQUOIADB:
                return new ScmSdbMetaLocation(metalocationBSON);
            default:
                throw new Exception("unknown siteType:siteName=" + siteName + ",type=" + metaType);
        }
    }

    public void cleanAll(boolean dryrun) throws Exception {
        logger.info("Cleaning workspace{}...", dryrun ? "(Dry Run Mode)" : "");
        ScmSession ss = null;
        try {
            BSONObject bson = CommonUtils.parseJsonFile(commonConfig.getWorkspaceConfigFilePath());
            String gatewayUrl = BsonUtils.getStringChecked(bson, "url");
            String user = BsonUtils.getStringChecked(bson, "userName");
            String password = BsonUtils.getStringChecked(bson, "password");

            if (!dryrun) {
                ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                        new ScmConfigOption(gatewayUrl, user, password));
            }

            BasicBSONList wsBsons = BsonUtils.getArrayChecked(bson, "workspaces");
            for (Object wsBSON : wsBsons) {
                String wsName = BsonUtils.getStringChecked((BSONObject) wsBSON, "name");
                if (dryrun) {
                    logger.info("Workspace {} will be delete", wsName);
                    continue;
                }

                try {
                    logger.info("Deleting workspace:" + wsName);
                    ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
                }
                catch (Exception e) {
                    logger.warn("Failed to delete workspace:" + wsName, e);
                }
            }
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            throw new Exception("Failed to clean workspace:" + e.getMessage(), e);
        }
        finally {
            CommonUtils.closeResource(ss);
        }
        logger.info("Clean workspace finish");
    }

    public void realCleanAll(boolean dryrun) throws Exception {
        logger.info("Cleaning workspace{}...", dryrun ? "(Dry Run Mode)" : "");
        ScmSession ss = null;
        try {
            BSONObject bson = CommonUtils.parseJsonFile(commonConfig.getWorkspaceConfigFilePath());
            String gatewayUrl = BsonUtils.getStringChecked(bson, "url");
            String user = BsonUtils.getStringChecked(bson, "userName");
            String password = BsonUtils.getStringChecked(bson, "password");

            if (!dryrun) {
                ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                        new ScmConfigOption(gatewayUrl, user, password));
            }
            ScmCursor<ScmWorkspaceInfo> list = ScmFactory.Workspace.listWorkspace(ss);
            while (list.hasNext()){
                String wsName = list.getNext().getName();
                if (dryrun) {
                    logger.info("Workspace {} will be delete", wsName);
                    continue;
                }

                try {
                    logger.info("Deleting workspace:" + wsName);
                    ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
                }
                catch (Exception e) {
                    logger.warn("Failed to delete workspace:" + wsName, e);
                }
            }
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            throw new Exception("Failed to clean workspace:" + e.getMessage(), e);
        }
        finally {
            CommonUtils.closeResource(ss);
        }
        logger.info("Clean workspace finish");
    }

}
