package com.sequoiacm.config.framework.bucket.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.bucket.metasource.BucketMetaService;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketBsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigDefine;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigFilterType;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigUpdater;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketNotifyOption;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class BucketDao {
    private static final Logger logger = LoggerFactory.getLogger(BucketDao.class);

    @Autowired
    private BucketMetaService metaService;

    @Autowired
    private BucketBsonConverter converter;

    @Autowired
    private Metasource metasource;

    @Autowired
    private DefaultVersionDao versionDao;

    @PostConstruct
    public void initBucketVersion() throws ScmConfigException {
        try {
            // BUCKET配置，除了每个 bucket 有一条独立的版本号记录，全局还有一条特殊版本号记录，任意BUCKET 被修改时，该记录的版本会递增
            versionDao.createVersion(ScmConfigNameDefine.BUCKET,
                    BucketConfigDefine.ALL_BUCKET_VERSION);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                return;
            }
            throw e;
        }
    }

    public void deleteBucketMetaSilence(String ws) {
        MetaCursor cursor = null;
        Transaction trans = null;
        try {
            trans = metasource.createTransaction();
            TableDao bucketTable = metaService.getBucketTable(trans);
            cursor = listBuckets(
                    new BucketConfigFilter(new BasicBSONObject(FieldName.Bucket.WORKSPACE, ws)));
            trans.begin();
            while (cursor.hasNext()) {
                BSONObject bucket = cursor.getNext();
                String bucketName = BsonUtils.getStringChecked(bucket, FieldName.Bucket.NAME);
                bucketTable.delete(new BasicBSONObject(FieldName.Bucket.NAME, bucketName));
                versionDao.deleteVersion(ScmConfigNameDefine.BUCKET, bucketName, trans);
            }
            trans.commit();
        }
        catch (Exception e) {
            if (trans != null) {
                trans.rollback();
            }
            logger.warn("failed to delete bucket meta:workspace={}", ws, e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if (trans != null) {
                trans.close();
            }
        }
    }

    public ScmConfOperateResult deleteBucket(BucketConfigFilter filter) throws ScmConfigException {
        if (filter.getType() != BucketConfigFilterType.EXACT_MATCH) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "delete bucket only support EXACT_MATCH:" + filter);
        }
        BSONObject deletedBucket;
        Transaction trans = metasource.createTransaction();
        try {
            trans.begin();
            TableDao table = metaService.getBucketTable(trans);
            deletedBucket = table.deleteAndCheck(
                    new BasicBSONObject(FieldName.Bucket.NAME, filter.getBucketName()));
            versionDao.deleteVersion(ScmConfigNameDefine.BUCKET, filter.getBucketName(), trans);
            trans.commit();
        }
        catch (Exception e) {
            trans.rollback();
            throw e;
        }
        finally {
            trans.close();
        }

        if (deletedBucket != null) {
            metaService.dropBucketFileTableSilence(
                    BsonUtils.getStringChecked(deletedBucket, FieldName.Bucket.FILE_TABLE));
            BucketConfig bucket = converter.convertToConfig(deletedBucket);

            // 删除 bucket，不需要递增全局版本，所以通知给一个 -1 无效的全局版本
            BucketNotifyOption notifyOption = new BucketNotifyOption(bucket.getName(), -1,
                    EventType.DELTE, -1);
            ScmConfEventBase event = new ScmConfEventBase(ScmConfigNameDefine.BUCKET, notifyOption);
            return new ScmConfOperateResult(bucket, event);
        }

        throw new ScmConfigException(ScmConfError.BUCKET_NOT_EXIST,
                "bucket not exist:" + filter.getBucketName());
    }

    public ScmConfOperateResult createBucket(BucketConfig config) throws ScmConfigException {
        if (isBucketExist(config.getName())) {
            throw new ScmConfigException(ScmConfError.BUCKET_EXIST,
                    "bucket exist:" + config.getName());
        }

        Date createTime = new Date();
        long bucketId = metaService.genBucketId();
        String tableName = metaService.createBucketFileTable(config.getWorkspace(), bucketId);
        config.setId(bucketId);
        config.setCreateTime(createTime.getTime());
        config.setUpdateTime(createTime.getTime());
        config.setFileTable(tableName);
        Transaction trans = metasource.createTransaction();
        try {
            trans.begin();
            TableDao table = metaService.getBucketTable(trans);
            versionDao.createVersion(ScmConfigNameDefine.BUCKET, config.getName());
            table.insert(config.toBSONObject());
            trans.commit();
        }
        catch (Exception e) {
            trans.rollback();
            // 这张表的表名是由桶ID而不是表名构成，即便当前这个异常是相同桶已存在，注意这里依然要drop掉它（因为它不是已存在桶的表）
            metaService.dropBucketFileTableSilence(tableName);
            if (e instanceof ScmConfigException) {
                if (((ScmConfigException) e).getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.BUCKET_EXIST,
                            "bucket exist:" + config.getName());
                }
            }
            throw e;
        }
        finally {
            trans.close();
        }
        // 创建 bucket，不需要递增全局版本，所以通知给一个 -1 无效的全局版本
        BucketNotifyOption notifyOption = new BucketNotifyOption(config.getName(), 1,
                EventType.CREATE, -1);
        ScmConfEventBase event = new ScmConfEventBase(ScmConfigNameDefine.BUCKET, notifyOption);
        return new ScmConfOperateResult(config, event);
    }

    private boolean isBucketExist(String name) throws ScmConfigException {
        TableDao table = metaService.getBucketTable(null);
        BSONObject bucketRecord = table.queryOne(new BasicBSONObject(FieldName.Bucket.NAME, name),
                null, null);
        return bucketRecord != null;
    }

    public List<Config> getBuckets(BucketConfigFilter filter) throws ScmConfigException {
        List<Config> ret = new ArrayList<>();
        MetaCursor cursor = listBuckets(filter);
        try {
            while (cursor.hasNext()) {
                ret.add(converter.convertToConfig(cursor.getNext()));
            }
            return ret;
        }
        finally {
            cursor.close();
        }

    }

    public MetaCursor listBuckets(BucketConfigFilter filter) throws MetasourceException {
        TableDao table = metaService.getBucketTable(null);
        if (filter.getType() == BucketConfigFilterType.FUZZY_MATCH) {
            return table.query(filter.getMatcher(), null, filter.getOrderBy(), filter.getSkip(),
                    filter.getLimit());
        }
        return table.query(new BasicBSONObject(FieldName.Bucket.NAME, filter.getBucketName()), null,
                null);
    }

    public long countBucket(BucketConfigFilter filter) throws MetasourceException {
        TableDao table = metaService.getBucketTable(null);
        if (filter.getType() == BucketConfigFilterType.FUZZY_MATCH) {
            return table.count(filter.getMatcher());
        }
        return table.count(new BasicBSONObject(FieldName.Bucket.NAME, filter.getBucketName()));
    }

    public ScmConfOperateResult updateBucket(BucketConfigUpdater bucketConfigUpdater)
            throws ScmConfigException {
        // 实现 updateBucket ，在更新特定 bucket 版本时，也要更新全局版本 ALL_BUCKET_VERSION！
        Transaction transaction = metasource.createTransaction();
        try {
            transaction.begin();
            TableDao bucketTable = metaService.getBucketTable(transaction);
            BSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                    bucketConfigUpdater.getBucketName());
            BSONObject newRecord = null;

            BSONObject updator = new BasicBSONObject()
                    .append(FieldName.Bucket.UPDATE_USER, bucketConfigUpdater.getUpdateUser())
                    .append(FieldName.Bucket.UPDATE_TIME, System.currentTimeMillis());
            String versionStatus = bucketConfigUpdater.getVersionStatus();
            if (!StringUtils.isEmpty(versionStatus)) {
                updator.put(FieldName.Bucket.VERSION_STATUS, versionStatus);
            }
            Map<String, String> customTag = bucketConfigUpdater.getCustomTag();
            if (customTag != null) {
                updator.put(FieldName.Bucket.CUSTOM_TAG, customTag);
            }
            newRecord = bucketTable.updateAndCheck(matcher, updator);
            if (newRecord == null) {
                throw new ScmConfigException(ScmConfError.BUCKET_NOT_EXIST,
                        "bucket not exist:" + bucketConfigUpdater.getBucketName());
            }

            Integer bucketVersion = versionDao.increaseVersion(ScmConfigNameDefine.BUCKET,
                    bucketConfigUpdater.getBucketName(), transaction);
            Integer globalVersion = versionDao.increaseVersion(ScmConfigNameDefine.BUCKET,
                    BucketConfigDefine.ALL_BUCKET_VERSION, transaction);
            transaction.commit();

            BucketNotifyOption notifyOption = new BucketNotifyOption(
                    bucketConfigUpdater.getBucketName(), bucketVersion, EventType.UPDATE,
                    globalVersion);
            ScmConfEventBase event = new ScmConfEventBase(ScmConfigNameDefine.BUCKET, notifyOption);
            return new ScmConfOperateResult(converter.convertToConfig(newRecord), event);
        }
        catch (Exception e) {
            transaction.rollback();
            throw e;
        }
        finally {
            transaction.close();
        }

    }
}
