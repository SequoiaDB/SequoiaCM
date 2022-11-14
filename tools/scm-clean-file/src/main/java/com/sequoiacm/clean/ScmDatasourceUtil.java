package com.sequoiacm.clean;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.mapping.ScmSiteObj;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.datasourcemgr.DataSourcePluginMgr;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.HadoopSiteUrl;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrlWithConf;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.datasource.metadata.cephswift.CephSwiftDataLocation;
import com.sequoiacm.datasource.metadata.hbase.HbaseDataLocation;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiacm.datasource.metadata.sftp.SftpDataLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.net.ConfigOptions;
import org.bson.BSONObject;

import java.io.File;
import java.util.Map;

public class ScmDatasourceUtil {

    public static ScmSiteUrl createSiteUrl(ScmSiteObj siteObj, String dataPasswdFilePath,
            ConfigOptions sdbConnConf, DatasourceOptions sdbDatasourceConf,
            Map<String, String> datasourceConf) throws ScmDatasourceException {
        switch (siteObj.getDataType()) {
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                return new SdbSiteUrl(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR,
                        siteObj.getDataUrlList(), siteObj.getDataUser(), dataPasswdFilePath,
                        sdbConnConf, sdbDatasourceConf);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                return new HadoopSiteUrl(siteObj.getDataType(), siteObj.getDataUrlList(),
                        siteObj.getDataUser(), dataPasswdFilePath, siteObj.getDataConf());
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR:
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR:
                return new ScmSiteUrlWithConf(siteObj.getDataType(), siteObj.getDataUrlList(),
                        siteObj.getDataUser(), dataPasswdFilePath, datasourceConf);
            default:
                throw new ScmDatasourceException(
                        "unknown datasource type:" + siteObj.getDataType());
        }
    }

    public static ScmService createDataService(int siteId, ScmSiteUrl siteUrl)
            throws ScmServerException {
        try {
            return DataSourcePluginMgr.getInstance().getPlugin().createService(siteId,
                    siteUrl);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to create data service", e);
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException(
                    "create service failed:siteId=" + siteId + ",siteUrl=" + siteUrl, e);
        }
    }

    public static ScmDataOpFactory createDataOpFactory(String dataType) throws ScmServerException {
        ScmDataOpFactory opFactory = null;
        String pluginDir = null;
        DatasourcePlugin plugin = null;
        try {
            if (dataType.equals(ScmDataSourceType.SEQUOIADB.getName())) {
                plugin = DataSourcePluginMgr.getInstance().initPlugin(null,
                        "com.sequoiacm.sequoiadb.SequoiadbPlugin");
                opFactory = plugin.createDataOpFactory();
            }
            else {
                String jarDir = ScmSystemUtils.getMyDir(ScmDatasourceUtil.class);
                PropertiesUtils.setJarPath(jarDir);
                pluginDir = PropertiesUtils.getJarPath() + File.separator + dataType;

                if (dataType.equals(ScmDataSourceType.HBASE.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.hbase.HbasePlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.CEPH_S3.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.cephs3.CephS3Plugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.CEPH_SWIFT.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.cephswift.CephSwiftPlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.HDFS.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.hdfs.HdfsPlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.SFTP.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.sftp.SftpPlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else {
                    throw new ScmSystemException("data source is unrecognized:type=" + dataType);
                }
            }
            return opFactory;
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to create DataOpFactory", e);
        }
    }

    public static ScmLocation createDataLocation(String dataType, BSONObject data, String siteName)
            throws ScmDatasourceException {
        switch (dataType) {
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                return new SdbDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR:
                return new CephS3DataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR:
                return new CephSwiftDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
                return new HbaseDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                return new HdfsDataLocation(data, siteName);
            case CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR:
                return new SftpDataLocation(data, siteName);
            default:
                throw new ScmDatasourceException("unknown datasource type:" + dataType);
        }
    }
}
