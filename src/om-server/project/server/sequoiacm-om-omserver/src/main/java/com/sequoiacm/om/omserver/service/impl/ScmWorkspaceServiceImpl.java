package com.sequoiacm.om.omserver.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileDeltaStatistics;
import com.sequoiacm.om.omserver.module.OmFileTrafficStatistics;
import com.sequoiacm.om.omserver.module.OmCacheWrapper;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmWorkspaceServiceImpl implements ScmWorkspaceService {

    private Map<String, OmCacheWrapper<OmWorkspaceDetail>> workspacesCache = new ConcurrentHashMap<>();
    private long cacheTTL;

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    public ScmWorkspaceServiceImpl(ScmOmServerConfig config) {
        this.cacheTTL = config.getCacheRefreshIntreval() * 1000;
    }

    @Override
    public OmWorkspaceInfoWithStatistics getWorksapceDetailWithStatistics(ScmOmSession session,
            String workspaceName) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = getWorksapceDetail(session, workspaceName);

        OmWorkspaceInfoWithStatistics wsDetailWithStatistics = new OmWorkspaceInfoWithStatistics();
        wsDetailWithStatistics.setCreateTime(wsDetail.getCreateTime());
        wsDetailWithStatistics.setCreateUser(wsDetail.getCreateUser());
        wsDetailWithStatistics.setDataLocations(wsDetail.getDataLocations());
        wsDetailWithStatistics.setDescription(wsDetail.getDescription());
        wsDetailWithStatistics.setMetaOption(wsDetail.getMetaOption());
        wsDetailWithStatistics.setName(wsDetail.getName());
        wsDetailWithStatistics.setUpdateTime(wsDetail.getUpdateTime());
        wsDetailWithStatistics.setUpdateUser(wsDetail.getUpdateUser());

        OmFileDeltaStatistics fileDelta = session.getMonitorDao().getFileDelta(workspaceName);
        wsDetailWithStatistics.setFileSizeDelta(fileDelta.getSizeDelta());
        wsDetailWithStatistics.setFileCountDelta(fileDelta.getCountDelta());

        OmFileTrafficStatistics fileTraffic = session.getMonitorDao()
                .getFileTraffic(workspaceName);
        wsDetailWithStatistics.setFileDownloadTraffic(fileTraffic.getDownloadTraffics());
        wsDetailWithStatistics.setFileUploadTraffic(fileTraffic.getUploadTraffics());

        String prefreSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        try {
            synchronized (session) {
                session.resetServiceEndpoint(prefreSite);
                wsDetailWithStatistics.setFileCount(session.getFileDao().countFile(workspaceName));
                wsDetailWithStatistics
                        .setBatchCount(session.getBatchDao().countBatch(workspaceName));
                wsDetailWithStatistics
                        .setDirectoryCount(session.getDirDao().countDir(workspaceName));
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }

        return wsDetailWithStatistics;
    }

    @Override
    public List<OmWorkspaceBasicInfo> getWorkspaceList(ScmOmSession session, long skip, int limit)
            throws ScmInternalException, ScmOmServerException {
        String prefresite = siteChooser.chooseFromAllSite();
        try {
            synchronized (session) {
                session.resetServiceEndpoint(prefresite);
                return session.getWorkspaceDao().getWorkspaceList(skip, limit);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmWorkspaceDetail getWorksapceDetail(ScmOmSession session, String workspaceName)
            throws ScmInternalException, ScmOmServerException {
        OmCacheWrapper<OmWorkspaceDetail> cache = workspacesCache.get(workspaceName);
        if (cache != null && !cache.isExpire(cacheTTL)) {
            return cache.getCache();
        }

        String rootSite = siteChooser.getRootSite();
        OmWorkspaceDetail ret = null;
        try {
            synchronized (session) {
                session.resetServiceEndpoint(rootSite);
                ret = session.getWorkspaceDao().getWorkspaceDetail(workspaceName);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
        workspacesCache.put(workspaceName, new OmCacheWrapper<OmWorkspaceDetail>(ret));
        return ret;
    }
}
