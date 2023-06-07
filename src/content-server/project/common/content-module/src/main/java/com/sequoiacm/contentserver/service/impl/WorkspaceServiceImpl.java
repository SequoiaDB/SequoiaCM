package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.config.ScmTagConfig;
import com.sequoiacm.contentserver.config.ScmWorkspaceTagRetrievalUpdaterConfig;
import com.sequoiacm.contentserver.dao.WorkspaceCreator;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnauthorizedException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ClientLocationOutline;
import com.sequoiacm.contentserver.model.ClientWorkspaceUpdator;
import com.sequoiacm.contentserver.model.DataTableDeleteOption;
import com.sequoiacm.contentserver.model.DataTableNameHistoryInfo;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.service.IPrivilegeService;
import com.sequoiacm.contentserver.service.IWorkspaceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.DatalocationFactory;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.sdbversion.EnableSdbVersionChecker;
import com.sequoiacm.infrastructure.sdbversion.SdbVersionChecker;
import com.sequoiacm.infrastructure.sdbversion.Version;
import com.sequoiacm.infrastructure.sdbversion.VersionFetcher;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaHistoryDataTableNameAccessor;
import com.sequoiacm.metasource.MetasourceVersion;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.Map.Entry;

@Service
@EnableSdbVersionChecker(versionFetcher = MetasourceVersionFetcher.class)
public class WorkspaceServiceImpl implements IWorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

    @Autowired
    IDatasourceService datasourceService;
    @Autowired
    IPrivilegeService privilegeService;

    @Autowired
    ScmAudit audit;

    @Autowired
    SdbVersionChecker sdbVersionChecker;

    @Autowired
    ScmTagConfig tagConfig;

    @Override
    public BSONObject getWorkspace(ScmUser user, String workspaceName) throws ScmServerException {
        audit.info(ScmAuditType.WS_DQL, user, workspaceName, 0,
                "get workspace by wsName=" + workspaceName);
        ScmWorkspaceInfo ws = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(workspaceName);
        return ws.getBSONObject();
    }

    @Override
    public MetaCursor getWorkspaceList(ScmUser user, BSONObject condition, BSONObject orderBy,
            long skip, long limit) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        MetaAccessor accessor = contentModule.getMetaService().getMetaSource()
                .getWorkspaceAccessor();
        try {
            MetaCursor ret = accessor.query(condition, null, orderBy, skip, limit);
            String auditMessage = "";
            if (condition != null) {
                auditMessage += " by filter=" + condition.toString();
            }
            audit.info(ScmAuditType.WS_DQL, user, null, 0, "get workspace list" + auditMessage);
            return ret;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get workspace list, condition=" + condition, e);
        }
    }

    @Override
    public BSONObject createWorkspace(ScmUser user, String wsName, BSONObject wsConf,
            String createUser) throws ScmServerException {
        if (!user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            throw new ScmOperationUnauthorizedException(
                    "permission denied:user=" + user.getUsername());
        }
        WorkspaceCreator creator = new WorkspaceCreator(wsName, createUser, wsConf,
                sdbVersionChecker, tagConfig);
        BSONObject ret = creator.create();

        audit.info(ScmAuditType.CREATE_WS, user, wsName, 0, "create workspace by wsConf=" + wsConf);
        return ret;
    }

    @Override
    public void deleteWorkspace(String sessionId, String token, ScmUser user, final String wsName,
            boolean isEnforced) throws ScmServerException {

        if (!user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            throw new ScmOperationUnauthorizedException(
                    "permission denied:user=" + user.getUsername());
        }
        final ScmContentModule contentModule = ScmContentModule.getInstance();
        if (!contentModule.isInMainSite()) {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(contentModule.getMainSiteName());
            client.deleteWorkspace(sessionId, token, wsName, isEnforced);

            audit.info(ScmAuditType.DELETE_WS, user, wsName, 0,
                    "delete wsName=" + wsName + ", isEnforced=" + isEnforced);
            return;
        }

        if (!isEnforced) {
            ScmWorkspaceInfo ws = contentModule.getWorkspaceInfo(wsName);
            if (ws != null) {
                long fileCount = contentModule.getMetaService().getCurrentFileCount(ws, null);
                if (fileCount > 0) {
                    throw new ScmServerException(ScmError.WORKSPACE_NOT_EMPTY,
                            "workspace is not empty:wsName=" + wsName);
                }
            }
            else {
                throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                        "workspace not exist:wsName=" + wsName);
            }
        }

        Map<Integer, ScmLocation> locations = null;
        ScmWorkspaceInfo workspaceInfo = contentModule.getWorkspaceInfo(wsName);
        if (workspaceInfo != null) {
            locations = workspaceInfo.getDataLocations();
        }

        ContenserverConfClient.getInstance().deleteWorkspace(new WorkspaceFilter(wsName));

        cleanPrivilege(token, user, wsName);
        if (locations != null) {
            Map<Integer, ScmLocation> locationMap = locations;
            AsyncUtils.execute(new Runnable() {
                @Override
                public void run() {
                    deleteDataTable(contentModule, wsName, locationMap);
                }
            });
        }

        audit.info(ScmAuditType.DELETE_WS, user, wsName, 0,
                "delete wsName=" + wsName + ", isEnforced=" + isEnforced);

    }

    private void cleanPrivilege(String token, ScmUser user, String wsName)
            throws ScmServerException {
        ScmFileServicePriv privClient = ScmFileServicePriv.getInstance();
        Map<String, ScmResource> resourcesMap = null;
        try {
            resourcesMap = privClient.getResourceMapFromAuthServer(wsName);
        }
        catch (Exception e) {
            logger.warn("failed to get resouces by workspace:workspace={}", wsName, e);
            return;
        }
        for (Entry<String, ScmResource> entry : resourcesMap.entrySet()) {
            ScmResource resource = entry.getValue();

            revoke(token, user, privClient, resource);
        }
    }

    private void revoke(String token, ScmUser user, ScmFileServicePriv privClient,
            ScmResource resource) {
        List<ScmPrivilege> privList = null;
        try {
            privList = privClient.listPrivilegeByResource(resource.getType(),
                    resource.getResource());
        }
        catch (Exception e) {
            logger.warn("failed to list privilege by resource:resource={}", resource, e);
            return;
        }
        for (ScmPrivilege priv : privList) {
            try {
                ScmRole role = privClient.findRoleById(priv.getRoleId());
                IResource iResource = privClient.createResource(resource.getType(),
                        resource.getResource());
                if (iResource == null) {
                    logger.warn("failed to revoke, failed to create IResource:resource={}",
                            resource);
                    return;
                }
                privClient.revoke(token, user, role.getRoleName(), iResource, priv.getPrivilege());
            }
            catch (Exception e) {
                logger.warn("failed ro revoke:roleId={},priveId={}", priv.getRoleId(), priv.getId(),
                        e);
            }
        }
    }

    private void deleteDataTable(ScmContentModule coontentserver, String wsName,
            Map<Integer, ScmLocation> locations) {
        // siteName map dataTableName list
        Map<String, List<String>> tableNameMap = new HashMap<>();

        MetaCursor c = null;
        MetaHistoryDataTableNameAccessor accessor = null;
        try {
            accessor = coontentserver.getMetaService().getMetaSource()
                    .getDataTableNameHistoryAccessor();
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.DataTableNameHistory.WORKSPACE_NAME, wsName);
            matcher.put(FieldName.DataTableNameHistory.WORKSPACE_IS_DELETED, true);
            c = accessor.query(matcher, null, null);
            while (c.hasNext()) {
                BSONObject record = c.getNext();
                DataTableNameHistoryInfo historyTableInfo = new DataTableNameHistoryInfo(record);
                List<String> tableNameList = tableNameMap.get(historyTableInfo.getSiteName());
                if (tableNameList == null) {
                    tableNameList = new ArrayList<>();
                    tableNameMap.put(historyTableInfo.getSiteName(), tableNameList);
                }
                tableNameList.add(historyTableInfo.getTableName());
            }
        }
        catch (Exception e) {
            logger.warn("failed to query history data table name, clear data table failed:ws={}",
                    wsName, e);
            return;
        }
        finally {
            if (c != null) {
                c.close();
            }
        }

        ScmSite mySite = coontentserver.getLocalSiteInfo();
        for (String siteName : tableNameMap.keySet()) {
            try {
                ScmSite siteInfo = coontentserver.getSiteInfo(siteName);
                if (null == siteInfo) {
                    throw new ScmServerException(ScmError.SITE_NOT_EXIST,
                            "site is not exist,siteName=" + siteName);
                }
                ScmLocation location = locations.get(siteInfo.getId());
                if (siteName.equalsIgnoreCase(mySite.getName())) {
                    datasourceService.deleteDataTables(tableNameMap.get(siteName), wsName,
                            location);
                }
                else {
                    ContentServerClient client = ContentServerClientFactory
                            .getFeignClientByServiceName(siteName.toLowerCase());
                    BSONObject res = client.deleteDataTablesKeepAlive(tableNameMap.get(siteName),
                            wsName, new DataTableDeleteOption(location.asBSON()));
                    ScmFeignExceptionUtils.handleException(res);
                }
            }
            catch (Exception e) {
                logger.warn("failed to clear data table:ws={},siteName={}", wsName, siteName, e);
                continue;
            }

            try {
                accessor.deleteHitoryDataTableName(wsName, siteName);
            }
            catch (Exception e) {
                logger.warn("failed to clear data table name record:ws={},siteName={}", wsName,
                        siteName, e);
            }
        }

    }

    @Override
    public BSONObject updateWorkspace(ScmUser user, String wsName, ClientWorkspaceUpdator updator)
            throws ScmServerException {
        if (!user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            audit.info(ScmAuditType.UPDATE_WS, user, wsName,
                    ScmError.OPERATION_UNAUTHORIZED.getErrorCode(),
                    "update workspace failed, permission denied: userName=" + user.getUsername());
            throw new ScmOperationUnauthorizedException(
                    "permission denied:user=" + user.getUsername());
        }
        WorkspaceUpdator confUpdator = validateUpdator(user.getUsername(), wsName, updator);
        WorkspaceConfig resp = ContenserverConfClient.getInstance()
                .updateWorkspaceConf(confUpdator);
        BSONObject ret = resp.toBSONObject();
        audit.info(ScmAuditType.UPDATE_WS, user, wsName, 0,
                "update wsName=" + wsName + ", updator=" + updator);
        return ret;
    }

    @Override
    public long countWorkspace(ScmUser user, BSONObject condition) throws ScmServerException {
        try {
            ScmContentModule contentModule = ScmContentModule.getInstance();
            MetaAccessor accessor = contentModule.getMetaService().getMetaSource()
                    .getWorkspaceAccessor();
            long ret = accessor.count(condition);
            String message = "count workspace";
            if (null != condition) {
                message += " by condition=" + condition.toString();
            }

            audit.info(ScmAuditType.WS_DQL, user, null, 0, message);
            return ret;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get workspace count, condition=" + condition, e);
        }
    }

    private WorkspaceUpdator validateUpdator(String userName, String wsName,
            ClientWorkspaceUpdator updator) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        List<Integer> dataLocationAfterUpdate = new ArrayList<>(wsInfo.getDataLocations().keySet());

        WorkspaceUpdator confUpdator = new WorkspaceUpdator(wsName,
                contentModule.getWorkspaceInfoCheckLocalSite(wsName).getBSONObject());
        confUpdator.setUpdateUser(userName);
        confUpdator.setUpdateTime(new Date().getTime());

        confUpdator.setNewDesc(updator.getDescription());
        confUpdator.setNewSiteCacheStrategy(updator.getSiteCacheStrategy() == null ? null
                : updator.getSiteCacheStrategy().name());
        confUpdator.setUpdateDomain(updator.getDomainName());

        Map<Integer, ScmLocation> oldDataLocations = wsInfo.getDataLocations();
        ClientLocationOutline addDataLocation = updator.getAddDataLocation();
        if (addDataLocation != null) {
            ScmSite addSite = contentModule.getSiteInfo(addDataLocation.getSiteName());
            if (addSite == null) {
                throw new ScmInvalidArgumentException(
                        "unknown data location:siteName=" + addDataLocation.getSiteName());
            }

            addDataLocation.addSiteId(addSite.getId());
            try {
                DatalocationFactory.createDataLocation(addSite.getDataUrl().getType(),
                        addDataLocation.toCompleteBSON(), addSite.getName());
            }
            catch (ScmDatasourceException e) {
                throw new ScmInvalidArgumentException(
                        "invalid data location:location=" + addDataLocation, e);
            }

            if (oldDataLocations.get(addSite.getId()) != null) {
                throw new ScmInvalidArgumentException(
                        "duplicate data location:siteName=" + addDataLocation.getSiteName());
            }
            confUpdator.setAddDataLocation(addDataLocation.toCompleteBSON());
            dataLocationAfterUpdate.add(addSite.getId());
            checkSiteStageTagValid(dataLocationAfterUpdate);
        }

        List<ClientLocationOutline> clientUpdateDataLocations = updator.getUpdateDataLocation();
        if (clientUpdateDataLocations != null && clientUpdateDataLocations.size() > 0) {
            BasicBSONList updateDataLocationBson = new BasicBSONList();
            for (ClientLocationOutline updateDataLocation : clientUpdateDataLocations) {
                ScmSite updateSite = contentModule.getSiteInfo(updateDataLocation.getSiteName());
                if (updateSite == null) {
                    throw new ScmInvalidArgumentException(
                            "unknown data location:siteName=" + updateDataLocation.getSiteName());
                }

                if (null == oldDataLocations.get(updateSite.getId())) {
                    throw new ScmInvalidArgumentException(
                            "site does not exist:siteName=" + updateDataLocation.getSiteName());
                }

                updateDataLocation.addSiteId(updateSite.getId());
                BSONObject mergedDataLocation;
                if (updator.getUpdateMerge()) {
                    mergedDataLocation = mergeDataLocation(updateDataLocation.toCompleteBSON(),
                            wsInfo.getSiteDataLocation(updateSite.getId()).asBSON());
                }
                else {
                    mergedDataLocation = updateDataLocation.toCompleteBSON();
                }

                try {
                    DatalocationFactory.createDataLocation(updateSite.getDataUrl().getType(),
                            mergedDataLocation, updateSite.getName());
                }
                catch (ScmDatasourceException e) {
                    throw new ScmInvalidArgumentException(
                            "invalid data location:location=" + mergedDataLocation, e);
                }

                updateDataLocationBson.add(mergedDataLocation);
            }

            confUpdator.setUpdateDataLocation(updateDataLocationBson);
        }

        String removeDataLocationSiteName = updator.getRemoveDataLocation();
        ScmSite removeSite = null;
        if (removeDataLocationSiteName != null) {
            removeSite = contentModule.getSiteInfo(removeDataLocationSiteName);
            if (removeSite == null) {
                throw new ScmInvalidArgumentException(
                        "unknown data location:siteName=" + removeDataLocationSiteName);
            }
            if (removeSite.isRootSite()) {
                throw new ScmInvalidArgumentException(
                        "can not remove root site:siteName=" + removeDataLocationSiteName);
            }

            if (null == oldDataLocations.get(removeSite.getId())) {
                throw new ScmInvalidArgumentException(
                        "site not in data location list:siteName=" + removeSite.getName());
            }

            if (removeSite.getName().equals(wsInfo.getPreferred())
                    && updator.getPreferred() == null) {
                updator.setPreferred(contentModule.getMainSiteName());
            }
            confUpdator.setRemoveDataLocationId(removeSite.getId());
            dataLocationAfterUpdate.remove(Integer.valueOf(removeSite.getId()));
        }

        if (updator.getPreferred() != null) {
            ScmSite preferredSite = contentModule.getSiteInfo(updator.getPreferred());
            if (preferredSite == null) {
                throw new ScmInvalidArgumentException(
                        "failed to update workspace, preferred site not found: workspace=" + wsName
                                + ", preferred=" + updator.getPreferred());
            }
            if (!dataLocationAfterUpdate.contains(preferredSite.getId())) {
                throw new ScmInvalidArgumentException(
                        "failed to update workspace, preferred site not in workspace data location list: workspace="
                                + wsName + ", preferred=" + updator.getPreferred());
            }
            confUpdator.setPreferred(updator.getPreferred());
        }
        if (updator.getEnableDirectory() != null) {
            confUpdator.setEnableDirectory(updator.getEnableDirectory());
        }
        return confUpdator;
    }

    private BSONObject mergeDataLocation(BSONObject clientDataLocation, BSONObject oldDataLocation)
            throws ScmInvalidArgumentException {
        if (null == oldDataLocation) {
            throw new ScmInvalidArgumentException("dataLocation does not exist: siteId ="
                    + clientDataLocation.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID));
        }

        BSONObject mergeBson = oldDataLocation;
        mergeDataLocationBSON(mergeBson, clientDataLocation);

        return mergeBson;
    }

    // merge bson from sourceBson to baseBson
    private void mergeDataLocationBSON(BSONObject baseBson, BSONObject sourceBson)
            throws ScmInvalidArgumentException {
        Set<String> sourceKeySet = sourceBson.keySet();

        for (String field : sourceKeySet) {
            Object tmp = sourceBson.get(field);
            if (tmp instanceof BasicBSONObject) {
                BSONObject tmpBson;
                if (baseBson.get(field) != null) {
                    tmpBson = (BSONObject) baseBson.get(field);
                }
                else {
                    tmpBson = new BasicBSONObject();
                }
                mergeDataLocationBSON(tmpBson, (BSONObject) tmp);
                baseBson.put(field, tmpBson);
            }
            else if (tmp instanceof BasicBSONList) {
                // BasicBSONList merge is not supported
                throw new ScmInvalidArgumentException(
                        "BasicBSONList copy is not supported. field =" + field);
            }
            else {
                baseBson.put(field, sourceBson.get(field));
            }
        }
    }

    private void checkSiteStageTagValid(List<Integer> dataLocationAfterUpdate)
            throws ScmInvalidArgumentException {
        Map<String, String> stageTagMap = new HashMap<>();
        ScmContentModule contentModule = ScmContentModule.getInstance();
        for (Integer siteId : dataLocationAfterUpdate) {
            ScmSite siteInfo = contentModule.getSiteInfo(siteId);
            if (StringUtils.hasText(siteInfo.getStageTag())) {
                if (stageTagMap.containsKey(siteInfo.getStageTag())) {
                    throw new ScmInvalidArgumentException(
                            "already exist same stage tag site, stagetag:" + siteInfo.getStageTag()
                                    + ", siteName:" + stageTagMap.get(siteInfo.getStageTag()));
                }
                stageTagMap.put(siteInfo.getStageTag(), siteInfo.getName());
            }
        }
    }

    @Override
    public BSONObject disabledTagRetrieval(ScmUser user, String ws) throws ScmServerException {
        if (!user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            audit.info(ScmAuditType.DELETE_WS, user, ws,
                    ScmError.OPERATION_UNAUTHORIZED.getErrorCode(),
                    "disable tag retrieval, permission denied: userName=" + user.getUsername());
            throw new ScmOperationUnauthorizedException(
                    "disable tag retrieval, permission denied:user=" + user.getUsername() + ", ws="
                            + ws);
        }
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresLock(ScmLockPathFactory.createWorkspaceTagRetrievalLock(ws));
        try {
            WorkspaceConfig wsConf = ContenserverConfClient.getInstance().getWorkspace(ws);
            if (wsConf == null) {
                throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                        "workspace not exist: workspace=" + ws);
            }

            if (Objects.equals(wsConf.getTagRetrievalStatus(),
                    ScmWorkspaceTagRetrievalStatus.DISABLED.getValue())) {
                throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                        "tag retrieval already disabled: workspace=" + ws);
            }

            WorkspaceUpdator updater = new WorkspaceUpdator(ws);
            updater.setTagRetrievalStatus(ScmWorkspaceTagRetrievalStatus.DISABLED.getValue());
            WorkspaceConfig ret = ContenserverConfClient.getInstance().updateWorkspaceConf(updater);

            if (Objects.equals(wsConf.getTagRetrievalStatus(),
                    ScmWorkspaceTagRetrievalStatus.INDEXING.getValue())) {
                cancelTaskSilence(wsConf);
            }

            dropTagLibFulltextIndexSilence(wsConf);
            return ret.toBSONObject();
        }
        finally {
            lock.unlock();
        }
    }

    private static void dropTagLibFulltextIndexSilence(WorkspaceConfig wsConf) {
        try {
            MetaAccessor tagLibAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().createMetaAccessor(wsConf.getTagLibTableName());
            tagLibAccessor.dropIndex("tag_lib_fulltext_idx");
        }
        catch (Exception e) {
            logger.warn("drop tag lib fulltext index failed, workspace: ws={}", wsConf.getWsName(),
                    e);
        }
    }

    private static void cancelTaskSilence(WorkspaceConfig wsConf) {
        try {
            Number taskId = BsonUtils.getNumber(wsConf.getExternalData(),
                    FieldName.FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK);
            if (taskId != null) {
                ScmContentModule.getInstance().getMetaService().getMetaSource()
                        .cancelMetaSourceTask(taskId.longValue(), true);
            }
            else {
                logger.warn(
                        "workspace tag retrieval status is indexing, but task id is null, workspace:{}",
                        wsConf.getWsName());
            }
        }
        catch (Exception e) {
            logger.warn("cancel task failed, workspace: ws={}, task={}", wsConf.getWsName(),
                    wsConf.getExternalData().get(FieldName.FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK),
                    e);
        }
    }

    @Override
    public BSONObject enableTagRetrieval(ScmUser user, String ws) throws ScmServerException {
        if (!user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            audit.info(ScmAuditType.DELETE_WS, user, ws,
                    ScmError.OPERATION_UNAUTHORIZED.getErrorCode(),
                    "enable tag retrieval, permission denied: userName=" + user.getUsername());
            throw new ScmOperationUnauthorizedException(
                    "enable tag retrieval, permission denied:user=" + user.getUsername() + ", ws="
                            + ws);
        }

        ScmLock lock = ScmLockManager.getInstance()
                .acquiresLock(ScmLockPathFactory.createWorkspaceTagRetrievalLock(ws));
        try {
            WorkspaceConfig wsConf = ContenserverConfClient.getInstance().getWorkspace(ws);
            if (wsConf == null) {
                throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                        "workspace not exist: workspace=" + ws);
            }

            if (!Objects.equals(wsConf.getTagRetrievalStatus(),
                    ScmWorkspaceTagRetrievalStatus.DISABLED.getValue())) {
                throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                        "enable tag retrieval failed, current status is "
                                + wsConf.getTagRetrievalStatus() + ", workspace=" + ws);
            }

            MetaAccessor tagLibMetaAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().createMetaAccessor(wsConf.getTagLibTableName());

            Long taskId = tagLibMetaAccessor.asyncCreateIndex("tag_lib_fulltext_idx",
                    FieldName.TagLib.tagLibFulltextIdxDef(),
                    FieldName.TagLib.tagLibFulltextIdxAttr(), null);
            if (taskId == null) {
                WorkspaceUpdator updater = new WorkspaceUpdator(ws);
                updater.setTagRetrievalStatus(ScmWorkspaceTagRetrievalStatus.ENABLED.getValue());
                WorkspaceConfig ret = ContenserverConfClient.getInstance()
                        .updateWorkspaceConf(updater);
                return ret.toBSONObject();
            }

            WorkspaceUpdator updater = new WorkspaceUpdator(ws);
            updater.setTagRetrievalStatus(ScmWorkspaceTagRetrievalStatus.INDEXING.getValue());
            updater.setExternalData(
                    new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK, taskId));
            WorkspaceConfig ret = ContenserverConfClient.getInstance().updateWorkspaceConf(updater);
            return ret.toBSONObject();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "enable tag retrieval failed: workspace=" + ws, e);
        }
        finally {
            lock.unlock();
        }
    }
}

