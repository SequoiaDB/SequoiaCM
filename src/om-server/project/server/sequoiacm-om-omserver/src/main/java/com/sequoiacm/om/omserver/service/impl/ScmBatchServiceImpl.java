package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import com.sequoiacm.om.omserver.dao.ScmBatchDao;
import com.sequoiacm.om.omserver.factory.ScmBatchDaoFactory;
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

    @Autowired
    private ScmBatchDaoFactory scmBatchDaoFactory;

    @Override
    public OmBatchDetail getBatch(ScmOmSession session, String wsName, String batchId)
            throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmBatchDao scmBatchDao = scmBatchDaoFactory.createScmBatchDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return scmBatchDao.getBatchDetail(wsName, batchId);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmBatchBasic> getBatchList(ScmOmSession session, String wsName, BSONObject filter,
            long skip, int limit) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmBatchDao scmBatchDao = scmBatchDaoFactory.createScmBatchDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return scmBatchDao.getBatchList(wsName, filter, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

}
