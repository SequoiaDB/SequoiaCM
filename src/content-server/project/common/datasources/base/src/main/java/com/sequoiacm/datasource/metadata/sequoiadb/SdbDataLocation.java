package com.sequoiacm.datasource.metadata.sequoiadb;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;

public class SdbDataLocation extends SdbLocation {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataLocation.class);

    private ScmShardingType csShardingType = ScmShardingType.YEAR;
    private ScmShardingType clShardingType = ScmShardingType.MONTH;
    private BSONObject dataCSOptions;
    private BSONObject dataCLOptions;

    public SdbDataLocation(BSONObject dataLocation) throws ScmDatasourceException {
        super(dataLocation);

        Object tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (null != tmp) {
            BSONObject sharding = (BSONObject)tmp;
            tmp = sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
            if (null != tmp) {
                if (tmp.equals("")) {
                    csShardingType = ScmShardingType.YEAR;
                }
                else {
                    csShardingType = getShardingType((String)tmp);
                }
            }

            tmp = sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            if (null != tmp) {
                if (tmp.equals("")) {
                    clShardingType = ScmShardingType.MONTH;
                }
                else {
                    clShardingType = getShardingType((String)tmp);
                }
            }
        }

        if (ScmShardingType.NONE == clShardingType) {
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "data collection's shardingType can't be supportted:type=" + tmp);
        }

        tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS);
        parseDataOptions((BSONObject)tmp);
    }

    public BSONObject getDataCSOptions() {
        return dataCSOptions;
    }

    public BSONObject getDataCLOptions() {
        return dataCLOptions;
    }

    private void parseDataOptions(BSONObject dataOptions) throws ScmDatasourceException {
        if (null == dataOptions) {
            dataCSOptions = new BasicBSONObject();
            dataCLOptions = new BasicBSONObject();
            return;
        }

        Object tmp = null;
        try {
            tmp = dataOptions.get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
            if (null == tmp) {
                dataCSOptions = new BasicBSONObject();
            }
            else {
                dataCSOptions = (BSONObject) tmp;
            }

            tmp = dataOptions.get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            if (null == tmp) {
                dataCLOptions = new BasicBSONObject();
            }
            else {
                dataCLOptions = (BSONObject) tmp;
            }
        }
        catch (Exception e) {
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "dataOptions=" + dataOptions.toString(), e);
        }
    }

    public String getDataCsName(String wsName, Date createDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(wsName);
        sb.append(SdbMetaDefine.CS_LOB_EXTRA);
        if (csShardingType != ScmShardingType.NONE) {
            sb.append("_");
            sb.append(getShardingStr(csShardingType, createDate));
        }

        return sb.toString();
    }

    public String getDataClName(Date createDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(SdbMetaDefine.CL_LOB);
        if (clShardingType != ScmShardingType.NONE) {
            sb.append("_");
            sb.append(getShardingStr(clShardingType, createDate));
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object right) {
        if (right == this) {
            return true;
        }

        if (!(right instanceof SdbLocation)) {
            return false;
        }

        if (!super.equals(right)) {
            return false;
        }

        SdbDataLocation r = (SdbDataLocation)right;
        return csShardingType.equals(r.csShardingType) && clShardingType.equals(r.clShardingType)
                && dataCSOptions.equals(r.dataCSOptions) && dataCLOptions.equals(r.dataCLOptions);
    }
}