@Component
class WorkspaceTagRetrievalStatusUpdater {
    private static final Logger logger = LoggerFactory
            .getLogger(WorkspaceTagRetrievalStatusUpdater.class);
    private final ScmTimer timer;

    public WorkspaceTagRetrievalStatusUpdater(ScmWorkspaceTagRetrievalUpdaterConfig config) {
        timer = ScmTimerFactory.createScmTimer("workspace-tag-retrieval-updater");
        timer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    updateWorkspaceTagRetrievalStatus();
                }
                catch (Throwable e) {
                    logger.error("update workspace tag retrieval status failed", e);
                }
            }
        }, config.getInterval(), config.getInterval());
    }

    @PreDestroy
    public void destroy() {
        timer.cancel();
    }

    private void updateWorkspaceTagRetrievalStatus() throws ScmServerException {
        WorkspaceFilter filter = new WorkspaceFilter();
        filter.setTagRetrievalStatus(ScmWorkspaceTagRetrievalStatus.INDEXING.getValue());
        List<WorkspaceConfig> indexingWsList = ContenserverConfClient.getInstance()
                .getWorkspace(filter);
        if (indexingWsList == null) {
            return;
        }
        for (WorkspaceConfig ws : indexingWsList) {
            try {
                check(ws.getWsName());
            }
            catch (Exception e) {
                logger.error("failed to check workspace index task status: ws" + ws.getWsName(), e);
            }
        }

    }

    private void check(String wsName) throws ScmServerException, ScmMetasourceException {
        ScmLock lock = null;
        try {
            lock = ScmLockManager.getInstance()
                    .acquiresLock(ScmLockPathFactory.createWorkspaceTagRetrievalLock(wsName));
            WorkspaceConfig ws = ContenserverConfClient.getInstance().getWorkspace(wsName);
            BSONObject extData = ws.getExternalData();
            if (extData == null) {
                logger.warn("workspace external data is null, workspace=" + ws.getWsName());
                return;
            }

            Number taskId = BsonUtils.getNumber(extData,
                    FieldName.FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK);
            if (taskId == null) {
                logger.warn(
                        "workspace external data task id is null, workspace tag retrieval status change to disable, workspace="
                                + ws.getWsName());
                updateWs(ws.getWsName(), ScmWorkspaceTagRetrievalStatus.DISABLED, null,
                        "task id not found in workspace ext data");
                return;
            }
            BSONObject task = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getMetaSourceTask(taskId.longValue());
            if (task == null) {
                logger.warn("workspace index task not found, workspace=" + ws.getWsName()
                        + ", taskId=" + taskId);
                updateWs(ws.getWsName(), ScmWorkspaceTagRetrievalStatus.DISABLED, null,
                        "task not found: " + taskId);
                return;
            }

            // Task 字段说明参考：SDB文档中心>参考手册>编目表>SYSTASKS集合
            Number taskStatus = BsonUtils.getNumberChecked(task, "Status");

            // cancel
            if (taskStatus.intValue() == 3) {
                updateWs(ws.getWsName(), ScmWorkspaceTagRetrievalStatus.DISABLED, null,
                        "index task canceled");
                return;
            }

            // complete
            if (taskStatus.intValue() == 9) {
                Number resultCode = BsonUtils.getNumber(task, "ResultCode");

                // 任务出错
                if (resultCode != null && resultCode.intValue() != 0) {
                    updateWs(ws.getWsName(), ScmWorkspaceTagRetrievalStatus.DISABLED, null,
                            "index task error: " + resultCode.intValue() + " "
                                    + task.get("ResultCodeDesc"));
                    return;
                }

                // 任务成功
                updateWs(ws.getWsName(), ScmWorkspaceTagRetrievalStatus.ENABLED, null, "");
            }
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private void updateWs(String wsName, ScmWorkspaceTagRetrievalStatus status, Long taskId,
            String taskErrorMsg) throws ScmServerException {
        WorkspaceUpdator updater = new WorkspaceUpdator(wsName);
        if (status != null) {
            updater.setTagRetrievalStatus(status.getValue());
        }

        BasicBSONObject newExt = new BasicBSONObject();
        if (taskId != null) {
            newExt.put(FieldName.FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK, taskId);
        }
        if (taskErrorMsg != null) {
            newExt.put(FieldName.FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK_ERROR, taskErrorMsg);
        }
        if (!newExt.isEmpty()) {
            updater.setExternalData(newExt);
        }
        ContenserverConfClient.getInstance().updateWorkspaceConf(updater);
    }
}

@Component
class MetasourceVersionFetcher implements VersionFetcher {

    @Override
    public Version fetchVersion() throws Exception {
        MetasourceVersion version = ScmContentModule.getInstance().getMetaService().getMetaSource()
                .getVersion();
        return new Version(version.getVersion(), version.getSubVersion(), version.getFixVersion());
    }
}
