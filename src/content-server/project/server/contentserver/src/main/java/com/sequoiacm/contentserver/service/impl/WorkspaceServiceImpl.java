package com.sequoiacm.contentserver.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.dao.WorkspaceCreator;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.model.ClientLocationOutline;
import com.sequoiacm.contentserver.model.ClientWorkspaceUpdator;
import com.sequoiacm.contentserver.model.DataTableNameHistoryInfo;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.service.IPrivilegeService;
import com.sequoiacm.contentserver.service.IWorkspaceService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.DatalocationFactory;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaHistoryDataTableNameAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;

@Service
public class WorkspaceServiceImpl implements IWorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

    @Autowired
    IDatasourceService datasourceService;
    @Autowired
    IPrivilegeService privilegeService;

    @Override
    public BSONObject getWorkspace(String workspaceName) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(workspaceName);
        return ws.getBSONObject();
    }

    @Override
    public MetaCursor getWorkspaceList(BSONObject condition,BSONObject orderBy, long skip, long limit)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        MetaAccessor accessor = contentServer.getMetaService().getMetaSource()
                .getWorkspaceAccessor();
        try {
            return accessor.query(condition, null, orderBy, skip , limit);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get workspace list, condition=" + condition, e);
        }
    }

    @Override
    public BSONObject createWorkspace(String wsName, BSONObject wsConf, String createUser)
            throws ScmServerException {
        WorkspaceCreator creator = new WorkspaceCreator(wsName, createUser, wsConf);
        return creator.create();
    }

    @Override
    public void deleteWorkspace(String sessionId, String token, ScmUser user, String wsName,
            boolean isEnforced) throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        if (!contentserver.isInMainSite()) {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(contentserver.getMainSiteName());
            client.deleteWorkspace(sessionId, token, wsName, isEnforced);
            return;
        }

        if (!isEnforced) {
            ScmWorkspaceInfo ws = contentserver.getWorkspaceInfo(wsName);
            if (ws != null) {
                long fileCount = contentserver.getMetaService()
                        .getCurrentFileCount(ws, null);
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
        ContenserverConfClient.getInstance().deleteWorkspace(new WorkspaceFilter(wsName));

        cleanPrivilege(token, user, wsName);

        deleteDataTable(contentserver, wsName);

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

    @Async
    private void deleteDataTable(ScmContentServer coontentserver, String wsName) {
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
                if (siteName.equalsIgnoreCase(mySite.getName())) {
                    datasourceService.deleteDataTables(tableNameMap.get(siteName));
                }
                else {
                    ContentServerClient client = ContentServerClientFactory
                            .getFeignClientByServiceName(siteName.toLowerCase());
                    client.deleteDataTables(tableNameMap.get(siteName));
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
    public BSONObject updateWorkspace(String wsName, ClientWorkspaceUpdator updator)
            throws ScmServerException {
        WorkspaceUpdator confUpdator = validateUpdator(wsName, updator);
        WorkspaceConfig resp = ContenserverConfClient.getInstance()
                .updateWorkspaceConf(confUpdator);
        return resp.toBSONObject();
    }

    @Override
    public long countWorkspace(BSONObject condition) throws ScmServerException {
        try {
            ScmContentServer contentServer = ScmContentServer.getInstance();
            MetaAccessor accessor = contentServer.getMetaService().getMetaSource()
                    .getWorkspaceAccessor();
            return accessor.count(condition);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get workspace count, condition=" + condition, e);
        }
    }

    private WorkspaceUpdator validateUpdator(String wsName, ClientWorkspaceUpdator updator)
            throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfoChecked(wsName);

        WorkspaceUpdator confUpdator = new WorkspaceUpdator(wsName,
                contentserver.getWorkspaceInfoChecked(wsName).getBSONObject());

        confUpdator.setNewDesc(updator.getDescription());

        BasicBSONList updatedDatalocations = (BasicBSONList) wsInfo.getBSONObject()
                .get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        ClientLocationOutline addDataLocation = updator.getAddDataLocation();
        if (addDataLocation != null) {
            ScmSite addSite = contentserver.getSiteInfo(addDataLocation.getSiteName());
            if (addSite == null) {
                throw new ScmInvalidArgumentException(
                        "unknown data location:siteName=" + addDataLocation.getSiteName());
            }

            addDataLocation.addSiteId(addSite.getId());
            try {
                DatalocationFactory.createDataLocation(addSite.getDataUrl().getType(),
                        addDataLocation.toCompleteBSON());
            }
            catch (ScmDatasourceException e) {
                throw new ScmInvalidArgumentException(
                        "invlid data location:location=" + addDataLocation);
            }

            for (Object dataLocation : updatedDatalocations) {
                BSONObject dataLocationObj = (BSONObject) dataLocation;
                int id = (int) dataLocationObj.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
                if (id == addSite.getId()) {
                    throw new ScmInvalidArgumentException(
                            "duplicate data location:siteName=" + addDataLocation.getSiteName());
                }
            }
            confUpdator.setAddDataLocation(addDataLocation.toCompleteBSON());
        }

        String removeDataLocationSiteName = updator.getRemoveDataLocation();
        ScmSite removeSite = null;
        if (removeDataLocationSiteName != null) {
            removeSite = contentserver.getSiteInfo(removeDataLocationSiteName);
            if (removeSite == null) {
                throw new ScmInvalidArgumentException(
                        "unknown data location:siteName=" + removeDataLocationSiteName);
            }
            if (removeSite.isRootSite()) {
                throw new ScmInvalidArgumentException(
                        "can not remove root site:siteName=" + removeDataLocationSiteName);
            }

            boolean isRemoveSiteInLocationList = false;
            for (Object dataLocation : updatedDatalocations) {
                BSONObject dataLocationObj = (BSONObject) dataLocation;
                int id = (int) dataLocationObj.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
                if (id == removeSite.getId()) {
                    isRemoveSiteInLocationList = true;
                }
            }

            if (!isRemoveSiteInLocationList) {
                throw new ScmInvalidArgumentException(
                        "site not in data location list:siteName=" + removeSite.getName());
            }
            confUpdator.setRemoveDataLocationId(removeSite.getId());
        }
        return confUpdator;
    }


}
