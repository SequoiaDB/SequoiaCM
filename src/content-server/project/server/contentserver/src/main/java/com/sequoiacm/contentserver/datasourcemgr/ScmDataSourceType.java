package com.sequoiacm.contentserver.datasourcemgr;

import com.sequoiacm.common.CommonDefine;

public enum ScmDataSourceType {
    SEQUOIADB(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR, CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB),
    HBASE(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR, CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE),
    CEPH_S3(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR, CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3),
    CEPH_SWIFT(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR, CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT),
    HDFS(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR, CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS);

    private String name;
    private int type;

    private ScmDataSourceType(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public static ScmDataSourceType getDataSourceType(String name) {
        for (ScmDataSourceType value : ScmDataSourceType.values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }

        return null;
    }
}
