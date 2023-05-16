package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.FileTableCreator;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmJsonInputStreamCursor;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigUpdater;
import com.sequoiacm.infrastructure.config.core.msg.metadata.*;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ContenserverConfClient {
    private static ContenserverConfClient instance = new ContenserverConfClient();
    private ScmConfClient client;
    private BucketInfoManager bucketInfoMgr;

    public static ContenserverConfClient getInstance() {
        return instance;
    }

    public ContenserverConfClient() {

    }

    public ContenserverConfClient init(ScmConfClient client, BucketInfoManager bucketInfoManager) {
        this.client = client;
        this.bucketInfoMgr = bucketInfoManager;
        return this;
    }

    public void subscribeWithAsyncRetry(ScmConfSubscriber subscriber) throws ScmServerException {
        try {
            client.subscribeWithAsyncRetry(subscriber);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR, e.getMessage(), e);
        }
    }

    public WorkspaceConfig createWorkspace(WorkspaceConfig config) throws ScmServerException {
        try {
            return (WorkspaceConfig) client.createConf(ScmConfigNameDefine.WORKSPACE, config,
                    false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.WORKSPACE_EXIST) {
                throw new ScmServerException(ScmError.WORKSPACE_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public String getGlobalConf(String confName) throws ScmServerException {
        try {
            return client.getGlobalConfig(confName);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public List<Version> getConfigVersion(String configName, VersionFilter filter)
            throws ScmServerException {
        try {
            return client.getConfVersion(configName, filter);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public Config deleteWorkspace(WorkspaceFilter filter) throws ScmServerException {
        try {
            return client.deleteConf(ScmConfigNameDefine.WORKSPACE, filter, false);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public WorkspaceConfig updateWorkspaceConf(WorkspaceUpdator updator) throws ScmServerException {
        try {
            return (WorkspaceConfig) client.updateConfig(ScmConfigNameDefine.WORKSPACE, updator,
                    false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE) {
                throw new ScmServerException(ScmError.WORKSPACE_CACHE_EXPIRE, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataAttributeConfig createAttribute(MetaDataAttributeConfig attribute)
            throws ScmServerException {
        MetaDataConfig config = new MetaDataConfig(attribute);
        try {
            MetaDataConfig resp = (MetaDataConfig) client.createConf(ScmConfigNameDefine.META_DATA,
                    config, false);
            return resp.getAttributeConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.ATTRIBUTE_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataClassConfig createClass(MetaDataClassConfig classConfig)
            throws ScmServerException {
        MetaDataConfig config = new MetaDataConfig(classConfig);
        try {
            MetaDataConfig resp = (MetaDataConfig) client.createConf(ScmConfigNameDefine.META_DATA,
                    config, false);
            return resp.getClassConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLASS_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public void deleteClass(MetaDataClassConfigFilter classFilter) throws ScmServerException {
        MetaDataConfigFilter filter = new MetaDataConfigFilter(classFilter);
        try {
            client.deleteConf(ScmConfigNameDefine.META_DATA, filter, false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLASS_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public void deleteAttribute(MetaDataAttributeConfigFilter attributeFilter)
            throws ScmServerException {
        MetaDataConfigFilter filter = new MetaDataConfigFilter(attributeFilter);
        try {
            client.deleteConf(ScmConfigNameDefine.META_DATA, filter, false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_IN_CLASS) {
                throw new ScmServerException(ScmError.METADATA_ATTR_DELETE_FAILED, e.getMessage(),
                        e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataAttributeConfig updateAttribute(MetaDataAttributeConfigUpdator attributeUpdator)
            throws ScmServerException {
        MetaDataConfigUpdator updator = new MetaDataConfigUpdator(attributeUpdator);
        try {
            MetaDataConfig resp = (MetaDataConfig) client
                    .updateConfig(ScmConfigNameDefine.META_DATA, updator, false);
            return resp.getAttributeConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_EXIST, e.getMessage(), e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public MetaDataClassConfig updateClass(MetaDataClassConfigUpdator classUpdator)
            throws ScmServerException {
        MetaDataConfigUpdator updator = new MetaDataConfigUpdator(classUpdator);
        try {
            MetaDataConfig resp = (MetaDataConfig) client
                    .updateConfig(ScmConfigNameDefine.META_DATA, updator, false);
            return resp.getClassConfig();
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.CLASS_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.CLASS_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_EXIST, e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_ALREADY_IN_CLASS) {
                throw new ScmServerException(ScmError.METADATA_ATTR_ALREADY_IN_CLASS,
                        e.getMessage(), e);
            }
            if (e.getError() == ScmConfError.ATTRIBUTE_NOT_IN_CLASS) {
                throw new ScmServerException(ScmError.METADATA_ATTR_NOT_IN_CLASS, e.getMessage(),
                        e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public long countBucket(BSONObject matcher) throws ScmServerException {
        BucketConfigFilter filter = new BucketConfigFilter(matcher);
        try {
            return client.countConf(ScmConfigNameDefine.BUCKET, filter);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to count bucket: matcher=" + matcher, e);
        }
    }

    public ScmObjectCursor<ScmBucket> listBucket(BSONObject matcher, BSONObject orderby, long skip,
            long limit) throws ScmServerException {
        BucketConfigFilter filter = new BucketConfigFilter(matcher, orderby, limit, skip);
        try {
            final ScmJsonInputStreamCursor<Config> cursor = client
                    .listConf(ScmConfigNameDefine.BUCKET, filter);
            return new ScmObjectCursor<ScmBucket>() {
                @Override
                public boolean hasNext() throws IOException {
                    return cursor.hasNext();
                }

                @Override
                public ScmBucket getNext() throws IOException {
                    return convertBucketConf((BucketConfig) cursor.getNext());
                }

                @Override
                public void close() {
                    cursor.close();
                }
            };
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to list bucket: matcher=" + matcher + ", orderby=" + orderby + ", skip="
                            + skip + ", limit=" + limit,
                    e);
        }
    }

    public ScmBucket getBucket(String name) throws ScmServerException {
        BucketConfigFilter filter = new BucketConfigFilter(name);
        try {
            BucketConfig bucketConf = (BucketConfig) client.getOneConf(ScmConfigNameDefine.BUCKET,
                    filter);
            if (bucketConf == null) {
                return null;
            }
            return convertBucketConf(bucketConf);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to get bucket:" + name, e);
        }
    }

    public WorkspaceConfig getWorkspace(String name) throws ScmServerException {
        WorkspaceFilter filter = new WorkspaceFilter(name);
        try {
            WorkspaceConfig conf = (WorkspaceConfig) client
                    .getOneConf(ScmConfigNameDefine.WORKSPACE, filter);
            return conf;
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to get workspace:" + name, e);
        }
    }

    public List<WorkspaceConfig> getWorkspace(WorkspaceFilter filter) throws ScmServerException {
        try {
            List<WorkspaceConfig> conf = (List) client.getConf(ScmConfigNameDefine.WORKSPACE,
                    filter);
            return conf;
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to get workspace:" + filter, e);
        }
    }

    public ScmBucket getBucketById(long id) throws ScmServerException {
        BucketConfigFilter filter = new BucketConfigFilter(
                new BasicBSONObject(FieldName.Bucket.ID, id), null, -1, 0);
        try {
            BucketConfig bucketConf = (BucketConfig) client.getOneConf(ScmConfigNameDefine.BUCKET,
                    filter);
            if (bucketConf == null) {
                return null;
            }
            return convertBucketConf(bucketConf);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to get bucket id:" + id, e);
        }
    }

    public ScmBucket createBucket(String user, String ws, String bucketName)
            throws ScmServerException {
        try {
            // 在本地先创建桶相关的集合，再发送给配置服务做配置变更。 但旧版本是在配置服务创建桶相关的集合，
            // 因此新版本内容对接旧版本配置服务时会出现创建两次集合的问题(表名根据递增id生成，因此内容服务创建的集合会残留)
            BucketConfig bucketConfig = FileTableCreator.createBucketTable(
                    (SdbMetaSource) ScmContentModule.getInstance().getMetaService().getMetaSource(),
                    user, ws, bucketName);
            BucketConfig ret = (BucketConfig) client.createConf(ScmConfigNameDefine.BUCKET,
                    bucketConfig, false);
            return convertBucketConf(ret);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.BUCKET_EXIST) {
                throw new ScmServerException(ScmError.BUCKET_EXISTS,
                        "bucket is exists: ws=" + ws + ", bucketName=" + bucketName, e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to create bucket:ws=" + ws + ", bucket=" + bucketName, e);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to create bucket:ws=" + ws + ", bucket=" + bucketName, e);
        }
    }

    public ScmBucket updateBucketVersionStatus(String user, String bucketName,
            ScmBucketVersionStatus status) throws ScmServerException {
        BucketConfigUpdater bucketConfigUpdater = new BucketConfigUpdater(bucketName);
        bucketConfigUpdater.setVersionStatus(status.name());
        bucketConfigUpdater.setUpdateUser(user);
        try {
            BucketConfig ret = (BucketConfig) client.updateConfig(ScmConfigNameDefine.BUCKET,
                    bucketConfigUpdater, false);
            return convertBucketConf(ret);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.BUCKET_NOT_EXIST) {
                throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                        "bucket not exists: bucketName=" + bucketName, e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to update bucket: bucket=" + bucketName, e);
        }
    }

    public void deleteBucket(String name) throws ScmServerException {
        try {
            client.deleteConf(ScmConfigNameDefine.BUCKET, new BucketConfigFilter(name), false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.BUCKET_NOT_EXIST) {
                throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS, "bucket not exist:" + name,
                        e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to delete bucket:" + name, e);
        }
    }

    public void updateBucketTag(String username, String bucketName, Map<String, String> customTag)
            throws ScmServerException {
        BucketConfigUpdater bucketConfigUpdater = new BucketConfigUpdater(bucketName);
        bucketConfigUpdater.setUpdateUser(username);
        bucketConfigUpdater.setCustomTag(customTag);
        try {
            client.updateConfig(ScmConfigNameDefine.BUCKET, bucketConfigUpdater, false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.BUCKET_NOT_EXIST) {
                throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                        "bucket not exists: bucketName=" + bucketName, e);
            }
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to update bucket: bucket=" + bucketName, e);
        }
    }

    private ScmBucket convertBucketConf(BucketConfig ret) {
        ScmBucketVersionStatus versionStatus = ScmBucketVersionStatus.parse(ret.getVersionStatus());
        if (versionStatus == null) {
            throw new IllegalArgumentException("invalid version status:" + ret.toString());
        }
        return new ScmBucket(ret.getName(), ret.getId(), ret.getCreateTime(), ret.getCreateUser(),
                ret.getWorkspace(), ret.getFileTable(), versionStatus, ret.getCustomTag(),
                ret.getUpdateUser(), ret.getUpdateTime(), bucketInfoMgr);
    }
}
