package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmTagDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmTagDaoFactory;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.tag.OmTagBasic;
import com.sequoiacm.om.omserver.module.tag.OmTagFilter;
import com.sequoiacm.om.omserver.service.ScmTagService;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmTagServiceImpl implements ScmTagService {

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmWorkspaceService wsService;

    @Autowired
    private ScmTagDaoFactory scmTagDaoFactory;

    @Override
    public List<OmTagBasic> listTag(ScmOmSession session, String wsName, String tagType,
            OmTagFilter omTagFilter, long skip, int limit)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmTagDao tagDao = scmTagDaoFactory.createTadDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return tagDao.listTag(wsName, tagType, omTagFilter, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public long countTag(ScmOmSession session, String wsName, String tagType,
            OmTagFilter omTagFilter) throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmTagDao tagDao = scmTagDaoFactory.createTadDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return tagDao.countTag(wsName, tagType, omTagFilter);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<String> listCustomTagKey(ScmOmSession session, String wsName, String keyMatcher,
            long skip, int limit) throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmTagDao tagDao = scmTagDaoFactory.createTadDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return tagDao.listCustomTagKey(wsName, keyMatcher, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmFileBasic> searchFileWithTag(ScmOmSession session, String wsName, int scope,
            BSONObject tagCondition, BSONObject fileCondition, BSONObject orderBy, long skip,
            long limit) throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmTagDao tagDao = scmTagDaoFactory.createTadDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return tagDao.searchFileWithTag(wsName, scope, tagCondition, fileCondition, orderBy,
                    skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public long countFileWithTag(ScmOmSession session, String wsName, int scope,
            BSONObject tagCondition, BSONObject fileCondition)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmTagDao tagDao = scmTagDaoFactory.createTadDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return tagDao.countFileWithTag(wsName, scope, tagCondition, fileCondition);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }
}
