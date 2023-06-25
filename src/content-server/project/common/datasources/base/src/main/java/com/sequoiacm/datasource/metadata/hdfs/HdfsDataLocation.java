package com.sequoiacm.datasource.metadata.hdfs;

import java.io.File;
import java.util.Date;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;

public class HdfsDataLocation extends ScmLocation {
    private static final Logger logger = LoggerFactory.getLogger(HdfsDataLocation.class);

    private ScmShardingType shardingType = ScmShardingType.MONTH;
    private String rootPath = HdfsCommonDefine.HDFS_ROOT_PATH;

    public HdfsDataLocation(BSONObject dataLocation, String siteName)
            throws ScmDatasourceException {
        super(dataLocation, siteName);

        try {
            Object tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
            if (null != tmp) {
                shardingType = getShardingType((String) tmp);
            }

            tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_HDFS_DFS_ROOT_PATH);
            if (null != tmp) {
                rootPath = (String) tmp;
            }
        }
        catch (Exception e) {
            logger.error("parse data loation failed:location=" + dataLocation.toString());
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "parse data loation failed:location=" + dataLocation.toString(), e);
        }
    }

    @Override
    public String getType() {
        return "hdfs";
    }

    public String getFileDir(String wsName, Date createDate, String fileName, String timezone) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDirectory(wsName, createDate, timezone));
        sb.append(File.separator);
        sb.append(fileName);
        return sb.toString();
    }

    public String getDirectory(String wsName, Date createDate, String timezone) {
        StringBuilder sb = new StringBuilder();
        if (!rootPath.startsWith(File.separator)) {
            sb.append(File.separator);
        }
        sb.append(rootPath);
        if (!rootPath.endsWith(File.separator)) {
            sb.append(File.separator);
        }
        sb.append(wsName);
        if (shardingType != ScmShardingType.NONE) {
            sb.append(File.separator);
            sb.append(getShardingStr(shardingType, createDate, timezone));
        }
        return sb.toString();
    }

    public String getRootPath() {
        return rootPath;
    }
}
