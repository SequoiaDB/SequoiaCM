package com.sequoiacm.contentserver.dao;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ScmFileDataDeleter {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileDataDeleter.class);
    private List<Integer> siteList;
    private ScmWorkspaceInfo ws;
    private ScmDataInfo dataInfo;

    public ScmFileDataDeleter(List<Integer> siteList, ScmWorkspaceInfo ws, ScmDataInfo dataInfo) {
        this.siteList = siteList;
        this.ws = ws;
        this.dataInfo = dataInfo;
    }

    public void deleteData() throws ScmServerException {
        if (siteList == null || siteList.size() <= 0) {
            return;
        }
        int localSite = ScmContentModule.getInstance().getLocalSite();
        if (localSite != ScmContentModule.getInstance().getMainSite()) {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(ScmContentModule.getInstance().getMainSiteName());
            client.deleteDataInSiteList(ws.getName(), dataInfo.getId(), dataInfo.getType(),
                    dataInfo.getCreateTime().getTime(), siteList);
            return;
        }

        List<Integer> deleteFailedList = new ArrayList<>();
        for (Integer siteId : siteList) {
            try {
                if (siteId == localSite) {
                    deleteDataLocal();
                    continue;
                }
                ScmInnerRemoteDataDeletor deleter = new ScmInnerRemoteDataDeletor(siteId, ws,
                        dataInfo);
                deleter.delete();
            }
            catch (Exception e) {
                deleteFailedList.add(siteId);
                logger.warn("failed to delete data: siteId={}, dataInfo={}", siteId, dataInfo, e);
            }
        }
        if (deleteFailedList.size() > 0) {
            throw new ScmServerException(ScmError.DATA_DELETE_ERROR,
                    "failed to delete data in specify site list: specifySiteList=" + siteList
                            + ", failedSiteList=" + deleteFailedList);
        }
    }

    private void deleteDataLocal() throws ScmServerException {
        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), ws.getName(),
                    ws.getDataLocation(), ScmContentModule.getInstance().getDataService(),
                    dataInfo);
            deletor.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                    "Failed to delete data: ws=" + ws.getName() + ", dataInfo=" + dataInfo, e);
        }
    }
}
