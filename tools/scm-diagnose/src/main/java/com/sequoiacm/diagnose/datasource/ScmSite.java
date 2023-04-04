package com.sequoiacm.diagnose.datasource;

import com.sequoiacm.contentserver.common.ScmClassLoader;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.slowlog.SlowLogManager;
import com.sequoiacm.tools.common.ScmDatasourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ScmSite {
    private static final Logger logger = LoggerFactory.getLogger(ScmSite.class);
    private ScmService dataService = null;
    private ScmDataOpFactory opFactory = null;
    private DatasourcePlugin plugin = null;

    public ScmSite(int siteId, ScmSiteUrl dataUrl, String dataType) throws ScmServerException {
        opFactory = createDataOpFactory(dataType);
        dataService = createDataService(siteId, dataUrl);
    }

    public ScmService getDataService() {
        return dataService;
    }

    private ScmService createDataService(int siteId, ScmSiteUrl siteUrl) throws ScmServerException {
        try {
            return this.plugin.createService(siteId, siteUrl);
        }
        catch (ScmDatasourceException e) {
            logger.error("create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl);
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "Failed to create data service", e);
        }
        catch (Exception e) {
            logger.error("create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl);
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl, e);
        }
    }

    public void clear() {
        if (null != dataService) {
            dataService.clear();
        }

        dataService = null;
        opFactory = null;
    }

    public ScmDataOpFactory getOpFactory() {
        return opFactory;
    }

    private ScmDataOpFactory createDataOpFactory(String dataType) throws ScmServerException {
        String pluginDir = null;
        try {
            if (dataType.equals(ScmDataSourceType.SEQUOIADB.getName())) {
                initPlugin(null, "com.sequoiacm.sequoiadb.SequoiadbPlugin");
                opFactory = plugin.createDataOpFactory();
            }
            else {
                String jarDir = ScmSystemUtils.getMyDir(ScmDatasourceUtil.class);
                PropertiesUtils.setJarPath(jarDir);
                pluginDir = PropertiesUtils.getJarPath() + File.separator + dataType;
                if (dataType.equals(ScmDataSourceType.HBASE.getName())) {
                    initPlugin(pluginDir, "com.sequoiacm.hbase.HbasePlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.CEPH_S3.getName())) {
                    initPlugin(pluginDir, "com.sequoiacm.cephs3.CephS3Plugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.CEPH_SWIFT.getName())) {
                    initPlugin(pluginDir, "com.sequoiacm.cephswift.CephSwiftPlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.HDFS.getName())) {
                    initPlugin(pluginDir, "com.sequoiacm.hdfs.HdfsPlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else if (dataType.equals(ScmDataSourceType.SFTP.getName())) {
                    initPlugin(pluginDir, "com.sequoiacm.sftp.SftpPlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else {
                    throw new ScmServerException(ScmError.SYSTEM_ERROR,
                            "data source is unrecognized:type=" + dataType);
                }
            }
            return opFactory;
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "Failed to create DataOpFactory",
                    e);
        }
    }

    private void initPlugin(String pluginDir, String className) throws ScmServerException {
        if (null != plugin) {
            return;
        }
        Object tmpObj = null;
        try {
            if (null != pluginDir) {
                ScmClassLoader.getInstance().addJar(pluginDir);
                String packageName = className.substring(0, className.lastIndexOf("."));
                SlowLogManager.addPackage(packageName,
                        ScmClassLoader.getInstance().getInnerLoader());
            }
            Class<?> clazz = ScmClassLoader.getInstance().loadClass(className);
            tmpObj = clazz.newInstance();
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "newInstance failed:className=" + className, e);
        }

        plugin = (DatasourcePlugin) tmpObj;
    }
}
