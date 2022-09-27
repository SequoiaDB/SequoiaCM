package com.sequoiacm.datasource;

import com.sequoiacm.datasource.metadata.sftp.SftpDataLocation;
import org.bson.BSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.datasource.metadata.cephswift.CephSwiftDataLocation;
import com.sequoiacm.datasource.metadata.hbase.HbaseDataLocation;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;

public class DatalocationFactory {
    public static ScmLocation createDataLocation(String type, BSONObject location, String siteName)
            throws ScmDatasourceException {
        switch (type) {
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                return new SdbDataLocation(location, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR:
                return new CephS3DataLocation(location, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR:
                return new CephSwiftDataLocation(location, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
                return new HbaseDataLocation(location, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                return new HdfsDataLocation(location, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR:
                return new SftpDataLocation(location, siteName);
            default:
                throw new ScmDatasourceException("unknown datasource type:" + type);
        }
    }
}
