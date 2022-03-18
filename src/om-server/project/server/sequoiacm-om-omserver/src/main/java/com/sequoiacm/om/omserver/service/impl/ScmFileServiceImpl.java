package com.sequoiacm.om.omserver.service.impl;

import java.io.InputStream;
import java.util.List;

import com.sequoiacm.om.omserver.dao.ScmFileDao;
import com.sequoiacm.om.omserver.factory.ScmFileDaoFactory;
import com.sequoiacm.om.omserver.module.*;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.ScmFileService;
import com.sequoiacm.om.omserver.service.ScmSiteService;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Component
public class ScmFileServiceImpl implements ScmFileService {
    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmWorkspaceService wsService;

    @Autowired
    private ScmSiteService siteService;

    @Autowired
    private ScmFileDaoFactory scmFileDaoFactory;

    @Override
    public OmFileDetail getFileDetail(ScmOmSession session, String wsName, String id,
            int majorVersion, int minorVersion) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmFileDao fileDao = scmFileDaoFactory.createFileDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            OmFileDetail fileDetail = fileDao.getFileDetail(wsName, id, majorVersion, minorVersion);
            // reset site list, replace site id to site name
            for (OmFileDataSiteInfo site : fileDetail.getSites()) {
                site.setSiteName(siteService.getSiteById(session, site.getSiteId()));
            }
            return fileDetail;
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void uploadFile(ScmOmSession session, String wsName, String siteName,
            OmFileInfo fileInfo, BSONObject uploadConf, InputStream is)
            throws ScmInternalException, ScmOmServerException {
        ScmFileDao fileDao = scmFileDaoFactory.createFileDao(session);
        try {
            session.resetServiceEndpoint(siteName);
            fileDao.uploadFile(wsName, fileInfo, uploadConf, is);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmFileBasic> getFileList(ScmOmSession session, String wsName, int scope,
            BSONObject condition, BSONObject orderBy, long skip, long limit)
            throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmFileDao fileDao = scmFileDaoFactory.createFileDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return fileDao.getFileList(wsName, scope, condition, orderBy, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public long getFileCount(ScmOmSession session, String wsName, int scope, BSONObject condition)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmFileDao fileDao = scmFileDaoFactory.createFileDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return fileDao.countFile(wsName, scope, condition);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void deleteFiles(ScmOmSession session, String wsName, List<String> fileIdList)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.getRootSite();
        ScmFileDao fileDao = scmFileDaoFactory.createFileDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            fileDao.deleteFiles(wsName, fileIdList);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void updateFileContent(ScmOmSession session, String wsName, String id, String siteName,
            BSONObject updateContentOption, InputStream newFileContent)
            throws ScmOmServerException, ScmInternalException {
        ScmFileDao fileDao = scmFileDaoFactory.createFileDao(session);
        try {
            session.resetServiceEndpoint(siteName);
            fileDao.updateFileContent(wsName, id, updateContentOption, newFileContent);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmFileContent downloadFile(ScmOmSession session, String wsName, String siteName,
            String id, int majorVersion, int minorVersion)
            throws ScmInternalException, ScmOmServerException {
        ScmFileDao fileDao = scmFileDaoFactory.createFileDao(session);
        try {
            session.resetServiceEndpoint(siteName);
            return fileDao.downloadFile(wsName, id, majorVersion, minorVersion);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

}
