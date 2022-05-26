package com.sequoiacm.infrastructure.config.core.msg.bucket;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.Converter;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Converter
public class BucketBsonConverter implements BsonConverter {
    @Override
    public BucketConfig convertToConfig(BSONObject config) {
        BucketConfig bucketConfig = new BucketConfig();
        bucketConfig.setId(BsonUtils.getNumberChecked(config, FieldName.Bucket.ID).longValue());
        bucketConfig.setName(BsonUtils.getStringChecked(config, FieldName.Bucket.NAME));
        bucketConfig.setCreateTime(
                BsonUtils.getNumberOrElse(config, FieldName.Bucket.CREATE_TIME, -1).longValue());
        bucketConfig.setUpdateTime(
                BsonUtils.getNumberOrElse(config, FieldName.Bucket.UPDATE_TIME, -1).longValue());
        bucketConfig.setCreateUser(BsonUtils.getString(config, FieldName.Bucket.CREATE_USER));
        bucketConfig.setUpdateUser(BsonUtils.getString(config, FieldName.Bucket.UPDATE_USER));
        bucketConfig.setWorkspace(BsonUtils.getStringChecked(config, FieldName.Bucket.WORKSPACE));
        bucketConfig.setFileTable(BsonUtils.getString(config, FieldName.Bucket.FILE_TABLE));
        bucketConfig.setVersionStatus(BsonUtils.getString(config, FieldName.Bucket.VERSION_STATUS));
        return bucketConfig;
    }

    @Override
    public BucketConfigFilter convertToConfigFilter(BSONObject configFilter)
            throws ScmConfigException {
        String typeStr = BsonUtils.getStringChecked(configFilter,
                ScmRestArgDefine.BUCKET_CONF_FILTER_TYPE);
        BucketConfigFilterType type = BucketConfigFilterType.valueOf(typeStr);
        if (type == BucketConfigFilterType.EXACT_MATCH) {
            String bucketName = BsonUtils.getStringChecked(configFilter,
                    ScmRestArgDefine.BUCKET_CONF_FILTER_NAME);
            return new BucketConfigFilter(bucketName);
        }

        BSONObject matcher = BsonUtils.getBSON(configFilter,
                ScmRestArgDefine.BUCKET_CONF_FILTER_MATCHER);
        BSONObject orderby = BsonUtils.getBSON(configFilter,
                ScmRestArgDefine.BUCKET_CONF_FILTER_ORDERBY);
        long skip = BsonUtils
                .getNumberOrElse(configFilter, ScmRestArgDefine.BUCKET_CONF_FILTER_SKIP, 0)
                .longValue();
        long limit = BsonUtils
                .getNumberOrElse(configFilter, ScmRestArgDefine.BUCKET_CONF_FILTER_LIMIT, -1)
                .longValue();
        return new BucketConfigFilter(matcher, orderby, limit, skip);
    }

    @Override
    public BucketNotifyOption convertToNotifyOption(EventType type, BSONObject notifyOption) {
        Integer version = BsonUtils.getInteger(notifyOption, ScmRestArgDefine.BUCKET_CONF_VERSION);
        String bucketName = BsonUtils.getStringChecked(notifyOption, FieldName.Bucket.NAME);
        int globalVersion = BsonUtils.getInteger(notifyOption,
                ScmRestArgDefine.BUCKET_CONF_GLOBAL_VERSION);
        return new BucketNotifyOption(bucketName, version, type, globalVersion);
    }

    @Override
    public ConfigUpdator convertToConfigUpdator(BSONObject configUpdator) {
        return new BucketConfigUpdater(
                BsonUtils.getStringChecked(configUpdator, FieldName.Bucket.NAME),
                BsonUtils.getStringChecked(configUpdator, FieldName.Bucket.VERSION_STATUS),
                BsonUtils.getStringChecked(configUpdator, FieldName.Bucket.UPDATE_USER));
    }

    @Override
    public VersionFilter convertToVersionFilter(BSONObject versionFilter) {
        return new DefaultVersionFilter(versionFilter);
    }

    @Override
    public Version convertToVersion(BSONObject version) {
        return new DefaultVersion(version);
    }

    @Override
    public String getConfigName() {
        return ScmConfigNameDefine.BUCKET;
    }
}
