package com.sequoiacm.om.omserver.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.factory.*;
import com.sequoiacm.om.omserver.module.*;
import com.sequoiacm.om.omserver.service.ScmSiteService;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmWorkspaceServiceImpl implements ScmWorkspaceService {

    private Map<String, OmCacheWrapper<OmWorkspaceDetail>> workspacesCache = new ConcurrentHashMap<>();
    private Map<String, OmCacheWrapper<Set<String>>> userAccessibleWsCache = new ConcurrentHashMap<>();
    private long cacheTTL;

    @Autowired
    private ScmSiteChooser siteChooser;
    @Autowired
    private ScmSiteService siteService;
    @Autowired
    private ScmWorkSpaceDaoFactory workSpaceDaoFactory;
    @Autowired
    private ScmMonitorDaoFactory monitorDaoFactory;
    @Autowired
    private ScmDirDaoFactory dirDaoFactory;
    @Autowired
    private ScmBatchDaoFactory batchDaoFactory;
    @Autowired
    private ScmFileDaoFactory fileDaoFactory;
    @Autowired
    private ScmUserDaoFactory userDaoFactory;

    @Autowired
    public ScmWorkspaceServiceImpl(ScmOmServerConfig config) {
        this.cacheTTL = config.getCacheRefreshInterval() * 1000;
    }

    @Override
    public OmWorkspaceInfoWithStatistics getWorksapceDetailWithStatistics(ScmOmSession session,
            String workspaceName) throws ScmInternalException, ScmOmServerException {
        OmWorkspaceDetail wsDetail = getWorkspaceDetail(session, workspaceName);
        ScmMonitorDao monitorDao = monitorDaoFactory.createMonitorDao(session);

        OmWorkspaceInfoWithStatistics wsDetailWithStatistics = new OmWorkspaceInfoWithStatistics();
        wsDetailWithStatistics.setCreateTime(wsDetail.getCreateTime());
        wsDetailWithStatistics.setCreateUser(wsDetail.getCreateUser());
        wsDetailWithStatistics.setDataLocations(wsDetail.getDataLocations());
        wsDetailWithStatistics.setDescription(wsDetail.getDescription());
        wsDetailWithStatistics.setMetaOption(wsDetail.getMetaOption());
        wsDetailWithStatistics.setName(wsDetail.getName());
        wsDetailWithStatistics.setUpdateTime(wsDetail.getUpdateTime());
        wsDetailWithStatistics.setUpdateUser(wsDetail.getUpdateUser());

        OmFileDeltaStatistics fileDelta = monitorDao.getFileDelta(workspaceName);
        wsDetailWithStatistics.setFileSizeDelta(fileDelta.getSizeDelta());
        wsDetailWithStatistics.setFileCountDelta(fileDelta.getCountDelta());

        OmFileTrafficStatistics fileTraffic = monitorDao.getFileTraffic(workspaceName);
        wsDetailWithStatistics.setFileDownloadTraffic(fileTraffic.getDownloadTraffics());
        wsDetailWithStatistics.setFileUploadTraffic(fileTraffic.getUploadTraffics());

        String prefreSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        try {
            session.resetServiceEndpoint(prefreSite);
            wsDetailWithStatistics
                    .setFileCount(fileDaoFactory.createFileDao(session).countFile(workspaceName));
            wsDetailWithStatistics.setBatchCount(
                    batchDaoFactory.createScmBatchDao(session).countBatch(workspaceName));
            wsDetailWithStatistics.setDirectoryCount(
                    dirDaoFactory.createScmDirDao(session).countDir(workspaceName));
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }

        return wsDetailWithStatistics;
    }

    @Override
    public List<OmWorkspaceBasicInfo> getUserRelatedWsList(ScmOmSession session,
            BSONObject condition, BSONObject orderby, long skip, int limit)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            condition = filterWsByUser(session, condition);
            List<ScmWorkspaceInfo> workspaceList = workspaceDao.getWorkspaceList(condition, orderby,
                    skip, limit);
            return transformToOmWsList(session, workspaceList);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    private List<OmWorkspaceBasicInfo> transformToOmWsList(ScmOmSession session,
            List<ScmWorkspaceInfo> workspaceList)
            throws ScmInternalException, ScmOmServerException {
        List<OmWorkspaceBasicInfo> omWorkspaceBasicList = new ArrayList<>();
        if (workspaceList != null && workspaceList.size() > 0) {
            for (ScmWorkspaceInfo scmWorkspaceInfo : workspaceList) {
                omWorkspaceBasicList.add(transformToOmBasicWsInfo(session, scmWorkspaceInfo));
            }
        }
        return omWorkspaceBasicList;
    }

    private OmWorkspaceBasicInfo transformToOmBasicWsInfo(ScmOmSession session,
            ScmWorkspaceInfo wsInfo) throws ScmOmServerException, ScmInternalException {
        OmWorkspaceBasicInfo basicInfo = new OmWorkspaceBasicInfo();
        basicInfo.setName(wsInfo.getName());
        basicInfo.setDescription(wsInfo.getDesc());
        basicInfo.setCreateUser(wsInfo.getCreateUser());
        basicInfo.setCreateTime(wsInfo.getCreateTime());
        basicInfo.setSiteList(transformToSiteList(session, wsInfo.getDataLocation()));
        return basicInfo;
    }

    private List<String> transformToSiteList(ScmOmSession session, List<BSONObject> dataLocation)
            throws ScmOmServerException, ScmInternalException {
        List<String> siteList = new ArrayList<>();
        if (dataLocation == null) {
            return siteList;
        }
        for (BSONObject bsonObject : dataLocation) {
            int siteId = Integer.parseInt(String.valueOf(bsonObject.get(RestParamDefine.SITE_ID)));
            siteList.add(siteService.getSiteById(session, siteId));
        }
        return siteList;
    }

    private BSONObject filterWsByUser(ScmOmSession session, BSONObject condition)
            throws ScmInternalException {
        try {
            String username = session.getUser();
            OmUserInfo user = userDaoFactory.createUserDao(session).getUser(username);
            if (user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
                return condition;
            }
            Set<String> userAccessibleWs = null;
            OmCacheWrapper<Set<String>> cacheWrapper = userAccessibleWsCache.get(session.getUser());
            if (cacheWrapper != null && !cacheWrapper.isExpire(cacheTTL)) {
                userAccessibleWs = cacheWrapper.getCache();
            }
            else {
                userAccessibleWs = workSpaceDaoFactory.createWorkspaceDao(session)
                        .getUserAccessibleWorkspaces(username);
                userAccessibleWsCache.put(session.getUser(),
                        new OmCacheWrapper<>(userAccessibleWs));
            }
            condition = ScmQueryBuilder.start(FieldName.FIELD_CLWORKSPACE_NAME).in(userAccessibleWs)
                    .and(condition).get();
            return condition;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
    }

    @Override
    public long getWorkspaceCount(ScmOmSession session, BSONObject condition)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            condition = filterWsByUser(session, condition);
            return workspaceDao.getWorkspaceCount(condition);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmWorkspaceDetail getWorkspaceDetail(ScmOmSession session, String workspaceName)
            throws ScmInternalException, ScmOmServerException {
        OmCacheWrapper<OmWorkspaceDetail> cache = workspacesCache.get(workspaceName);
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        if (cache != null && !cache.isExpire(cacheTTL)) {
            return cache.getCache();
        }

        String rootSite = siteChooser.getRootSite();
        OmWorkspaceDetail ret = null;
        try {
            session.resetServiceEndpoint(rootSite);
            ScmWorkspace workspaceDetail = workspaceDao.getWorkspaceDetail(workspaceName);
            ret = transformToOmWsDetail(session, workspaceDetail);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
        workspacesCache.put(workspaceName, new OmCacheWrapper<OmWorkspaceDetail>(ret));
        return ret;
    }

    private OmWorkspaceDetail transformToOmWsDetail(ScmOmSession session, ScmWorkspace ws)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail ret = new OmWorkspaceDetail();
        ret.setCreateTime(ws.getCreateTime());
        ret.setCreateUser(ws.getCreateUser());
        ret.setDescription(ws.getDescription());
        ret.setName(ws.getName());
        ret.setUpdateTime(ws.getUpdateTime());
        ret.setUpdateUser(ws.getUpdateUser());

        List<OmWorkspaceDataLocation> retDatalocations = new ArrayList<>();
        for (ScmDataLocation site : ws.getDataLocations()) {
            OmWorkspaceDataLocation retDataLocation = new OmWorkspaceDataLocation();
            retDataLocation.setSiteName(site.getSiteName());
            retDataLocation.setSiteType(site.getType().toString());
            BSONObject option = generateOption(site);
            retDataLocation.setOptions(option);
            retDatalocations.add(retDataLocation);
        }
        ret.setDataLocations(retDatalocations);
        BSONObject option = generateMetaOption(session, ws.getMetaLocation());
        ret.setMetaOption(option);
        return ret;
    }

    private BSONObject generateMetaOption(ScmOmSession session, ScmMetaLocation metaLocation)
            throws ScmOmServerException, ScmInternalException {
        BSONObject metaBson = metaLocation.getBSONObject();
        List<OmSiteInfo> siteList = siteService.getSiteList(session);
        for (OmSiteInfo omSiteInfo : siteList) {
            if (omSiteInfo.getName().equals(metaLocation.getSiteName())) {
                metaBson.put(RestParamDefine.DATA_URL, omSiteInfo.getDataUrl());
                break;
            }
        }
        return metaBson;
    }

    private BSONObject generateOption(ScmLocation site) {
        BSONObject siteBson = site.getBSONObject();
        siteBson.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        return siteBson;
    }
}
