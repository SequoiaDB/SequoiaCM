package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmBucketDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmBucketDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmUserDaoFactory;
import com.sequoiacm.om.omserver.factory.ScmWorkSpaceDaoFactory;
import com.sequoiacm.om.omserver.module.OmBatchOpResult;
import com.sequoiacm.om.omserver.module.OmBucketCreateInfo;
import com.sequoiacm.om.omserver.module.OmBucketDetail;
import com.sequoiacm.om.omserver.module.OmBucketUpdateInfo;
import com.sequoiacm.om.omserver.module.OmCacheWrapper;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.service.ScmBucketService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScmBucketServiceImpl implements ScmBucketService {

    private static final Logger logger = LoggerFactory.getLogger(ScmBucketServiceImpl.class);

    private Map<String, OmCacheWrapper<Set<String>>> userAccessibleBucketCache = new ConcurrentHashMap<>();
    private long cacheTTL;

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmBucketDaoFactory scmBucketDaoFactory;

    @Autowired
    private ScmUserDaoFactory userDaoFactory;

    @Autowired
    private ScmWorkSpaceDaoFactory workSpaceDaoFactory;

    @Autowired
    public ScmBucketServiceImpl(ScmOmServerConfig config) {
        this.cacheTTL = config.getCacheRefreshInterval() * 1000;
    }

    @Override
    public List<String> getUserAccessibleBuckets(ScmOmSession session)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.getRootSite();
        try {
            session.resetServiceEndpoint(preferSite);
            Set<String> userAccessibleBuckets;
            OmCacheWrapper<Set<String>> cacheWrapper = userAccessibleBucketCache
                    .get(session.getUser());
            if (cacheWrapper != null && !cacheWrapper.isExpire(cacheTTL)) {
                userAccessibleBuckets = cacheWrapper.getCache();
            }
            else {
                userAccessibleBuckets = scmBucketDaoFactory.createScmBucketDao(session)
                        .getUserAccessibleBuckets(session.getUser());
                userAccessibleBucketCache.put(session.getUser(),
                        new OmCacheWrapper<>(userAccessibleBuckets));
            }
            return new ArrayList<>(userAccessibleBuckets);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public OmBucketDetail getBucketDetail(ScmOmSession session, String bucketName)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return bucketDao.getBucketDetail(bucketName);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public long countFile(ScmOmSession session, String bucketName, BSONObject condition)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return bucketDao.countFile(bucketName, condition);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmFileBasic> listFiles(ScmOmSession session, String bucketName, BSONObject filter,
            BSONObject orderBy, long skip, int limit)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            return bucketDao.listFile(bucketName, filter, orderBy, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void createFile(ScmOmSession session, String bucketName, String siteName,
            OmFileInfo fileInfo, BSONObject uploadConf, InputStream is)
            throws ScmInternalException {
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            session.resetServiceEndpoint(siteName);
            bucketDao.createFile(bucketName, fileInfo, uploadConf, is);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmBucketDetail> listBucket(ScmOmSession session, BSONObject filter,
            BSONObject orderBy, long skip, int limit, Boolean isStrictMode)
            throws ScmInternalException, ScmOmServerException {
        String preferSite = siteChooser.getRootSite();
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            filter = processFilter(session, filter, isStrictMode);
            session.resetServiceEndpoint(preferSite);
            return bucketDao.listBucket(filter, orderBy, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public long countBucket(ScmOmSession session, BSONObject filter, Boolean isStrictMode) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            filter = processFilter(session, filter, isStrictMode);
            session.resetServiceEndpoint(preferSite);
            return bucketDao.countBucket(filter);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void updateBucket(ScmOmSession session, String bucketName, OmBucketUpdateInfo bucketUpdateInfo)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.getRootSite();
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            String versionStatus = bucketUpdateInfo.getVersionStatus();
            if (ScmBucketVersionStatus.Enabled.name().equals(versionStatus)) {
                bucketDao.enableVersionControl(bucketName);
            }
            else if (ScmBucketVersionStatus.Suspended.name().equals(versionStatus)) {
                bucketDao.suspendVersionControl(bucketName);
            }
            else {
                throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                        "illegal versionStatus:" + versionStatus);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmBatchOpResult> createBucket(ScmOmSession session,
            OmBucketCreateInfo bucketCreateInfo) throws ScmOmServerException, ScmInternalException {
        checkBucketCreateInfo(bucketCreateInfo);
        List<OmBatchOpResult> res = new ArrayList<>(bucketCreateInfo.getBucketNames().size());
        String preferSite = siteChooser.getRootSite();
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            ScmWorkspace workspace = workSpaceDaoFactory.createWorkspaceDao(session)
                    .getWorkspaceDetail(bucketCreateInfo.getWorkspace());
            for (String bucketName : bucketCreateInfo.getBucketNames()) {
                try {
                    ScmBucket bucket = bucketDao.createBucket(workspace, bucketName);
                    String message = null;
                    if (ScmBucketVersionStatus.Enabled.name()
                            .equals(bucketCreateInfo.getVersionStatus())) {
                        try {
                            bucket.enableVersionControl();
                        }
                        catch (Exception e) {
                            message = "create bucket success but update version status failed:"
                                    + e.getMessage();
                            logger.error(message + ", bucket={}, versionStatus={}", bucketName,
                                    bucketCreateInfo.getVersionStatus(), e);
                        }
                    }
                    res.add(new OmBatchOpResult(bucketName, true, message));
                }
                catch (Exception e) {
                    if (e instanceof ScmInternalException) {
                        siteChooser.onException((ScmInternalException) e);
                    }
                    logger.error("failed to create bucket: bucketName={}", bucketName, e);
                    res.add(new OmBatchOpResult(bucketName, false, e.getMessage()));
                }
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
        userAccessibleBucketCache.clear();
        return res;
    }

    private void checkBucketCreateInfo(OmBucketCreateInfo bucketCreateInfo)
            throws ScmOmServerException {
        List<String> bucketNames = bucketCreateInfo.getBucketNames();
        if (bucketNames == null || bucketNames.isEmpty()) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "bucketNames is empty");
        }
        String versionStatus = bucketCreateInfo.getVersionStatus();
        if (!ScmBucketVersionStatus.Enabled.name().equals(versionStatus)
                && !ScmBucketVersionStatus.Disabled.name().equals(versionStatus)) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "illegal versionStatus:" + versionStatus);
        }
    }

    @Override
    public List<OmBatchOpResult> deleteBuckets(ScmOmSession session, List<String> bucketNames)
            throws ScmOmServerException, ScmInternalException {
        if (bucketNames == null || bucketNames.isEmpty()) {
            throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                    "bucketNames is empty");
        }
        List<OmBatchOpResult> res = new ArrayList<>(bucketNames.size());
        String preferSite = siteChooser.getRootSite();
        session.resetServiceEndpoint(preferSite);
        ScmBucketDao bucketDao = scmBucketDaoFactory.createScmBucketDao(session);
        for (String bucketName : bucketNames) {
            try {
                bucketDao.deleteBucket(bucketName);
                res.add(new OmBatchOpResult(bucketName, true));
            }
            catch (Exception e) {
                if (e instanceof ScmInternalException) {
                    siteChooser.onException((ScmInternalException) e);
                }
                logger.error("failed to delete bucket: bucketName={}", bucketName, e);
                res.add(new OmBatchOpResult(bucketName, false, e.getMessage()));
            }
        }
        userAccessibleBucketCache.clear();
        return res;
    }

    private BSONObject processFilter(ScmOmSession session, BSONObject filter, Boolean isStrictMode)
            throws ScmOmServerException, ScmInternalException {
        try {
            if (!isStrictMode) {
                return filter;
            }
            String username = session.getUser();
            OmUserInfo user = userDaoFactory.createUserDao(session).getUser(username);
            if (user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
                return filter;
            }
            filter = ScmQueryBuilder.start(FieldName.Bucket.NAME)
                    .in(getUserAccessibleBuckets(session)).and(filter).get();
            return filter;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }

    }
}
