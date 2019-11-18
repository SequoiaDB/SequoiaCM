package com.sequoiacm.contentserver.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.exception.ScmServerException;

public class ScmInnerRemoteDataDeletor {
    private static final Logger logger = LoggerFactory.getLogger(ScmInnerRemoteDataDeletor.class);

    private int remoteSiteId = 0;
    private ScmWorkspaceInfo wsInfo;
    private ScmDataInfo dataInfo;

    public ScmInnerRemoteDataDeletor(int remoteSiteId, ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo)
            throws ScmServerException {
        this.remoteSiteId = remoteSiteId;
        this.wsInfo = wsInfo;
        this.dataInfo = dataInfo;
    }

    public void delete() throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmSite remoteSiteInfo = contentserver.getSiteInfo(remoteSiteId);
        try {
            ContentServerClient c = ContentServerClientFactory.getFeignClientByServiceName(remoteSiteInfo.getName());
            c.deleteData(wsInfo.getName(), dataInfo.getId(), dataInfo.getType(), dataInfo.getCreateTime().getTime());
        }
        catch (Exception e) {
            logger.error("delete remote data failed:remote="
                    + remoteSiteInfo.getName() + ",ws=" + wsInfo.getName()
                    + ",dataId=" + dataInfo.getId(), e);
        }

    }
}
