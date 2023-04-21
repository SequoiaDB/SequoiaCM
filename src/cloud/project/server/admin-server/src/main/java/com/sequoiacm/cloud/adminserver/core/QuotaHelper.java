package com.sequoiacm.cloud.adminserver.core;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.dao.SiteDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketConfSubscriber;
import com.sequoiacm.infrastructure.config.client.core.workspace.WorkspaceConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceInstance;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmWorkspaceResource;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class QuotaHelper {

    @Autowired
    private ScmConfClient confClient;

    @Autowired
    private BucketConfSubscriber bucketConfSubscriber;

    @Autowired
    private WorkspaceConfSubscriber workspaceConfSubscriber;

    @Autowired
    private ScmServiceDiscoveryClient serviceDiscoveryClient;

    @Autowired
    private SiteDao siteDao;

    @Autowired
    private ScmPrivClient privClient;

    @Autowired
    private ScmServiceDiscoveryClient discoveryClient;

    public List<ScmServiceInstance> getS3AndContentServerInstance() {
        List<ScmServiceInstance> res = new ArrayList<>();
        List<ScmServiceInstance> instances = discoveryClient.getInstances();
        for (ScmServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            if (metadata != null) {
                boolean isContentServer = Boolean.parseBoolean(metadata.get("isContentServer"));
                boolean isS3Server = Boolean.parseBoolean(metadata.get("isS3Server"));
                if (isContentServer || isS3Server) {
                    res.add(instance);
                }
            }
        }
        return res;
    }

    public void checkTypeAndName(String type, String name) throws StatisticsException {
        if (StatisticsDefine.QuotaType.BUCKET.equals(type)) {
            try {
                BucketConfig bucket = bucketConfSubscriber.getBucket(name);
                if (bucket == null) {
                    throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                            "bucket is not exist:" + name);
                }
            }
            catch (ScmConfigException e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to get bucket, bucketName=" + name, e);
            }

        }
        else {
            throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                    "unsupported quota type:" + type);
        }
    }

    public void checkPriv(String type, String name, String username, ScmPrivilegeDefine priv,
            String opDesc) throws StatisticsException {
        if (StatisticsDefine.QuotaType.BUCKET.equals(type)) {
            BucketConfig bucket = null;
            try {
                bucket = bucketConfSubscriber.getBucket(name);
            }
            catch (ScmConfigException e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to get bucket, bucketName=" + name, e);
            }
            if (bucket == null) {
                throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                        "bucket is not exist:" + name);
            }
            IResourceBuilder wsResBuilder = privClient
                    .getResourceBuilder(ScmWorkspaceResource.RESOURCE_TYPE);
            IResource wsResource = wsResBuilder.fromStringFormat(bucket.getWorkspace());
            boolean isPermitted = privClient.check(username, wsResource, priv.getFlag());
            if (!isPermitted) {
                throw new StatisticsException(StatisticsError.UNAUTHORIZED_OPERATION,
                        opDesc + " failed, do not have priority:user=" + username + ",ws="
                                + bucket.getWorkspace() + ", needPriv=" + priv.getName());
            }
        }
        else {
            throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                    "unsupported quota type:" + type);
        }

    }

    public QuotaConfig getQuotaConfig(String type, String name) throws StatisticsException {
        try {
            return (QuotaConfig) confClient.getOneConf(ScmConfigNameDefine.QUOTA,
                    new QuotaFilter(type, name));
        }
        catch (ScmConfigException e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to get " + type + " quota:name=" + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<QuotaConfig> getQuotaConfigs() throws StatisticsException {
        try {
            List<Config> confList = confClient.getConf(ScmConfigNameDefine.QUOTA,
                    new QuotaFilter());
            return (List<QuotaConfig>) (Object) confList;
        }
        catch (ScmConfigException e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to get quota configs", e);
        }
    }

    public ScmServiceInstance choseStatisticsInstanceRandom(String type, String name)
            throws StatisticsException {
        if (StatisticsDefine.QuotaType.BUCKET.equals(type)) {
            try {
                List<Integer> siteIds = getSiteIdsAndShuffle(name);
                ScmServiceInstance instance = null;
                for (Integer siteId : siteIds) {
                    String siteName = getSiteName(siteId);
                    instance = serviceDiscoveryClient.choseInstance(siteName);
                    if (instance != null) {
                        break;
                    }
                }
                if (instance == null) {
                    throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "no available statistics instance:type=" + name + ",name=" + name);
                }
                return instance;

            }
            catch (StatisticsException e) {
                throw e;
            }
            catch (Exception e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to chose statistics instance:type=" + name + ",name=" + name, e);
            }
        }
        else {
            throw new IllegalArgumentException("invalid type:" + type);
        }
    }

    private List<Integer> getSiteIdsAndShuffle(String bucketName)
            throws ScmConfigException, StatisticsException {
        BucketConfig bucket = bucketConfSubscriber.getBucket(bucketName);
        if (bucket == null) {
            throw new IllegalArgumentException("bucket is not exist:" + bucketName);
        }
        WorkspaceConfig workspace = workspaceConfSubscriber.getWorkspace(bucket.getWorkspace());
        if (workspace == null) {
            throw new IllegalArgumentException("workspace is not exist:" + bucket.getWorkspace());
        }
        BasicBSONList dataLocations = workspace.getDataLocations();
        List<Integer> siteIds = new ArrayList<>();
        for (Object dataLocation : dataLocations) {
            BSONObject dataLocationObj = (BSONObject) dataLocation;
            siteIds.add(BsonUtils
                    .getNumberChecked(dataLocationObj, FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID)
                    .intValue());
        }

        int rootSiteId = siteDao.getRootSiteId();
        siteIds.remove(Integer.valueOf(rootSiteId));
        Collections.shuffle(siteIds);
        siteIds.add(0, rootSiteId);
        return siteIds;
    }


    private String getSiteName(Integer siteId) throws StatisticsException {
        MetaCursor cursor = null;
        try {
            cursor = siteDao.query(new BasicBSONObject(FieldName.FIELD_CLSITE_ID, siteId));
            if (cursor.hasNext()) {
                BSONObject site = cursor.getNext();
                return BsonUtils.getStringChecked(site, FieldName.FIELD_CLSITE_NAME);
            }
            throw new IllegalArgumentException("site is not exist:siteId=" + siteId);
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to get site name:siteId" + siteId, e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public BSONObject generateExtraInfo(String type, String name) throws StatisticsException {
        try {
            if (StatisticsDefine.QuotaType.BUCKET.equals(type)) {
                BucketConfig bucket = bucketConfSubscriber.getBucket(name);
                if (bucket == null) {
                    throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                            "bucket is not exist:" + name);
                }
                return new BasicBSONObject(FieldName.QuotaSync.EXTRA_INFO_WORKSPACE,
                        bucket.getWorkspace());
            }
            else {
                throw new IllegalArgumentException("invalid type:" + type);
            }
        }
        catch (StatisticsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to generate extra info for " + type + " quota:name=" + name, e);
        }
    }
}
