package com.sequoiacm.contentserver.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.model.DataTableNameHistoryInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.DatalocationFactory;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;

public class WorkspaceCreator {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceCreator.class);
    private WorkspaceConfig wsConfig;
    private List<BSONObject> hdfsDataTableNameHistoryRecs = new ArrayList<>();
    private ScmContentServer contentserver;

    public WorkspaceCreator(String wsName, String createUser, BSONObject clientWsConfObj)
            throws ScmServerException {
        contentserver = ScmContentServer.getInstance();
        wsConfig = formate(wsName, createUser, clientWsConfObj);
    }

    public BSONObject create() throws ScmServerException {
        WorkspaceConfig resp = ContenserverConfClient.getInstance().createWorkspace(wsConfig);

        insertHdfsTableNameHistoryRecs();

        return resp.toBSONObject();
    }

    private void insertHdfsTableNameHistoryRecs() {
        if (hdfsDataTableNameHistoryRecs.size() <= 0) {
            return;
        }
        try {
            MetaAccessor accessor = contentserver.getMetaService().getMetaSource()
                    .getDataTableNameHistoryAccessor();
            for (BSONObject rec : hdfsDataTableNameHistoryRecs) {
                accessor.insert(rec);
            }
        }
        catch (Exception e) {
            logger.warn("failed record hdfs data path:wsName={}", wsConfig.getWsName(), e);
        }
    }

    private WorkspaceConfig formate(String wsName, String createUser, BSONObject clientWsConfObj)
            throws ScmInvalidArgumentException {
        WorkspaceConfig wsConfig = new WorkspaceConfig();
        wsConfig.setWsName(wsName);
        wsConfig.setCreateUser(createUser);
        wsConfig.setDesc((String) clientWsConfObj.get(FieldName.FIELD_CLWORKSPACE_DESCRIPTION));

        BSONObject metaLocationObj = (BSONObject) clientWsConfObj
                .get(FieldName.FIELD_CLWORKSPACE_META_LOCATION);

        BasicBSONList dataLocations = (BasicBSONList) clientWsConfObj
                .get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);

        // check location is valid
        String metaSiteName = (String) metaLocationObj
                .get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        if (metaSiteName == null) {
            throw new ScmInvalidArgumentException(
                    "failed to create workspace, metalocation missing site name:wsName=" + wsName
                    + ",metaLocation=" + metaLocationObj);
        }
        ScmSite metaSiteInfo = contentserver.getSiteInfo(metaSiteName);
        if (metaSiteInfo == null) {
            throw new ScmInvalidArgumentException("failed to create workspace, no such site:wsName="
                    + wsName + ",metalocation=" + metaLocationObj);
        }
        if (!metaSiteInfo.isRootSite()) {
            throw new ScmInvalidArgumentException(
                    "failed to create workspace,metalocation must be root site:wsName=" + wsName
                    + ",metalocation=" + metaLocationObj);
        }
        metaLocationObj.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        metaLocationObj.put(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID, metaSiteInfo.getId());

        try {
            new SdbMetaSourceLocation(metaLocationObj, null);
        }
        catch (SdbMetasourceException e) {
            throw new ScmInvalidArgumentException(
                    "failed to create workspace, invlid metalocation:wsName=" + wsName
                    + ",metaLocation=" + metaLocationObj,
                    e);
        }
        wsConfig.setMetalocation(metaLocationObj);

        boolean isDatalocationsContainRootSite = false;
        for (Object obj : dataLocations) {
            BSONObject dataLocationObj = (BSONObject) obj;
            String siteName = (String) dataLocationObj
                    .get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            ScmSite siteInfo = contentserver.getSiteInfo(siteName);
            if (siteInfo == null) {
                throw new ScmInvalidArgumentException(
                        "failed to create workspace, no such site:wsName=" + wsName + ",siteName="
                                + siteName);
            }
            dataLocationObj.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
            dataLocationObj.put(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID, siteInfo.getId());
            try {
                ScmLocation dataLocation = DatalocationFactory
                        .createDataLocation(siteInfo.getDataUrl().getType(), dataLocationObj);
                if (dataLocation instanceof HdfsDataLocation) {
                    HdfsDataLocation hdfsDataLocation = (HdfsDataLocation) dataLocation;
                    BSONObject historyRec = createHdfsTableNameRec(wsName,
                            hdfsDataLocation.getSiteId(), hdfsDataLocation.getRootPath());
                    hdfsDataTableNameHistoryRecs.add(historyRec);
                }
            }
            catch (ScmDatasourceException e) {
                throw new ScmInvalidArgumentException(
                        "failed to create workspace, invlid datalocation:wsName=" + wsName
                        + ",dataLocation=" + dataLocationObj);
            }
            if (siteInfo.isRootSite()) {
                isDatalocationsContainRootSite = true;
            }
        }
        if (!isDatalocationsContainRootSite) {
            throw new ScmInvalidArgumentException(
                    "failed to create workspace,data location must contain root site:wsName="
                            + wsName);
        }

        wsConfig.setDataLocations(dataLocations);

        return wsConfig;
    }

    private BSONObject createHdfsTableNameRec(String wsName, int siteId, String rootPath) {
        if (!rootPath.startsWith("/")) {
            rootPath = "/" + rootPath;
        }
        if (!rootPath.endsWith("/")) {
            rootPath = rootPath + "/";
        }
        rootPath += wsName;
        ScmSite site = contentserver.getSiteInfo(siteId);

        DataTableNameHistoryInfo record = new DataTableNameHistoryInfo();
        record.setSiteName(site.getName());
        record.setTableCreateTime(System.currentTimeMillis());
        record.setTableName(rootPath);
        record.setWsIsDeleted(false);
        record.setWsName(wsName);
        return record.toBSONObject();
    }

}