package com.sequoiacm.diagnose.utils;

import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.HadoopSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrlWithConf;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScmDataSourceUtils {
    public static ScmSiteUrl createSiteUrl(ScmSiteInfo siteInfo, String dataPasswdFilePath)
            throws ScmDatasourceException {
        switch (siteInfo.getDataTypeStr()) {
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                return new SdbSiteUrl(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR,
                        siteInfo.getDataUrl(), siteInfo.getDataUser(), dataPasswdFilePath,
                        new com.sequoiadb.net.ConfigOptions(), getSdbDatasourceConf());
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                Map<String, String> conf = siteInfo.getDataConf();
                conf.put("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
                return new HadoopSiteUrl(siteInfo.getDataTypeStr(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), dataPasswdFilePath, siteInfo.getDataConf());
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR:
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR:
                return new ScmSiteUrlWithConf(siteInfo.getDataTypeStr(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), dataPasswdFilePath, new HashMap<String, String>());
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR:
                return new ScmSiteUrlWithConf(siteInfo.getDataTypeStr(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), dataPasswdFilePath, null);
            default:
                throw new ScmDatasourceException(
                        "unknown datasource type:" + siteInfo.getDataTypeStr());
        }
    }

    public static SequoiadbDatasource createSdbMetaSource(List<String> metaSdbCoord,
            String metaSdbUser, String metaSdbPassword) {
        return new SequoiadbDatasource(metaSdbCoord, metaSdbUser, metaSdbPassword,
                new ConfigOptions(), getSdbDatasourceConf());
    }

    private static DatasourceOptions getSdbDatasourceConf() {
        DatasourceOptions sdbDatasourceConf = new DatasourceOptions();
        List<String> preferedInstance = new ArrayList<>();
        preferedInstance.add("M");
        sdbDatasourceConf.setPreferedInstance(preferedInstance);
        return sdbDatasourceConf;
    }
}
