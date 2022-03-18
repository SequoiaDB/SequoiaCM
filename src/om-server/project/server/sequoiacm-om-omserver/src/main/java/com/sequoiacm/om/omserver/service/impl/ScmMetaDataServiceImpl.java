package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmMetaDataDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmMetaDataDaoFactory;
import com.sequoiacm.om.omserver.module.*;
import com.sequoiacm.om.omserver.service.ScmMetaDataService;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScmMetaDataServiceImpl implements ScmMetaDataService {

    @Autowired
    private ScmWorkspaceService wsService;

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmMetaDataDaoFactory scmMetaDataDaoFactory;

    @Override
    public List<OmClassBasic> listClass(ScmOmSession session, String wsName, BSONObject filter,
            BSONObject orderBy, int skip, int limit)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmMetaDataDao metaDataDao = scmMetaDataDaoFactory.createMetaDataDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return metaDataDao.getClassList(wsName, filter, orderBy, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public long getClassCount(ScmOmSession session, String wsName, BSONObject condition)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmMetaDataDao metaDataDao = scmMetaDataDaoFactory.createMetaDataDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return metaDataDao.countClass(wsName, condition);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmClassDetail getClassDetail(ScmOmSession session, String wsName, String id)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmMetaDataDao metaDataDao = scmMetaDataDaoFactory.createMetaDataDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return metaDataDao.getClassDetail(wsName, id);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }
}
