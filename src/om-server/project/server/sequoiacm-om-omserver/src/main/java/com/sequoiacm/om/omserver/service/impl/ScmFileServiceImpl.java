package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileContent;
import com.sequoiacm.om.omserver.module.OmFileDataSiteInfo;
import com.sequoiacm.om.omserver.module.OmFileDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
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

    @Override
    public OmFileDetail getFileDetail(ScmOmSession session, String ws, String id, int majorVersion,
            int minorVersion) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorksapceDetail(session, ws);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        try {
            synchronized (session) {
                session.resetServiceEndpoint(preferSite);
                OmFileDetail fileDetail = session.getFileDao().getFileDetail(ws, id, majorVersion,
                        minorVersion);
                // reset site list, replace site id to site name
                for (OmFileDataSiteInfo site : fileDetail.getSites()) {
                    site.setSiteName(siteService.getSiteById(session, site.getSiteId()));
                }
                return fileDetail;
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmFileBasic> getFileList(ScmOmSession session, String ws, BSONObject condition,
            long skip, long limit) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorksapceDetail(session, ws);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        try {
            synchronized (session) {
                session.resetServiceEndpoint(preferSite);
                return session.getFileDao().getFileList(ws, condition, skip, limit);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmFileContent downloadFile(ScmOmSession session, String ws, String id, int majorVersion,
            int minorVersion) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = wsService.getWorksapceDetail(session, ws);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        try {
            synchronized (session) {
                session.resetServiceEndpoint(preferSite);
                return session.getFileDao().downloadFile(ws, id, majorVersion, minorVersion);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

}
