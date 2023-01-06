package com.sequoiacm.contentserver.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataDeletor;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class ScmFileDataDeleter {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileDataDeleter.class);
    private final Date dataCreateTime;
    private final String dataId;
    private final int dataType;
    private List<ScmFileLocation> siteList;
    private ScmWorkspaceInfo ws;

    public ScmFileDataDeleter(List<ScmFileLocation> siteList, ScmWorkspaceInfo ws,
            String dataId, int dataType, Date dataCreateTime) {
        this.siteList = siteList;
        this.ws = ws;
        this.dataId = dataId;
        this.dataType = dataType;
        this.dataCreateTime = dataCreateTime;
    }

    public void deleteData() throws ScmServerException {
        if (siteList == null || siteList.size() <= 0) {
            return;
        }
        int localSite = ScmContentModule.getInstance().getLocalSite();
        if (localSite != ScmContentModule.getInstance().getMainSite()) {
            List<Integer> siteIdList = new ArrayList<>();
            for (ScmFileLocation fileLocation : siteList) {
                siteIdList.add(fileLocation.getSiteId());
            }

            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(ScmContentModule.getInstance().getMainSiteName());
            client.deleteDataInSiteList(ws.getName(), dataId, dataType, dataCreateTime.getTime(),
                    siteIdList, siteList);
            return;
        }

        List<Integer> deleteFailedList = new ArrayList<>();
        for (ScmFileLocation site : siteList) {
            ScmDataInfo scmDataInfo = ScmDataInfo.forOpenExistData(dataType, dataId, dataCreateTime,
                    site.getWsVersion(), site.getTableName());
            try {
                if (site.getSiteId() == localSite) {
                    deleteDataLocal(scmDataInfo);
                    continue;
                }
                ScmInnerRemoteDataDeletor deleter = new ScmInnerRemoteDataDeletor(site.getSiteId(),
                        ws, scmDataInfo);
                deleter.delete();
            }
            catch (Exception e) {
                deleteFailedList.add(site.getSiteId());
                logger.warn("failed to delete data: siteId={}, dataInfo={}", site.getSiteId(),
                        scmDataInfo, e);
            }
        }
        if (deleteFailedList.size() > 0) {
            throw new ScmServerException(ScmError.DATA_DELETE_ERROR,
                    "failed to delete data in specify site list: specifySiteList=" + siteList
                            + ", failedSiteList=" + deleteFailedList);
        }
    }

    private void deleteDataLocal(ScmDataInfo dataInfo) throws ScmServerException {
        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), ws.getName(),
                    ws.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(), dataInfo);
            deletor.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                    "Failed to delete data: ws=" + ws.getName() + ", dataInfo=" + dataInfo, e);
        }
    }
}
