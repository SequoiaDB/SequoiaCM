package com.sequoiacm.contentserver.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.exception.ScmServerException;

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
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmSite remoteSiteInfo =contentModule.getSiteInfo(remoteSiteId);
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
