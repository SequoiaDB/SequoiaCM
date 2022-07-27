package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmBucketDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmBucketDaoFactory;
import com.sequoiacm.om.omserver.module.OmBucketDetail;
import com.sequoiacm.om.omserver.module.OmCacheWrapper;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import com.sequoiacm.om.omserver.service.ScmBucketService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
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

    private Map<String, OmCacheWrapper<Set<String>>> userAccessibleBucketCache = new ConcurrentHashMap<>();
    private long cacheTTL;

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmBucketDaoFactory scmBucketDaoFactory;

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
}
