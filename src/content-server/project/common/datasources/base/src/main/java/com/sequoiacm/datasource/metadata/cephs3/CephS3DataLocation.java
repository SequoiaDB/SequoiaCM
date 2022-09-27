package com.sequoiacm.datasource.metadata.cephs3;

import java.util.Date;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;

public class CephS3DataLocation extends ScmLocation {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataLocation.class);
    private ScmShardingType shardingType = ScmShardingType.MONTH;
    private String prefixBucketName;
    private String bucketName;
    private ScmShardingType objectShardingType;

    public CephS3DataLocation(BSONObject record, String siteName) throws ScmDatasourceException {
        super(record, siteName);
        try {
            Object tmp = record.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            if (tmp != null) {
                shardingType = getShardingType((String) tmp);
            }
            tmp = record.get(FieldName.FIELD_CLWORKSPACE_OBJECT_SHARDING_TYPE);
            if (tmp != null) {
                objectShardingType = getShardingType((String) tmp);
            }
            prefixBucketName = (String) record.get(FieldName.FIELD_CLWORKSPACE_CONTAINER_PREFIX);
            bucketName = (String) record.get(FieldName.FIELD_CLWORKSPACE_BUCKET_NAME);
        }
        catch (Exception e) {
            logger.error("parse data location failed:location=" + record.toString());
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "parse data location failed:location=" + record.toString(), e);
        }
    }

    @Override
    public String getType() {
        return "ceph_s3";
    }

    /**
     * Bucket names should not contain underscores
     * Bucket names should be between 3 and 63 characters long
     * Bucket names should not end with a dash
     * Bucket names cannot contain adjacent periods
     * Bucket names cannot contain dashes next to periods (e.g.,
     * "my-.bucket.com" and "my.-bucket" are invalid)
     * Bucket names cannot contain uppercase characters
     **/
    public String getBucketName(String wsName, Date createDate) {
        if (bucketName != null && !bucketName.isEmpty()) {
            return bucketName;
        }
        StringBuilder sb = new StringBuilder();
        if (prefixBucketName == null) {
            // use default prefix
            sb.append(wsName.toLowerCase().replace("_", "-"));
            sb.append("-");
            sb.append(CephS3MetaDefine.DefaultValue.S3_BUCKETNAME_EXTRA);
            if (shardingType != ScmShardingType.NONE) {
                sb.append("-");
                sb.append(getShardingStr(shardingType, createDate).toLowerCase());
            }
        }
        else {
            // use custom prefix
            sb.append(prefixBucketName);
            sb.append(getShardingStr(shardingType, createDate).toLowerCase());
        }
        return sb.toString();
    }

    public String getObjectId(String dataId, String wsName, Date createTime) {
        if (objectShardingType != null && objectShardingType != ScmShardingType.NONE) {
            return wsName + "/" + getShardingStr(objectShardingType, createTime) + "/" + dataId;
        }
        else {
            return dataId;
        }
    }

    public String getUserBucketName() {
        return bucketName;
    }
}
