package com.sequoiacm.datasource.metadata.hbase;

import java.util.Date;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;

public class HbaseDataLocation extends ScmLocation {
    private static final Logger logger = LoggerFactory.getLogger(HbaseDataLocation.class);

    private ScmShardingType shardingType = ScmShardingType.MONTH;

    private String nameSpace = null;

    public HbaseDataLocation(BSONObject dataLocation, String siteName)
            throws ScmDatasourceException {
        super(dataLocation, siteName);

        try {
            Object tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            if (null != tmp) {
                shardingType = getShardingType((String) tmp);
            }

            tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_HABSE_NAME_SPACE);
            nameSpace = (String) tmp;

            // tmp =
            // dataLocation.get(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_OPERATION_TIMEOUT);
            // if (null != tmp) {
            // conf.set(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_OPERATION_TIMEOUT,
            // (String) tmp);
            // }
            //
            // tmp =
            // dataLocation.get(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_PAUSE);
            // if (null != tmp) {
            // conf.set(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_PAUSE, (String)
            // tmp);
            // }
            //
            // tmp =
            // dataLocation.get(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_RETRIES_NUMBER);
            // if (null != tmp) {
            // conf.set(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_RETRIES_NUMBER,
            // (String) tmp);
            // }
            //
            // tmp =
            // dataLocation.get(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD);
            // if (null != tmp) {
            // conf.set(FieldName.FIELD_CLWORKSPACE_HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD,
            // (String) tmp);
            // }

        }
        catch (Exception e) {
            logger.error("parse data location failed:location=" + dataLocation.toString());
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "parse data location failed:location=" + dataLocation.toString(), e);
        }
    }

    @Override
    public String getType() {
        return "hbase";
    }

    public String getTableName(String wsName, Date createDate) {
        StringBuilder sb = new StringBuilder();

        //namespace
        if(nameSpace != null) {
            sb.append(nameSpace);
            sb.append(":");
        }

        //tbalename
        sb.append(wsName);
        sb.append("_");
        sb.append(HbaseMetaDefine.DefaultValue.HBASE_TABLENAME_EXTRA);

        if (shardingType != ScmShardingType.NONE) {
            sb.append("_");
            sb.append(getShardingStr(shardingType, createDate));
        }

        return sb.toString();
    }
}
