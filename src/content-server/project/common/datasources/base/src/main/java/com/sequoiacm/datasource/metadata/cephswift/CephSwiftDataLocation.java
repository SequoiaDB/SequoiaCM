package com.sequoiacm.datasource.metadata.cephswift;

import java.util.Date;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;

public class CephSwiftDataLocation extends ScmLocation {
    private static final Logger logger = LoggerFactory.getLogger(CephSwiftDataLocation.class);
    private ScmShardingType shardingType = ScmShardingType.MONTH;

    public CephSwiftDataLocation(BSONObject record, String siteName) throws ScmDatasourceException {
        super(record, siteName);
        try {
            Object tmp = record.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            if (tmp != null) {
                shardingType = getShardingType((String) tmp);
            }
        }
        catch (Exception e) {
            logger.error("parse data location failed:location=" + record.toString());
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "parse data location failed:location=" + record.toString(), e);
        }
    }

    @Override
    public String getType() {
        return "ceph_swift";
    }

    public String getContainerName(String wsName, Date createDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(wsName);
        sb.append("_");
        sb.append(CephSwiftMetaDefine.DefaultValue.SWIFT_DEFAULT_CONTAINER_NAME);
        if (shardingType != ScmShardingType.NONE) {
            sb.append("_");
            sb.append(getShardingStr(shardingType, createDate));
        }
        return sb.toString();
    }
}
