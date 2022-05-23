package com.sequoiacm.datasource.metadata.sftp;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class SftpDataLocation extends ScmLocation {
    private static final Logger logger = LoggerFactory.getLogger(SftpDataLocation.class);

    private ScmShardingType shardingType = ScmShardingType.DAY;
    private String dataPath = "/scmfile/";

    public SftpDataLocation(BSONObject record) throws ScmDatasourceException {
        super(record);
        try {
            Object tmp = record.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            if (tmp != null) {
                shardingType = getShardingType((String) tmp);
            }
            String path = (String) record.get(FieldName.FIELD_CLWORKSPACE_DATA_PATH);
            if (path != null && !path.isEmpty()) {
                if (!path.startsWith("/")) {
                    throw new ScmDatasourceException(
                            "dataPath must be an absolute path, dataPath=" + path);
                }
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                this.dataPath = path;
            }

        }
        catch (ScmDatasourceException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("parse data location failed:location=" + record.toString());
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "parse data location failed:location=" + record.toString(), e);
        }
    }

    @Override
    public String getType() {
        return CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR;
    }

    public String getFileDir(String wsName, Date createDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(dataPath).append(wsName).append("/");
        if (shardingType != ScmShardingType.NONE) {
            sb.append(getShardingStr(shardingType, createDate).toLowerCase()).append("/");
        }
        return sb.toString();
    }

    public String getFilePath(String wsName, Date createDate, String dataId) {
        return getFileDir(wsName, createDate) + dataId;
    }

}
