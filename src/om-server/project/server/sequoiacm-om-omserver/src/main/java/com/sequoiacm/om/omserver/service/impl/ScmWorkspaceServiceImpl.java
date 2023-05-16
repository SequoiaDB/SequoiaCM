package com.sequoiacm.om.omserver.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.om.omserver.module.OmDeltaStatistics;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephSwiftDataLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHdfsDataLocation;
import com.sequoiacm.client.element.bizconf.ScmLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSftpDataLocation;
import com.sequoiacm.client.element.bizconf.ScmTagLibMetaOption;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmBatchDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmDirDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmFileDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmMonitorDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmUserDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmWorkSpaceDaoFactory;
import com.sequoiacm.om.omserver.module.OmBatchOpResult;
import com.sequoiacm.om.omserver.module.OmCacheWrapper;
import com.sequoiacm.om.omserver.module.OmFileTrafficStatistics;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceCreateInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDataLocation;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;
import com.sequoiacm.om.omserver.service.ScmSiteService;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmWorkspaceServiceImpl implements ScmWorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(ScmWorkspaceServiceImpl.class);

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

        OmWorkspaceInfoWithStatistics wsDetailWithStatistics = new OmWorkspaceInfoWithStatistics();
        wsDetailWithStatistics.setCreateTime(wsDetail.getCreateTime());
        wsDetailWithStatistics.setCreateUser(wsDetail.getCreateUser());
        wsDetailWithStatistics.setDataLocations(wsDetail.getDataLocations());
        wsDetailWithStatistics.setDescription(wsDetail.getDescription());
        wsDetailWithStatistics.setMetaOption(wsDetail.getMetaOption());
        wsDetailWithStatistics.setName(wsDetail.getName());
        wsDetailWithStatistics.setUpdateTime(wsDetail.getUpdateTime());
        wsDetailWithStatistics.setUpdateUser(wsDetail.getUpdateUser());
        wsDetailWithStatistics.setSiteCacheStrategy(wsDetail.getSiteCacheStrategy());
        wsDetailWithStatistics.setTagRetrievalStatus(wsDetail.getTagRetrievalStatus());
        wsDetailWithStatistics.setTagLibDomain(wsDetail.getTagLibDomain());
        String prefreSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        try {
            session.resetServiceEndpoint(prefreSite);
            wsDetailWithStatistics.setFileCount(fileDaoFactory.createFileDao(session).countFile(
                    workspaceName, ScopeType.SCOPE_CURRENT.getScope(), new BasicBSONObject()));
            wsDetailWithStatistics.setBatchCount(
                    batchDaoFactory.createScmBatchDao(session).countBatch(workspaceName));
            wsDetailWithStatistics.setDirectoryCount(dirDaoFactory.createScmDirDao(session)
                    .countDir(workspaceName, new BasicBSONObject()));
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }

        return wsDetailWithStatistics;
    }

    @Override
    public List<OmWorkspaceBasicInfo> getUserRelatedWsList(ScmOmSession session,
            BSONObject condition, BSONObject orderby, long skip, int limit, Boolean isStrictMode)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            condition = filterWsByUser(session, condition, isStrictMode);
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

    private BSONObject filterWsByUser(ScmOmSession session, BSONObject condition,
            Boolean isStrictMode) throws ScmInternalException {
        try {
            String username = session.getUser();
            OmUserInfo user = userDaoFactory.createUserDao(session).getUser(username);
            if (!isStrictMode && user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
                return condition;
            }
            Set<String> userAccessibleWs = null;
            OmCacheWrapper<Set<String>> cacheWrapper = userAccessibleWsCache.get(session.getUser());
            if (cacheWrapper != null && !cacheWrapper.isExpire(cacheTTL)) {
                userAccessibleWs = cacheWrapper.getCache();
            }
            else {
                userAccessibleWs = workSpaceDaoFactory.createWorkspaceDao(session)
                        .getUserAccessibleWorkspaces(username, null);
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
    public List<OmWorkspaceBasicInfo> getCreatePrivilegeWsList(ScmOmSession session)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            Set<String> wsList = workspaceDao.getUserAccessibleWorkspaces(session.getUser(),
                    ScmPrivilegeType.CREATE);
            BSONObject condition = ScmQueryBuilder.start(FieldName.FIELD_CLWORKSPACE_NAME)
                    .in(wsList).get();
            List<ScmWorkspaceInfo> workspaceList = workspaceDao.getWorkspaceList(condition, null, 0,
                    -1);
            return transformToOmWsList(session, workspaceList);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
        catch (ScmException e) {
            ScmInternalException scmInternalException = new ScmInternalException(e.getError(),
                    e.getMessage(), e);
            siteChooser.onException(scmInternalException);
            throw scmInternalException;
        }
    }

    @Override
    public List<OmBatchOpResult> createWorkspaces(ScmOmSession session,
            OmWorkspaceCreateInfo workspacesInfo)
            throws ScmOmServerException, ScmInternalException {
        List<String> wsNameList = workspacesInfo.getWsNameList();
        if (wsNameList == null || wsNameList.size() == 0) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "workspaceNames is empty");
        }
        String preferSite = siteChooser.getRootSite();
        List<OmBatchOpResult> resultList = new ArrayList<>(workspacesInfo.getWsNameList().size());
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        try {
            session.resetServiceEndpoint(preferSite);
            Map<String, OmSiteInfo> sitesMap = siteService.getSitesAsMap(session);
            setSiteCacheStrategy(conf, workspacesInfo.getCacheStrategy());
            if (!StringUtils.isEmpty(workspacesInfo.getPreferred())) {
                conf.setPreferred(workspacesInfo.getPreferred());
            }
            if (workspacesInfo.getDescription() != null) {
                conf.setDescription(workspacesInfo.getDescription());
            }
            conf.setEnableDirectory(workspacesInfo.isDirectoryEnabled());
            conf.setEnableTagRetrieval(workspacesInfo.isTagRetrievalEnabled());
            conf.setTagLibMetaOption(new ScmTagLibMetaOption(workspacesInfo.getTagLibDomain()));
            conf.setMetaLocation(new ScmSdbMetaLocation(workspacesInfo.getMetaLocation()));
            for (BSONObject location : workspacesInfo.getDataLocations()) {
                conf.addDataLocation(createDataLocation(location, sitesMap));
            }
        }
        catch (ScmInvalidArgumentException e) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT, e.getMessage(), e);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }

        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        for (String wsName : wsNameList) {
            try {
                conf.setName(wsName);
                workspaceDao.createWorkspace(conf);
                resultList.add(new OmBatchOpResult(wsName, true, null));
            }
            catch (Exception e) {
                if (e instanceof ScmInternalException) {
                    siteChooser.onException((ScmInternalException) e);
                }
                logger.error("failed to create workspace: wsName={}", wsName, e);
                resultList.add(new OmBatchOpResult(wsName, false, e.getMessage()));
            }
        }
        userAccessibleWsCache.clear();
        return resultList;
    }

    private void setSiteCacheStrategy(ScmWorkspaceConf conf, String cacheStrategy)
            throws ScmOmServerException {
        if (cacheStrategy == null || cacheStrategy.isEmpty()) {
            return;
        }
        ScmSiteCacheStrategy strategy = ScmSiteCacheStrategy.getStrategy(cacheStrategy);
        if (strategy == ScmSiteCacheStrategy.UNKNOWN) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "site cache strategy is unknown:" + cacheStrategy);
        }
        else {
            try {
                conf.setSiteCacheStrategy(strategy);
            }
            catch (ScmInvalidArgumentException e) {
                throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT, e.getMessage(),
                        e);
            }
        }

    }

    private ScmDataLocation createDataLocation(BSONObject dataLocationBSON,
            Map<String, OmSiteInfo> sitesMap) throws ScmOmServerException {
        String siteName = (String) dataLocationBSON
                .get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        OmSiteInfo site = sitesMap.get(siteName);
        if (site == null) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "site is not exits: siteName=" + siteName);
        }
        ScmType.DatasourceType dataType = site.getDatasourceTypeEnum();
        try {
            switch (dataType) {
                case SEQUOIADB:
                    return new ScmSdbDataLocation(dataLocationBSON);
                case HDFS:
                    return new ScmHdfsDataLocation(dataLocationBSON);
                case HBASE:
                    return new ScmHbaseDataLocation(dataLocationBSON);
                case CEPH_S3:
                    return new ScmCephS3DataLocation(dataLocationBSON);
                case CEPH_SWIFT:
                    return new ScmCephSwiftDataLocation(dataLocationBSON);
                case SFTP:
                    return new ScmSftpDataLocation(dataLocationBSON);
                default:
                    throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                            "unknown siteType:siteName=" + siteName + ",type=" + dataType);
            }
        }
        catch (ScmInvalidArgumentException e) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT, e.getMessage(), e);
        }
    }

    @Override
    public List<OmBatchOpResult> deleteWorkspaces(ScmOmSession session, List<String> wsNames,
            boolean isForce) throws ScmOmServerException, ScmInternalException {
        if (wsNames == null || wsNames.isEmpty()) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "workspaceNames is empty");
        }
        String preferSite = siteChooser.getRootSite();
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        session.resetServiceEndpoint(preferSite);
        List<OmBatchOpResult> res = new ArrayList<>(wsNames.size());
        for (String wsName : wsNames) {
            try {
                workspaceDao.deleteWorkspace(wsName, isForce);
                res.add(new OmBatchOpResult(wsName, true));
                workspacesCache.remove(wsName);
            }
            catch (Exception e) {
                if (e instanceof ScmInternalException) {
                    siteChooser.onException((ScmInternalException) e);
                }
                logger.warn("failed to delete workspace: wsName={}", wsName, e);
                res.add(new OmBatchOpResult(wsName, false, e.getMessage()));
            }
        }
        userAccessibleWsCache.clear();
        return res;
    }

    @Override
    public long getWorkspaceCount(ScmOmSession session, BSONObject condition, Boolean strictMode)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            condition = filterWsByUser(session, condition, strictMode);
            return workspaceDao.getWorkspaceCount(condition);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void removeWorkspaceCache(String user) {
        userAccessibleWsCache.remove(user);
    }

    @Override
    public void updateWorkspace(ScmOmSession session, String workspaceName, OmWorkspaceInfo wsInfo)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            workspaceDao.updateWorkspace(session, workspaceName, wsInfo);
            workspacesCache.remove(workspaceName);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmFileTrafficStatistics getWorkspaceTraffic(ScmOmSession session, String workspaceName,
            Long beginTime, Long endTime) throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.chooseFromAllSite();
        try {
            session.resetServiceEndpoint(preferSite);
            ScmMonitorDao monitorDao = monitorDaoFactory.createMonitorDao(session);
            return monitorDao.getFileTraffic(workspaceName, beginTime, endTime);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }

    }

    @Override
    public OmDeltaStatistics getWorkspaceFileDelta(ScmOmSession session, String workspaceName,
            Long beginTime, Long endTime) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        try {
            session.resetServiceEndpoint(preferSite);
            ScmMonitorDao monitorDao = monitorDaoFactory.createMonitorDao(session);
            return monitorDao.getFileDelta(workspaceName, beginTime, endTime);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmWorkspaceDetail getWorkspaceDetail(ScmOmSession session, String wsName)
            throws ScmInternalException, ScmOmServerException {
        return getWorkspaceDetail(session, wsName, false);
    }

    @Override
    public OmWorkspaceDetail getWorkspaceDetail(ScmOmSession session, String workspaceName,
            boolean forceFetch) throws ScmInternalException, ScmOmServerException {
        if (!forceFetch) {
            OmCacheWrapper<OmWorkspaceDetail> cache = workspacesCache.get(workspaceName);
            if (cache != null && !cache.isExpire(cacheTTL)) {
                return cache.getCache();
            }
        }
        ScmWorkspaceDao workspaceDao = workSpaceDaoFactory.createWorkspaceDao(session);
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
        ret.setEnableDirectory(ws.isEnableDirectory());
        ScmTagLibMetaOption tagLibMetaOption;
        try {
            tagLibMetaOption = ws.getTagLibMetaOption();
            if (tagLibMetaOption != null) {
                ret.setTagLibDomain(tagLibMetaOption.getTagLibDomain());
            }
            ret.setTagRetrievalStatus(ws.getTagRetrievalStatus().getValue());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
        ret.setUpdateTime(ws.getUpdateTime());
        ret.setUpdateUser(ws.getUpdateUser());
        ret.setSiteCacheStrategy(ws.getSiteCacheStrategy().name());

        List<OmWorkspaceDataLocation> retDataLocations = new ArrayList<>();
        for (ScmDataLocation site : ws.getDataLocations()) {
            OmWorkspaceDataLocation retDataLocation = new OmWorkspaceDataLocation();
            retDataLocation.setSiteName(site.getSiteName());
            retDataLocation.setSiteType(site.getType().toString());
            BSONObject option = generateOption(site);
            retDataLocation.setOptions(option);
            retDataLocations.add(retDataLocation);
        }
        ret.setDataLocations(retDataLocations);
        BSONObject option = generateMetaOption(session, ws.getMetaLocation());
        ret.setMetaOption(option);
        return ret;
    }

    private BSONObject generateMetaOption(ScmOmSession session, ScmMetaLocation metaLocation)
            throws ScmOmServerException, ScmInternalException {
        BSONObject metaBson = metaLocation.getBSONObject();
        List<OmSiteInfo> siteList = siteService.getSiteList(session, null, 0, -1);
        for (OmSiteInfo omSiteInfo : siteList) {
            if (omSiteInfo.getName().equals(metaLocation.getSiteName())) {
                metaBson.put(RestParamDefine.DATA_URL, omSiteInfo.getDatasourceUrl());
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
