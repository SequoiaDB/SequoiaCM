package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBatchBasic;
import com.sequoiacm.om.omserver.module.OmBatchDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.service.ScmBatchService;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Component
public class ScmBatchServiceImpl implements ScmBatchService {

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmWorkspaceService wsService;

    @Override
    public OmBatchDetail getBatch(ScmOmSession session, String wsName, String batchId)
            throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorksapceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);

        try {
            synchronized (session) {
                session.resetServiceEndpoint(preferSite);
                return session.getBatchDao().getBatchDetail(wsName, batchId);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmBatchBasic> getBatchList(ScmOmSession session, String wsName, BSONObject filter,
            long skip, int limit) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorksapceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        try {
            synchronized (session) {
                session.resetServiceEndpoint(preferSite);
                return session.getBatchDao().getBatchList(wsName, filter, skip, limit);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

}
