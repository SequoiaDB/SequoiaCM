package com.sequoiacm.contentserver.site;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.datasourcemgr.DataSourcePluginMgr;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.exception.ScmError;

public class ScmSiteMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmSiteMgr.class);

    private ScmMetaService metaService = null;
    private ScmService dataService = null;
    private ScmDataOpFactory opFactory = null;

    public ScmSiteMgr() {
    }

    public ScmService getDataService() {
        return dataService;
    }

    public ScmMetaService getMetaService() {
        return metaService;
    }

    public void clear() {
        if (null != dataService) {
            dataService.clear();
        }

        if (null != metaService) {
            metaService.close();
        }

        dataService = null;
        metaService = null;
        opFactory = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("meta:").append(metaService.toString()).append('\n');
        sb.append("data:").append(dataService.toString()).append('\n');

        if (sb.length() > 0) {
            // eat last '\n'
            return sb.substring(0, sb.length() - 1);
        }

        return sb.toString();
    }

    private ScmMetaService createMetaService(int siteId, ScmSiteUrl siteUrl)
            throws ScmServerException {
        try {
            return new ScmMetaService(siteId, siteUrl);
        }
        catch (ScmServerException e) {
            logger.error("create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl);
            throw e;
        }
        catch (Exception e) {
            logger.error("create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl);
            throw new ScmInvalidArgumentException(
                    "create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl, e);
        }
    }

    private ScmService createDataService(int siteId, ScmSiteUrl siteUrl) throws ScmServerException {
        try {
            return DataSourcePluginMgr.getInstance().getPlugin().createService(siteId,
                    siteUrl);
        }
        catch (ScmDatasourceException e) {
            logger.error("create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl);
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to create data service", e);
        }
        catch (Exception e) {
            logger.error("create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl);
            throw new ScmInvalidArgumentException(
                    "create service failed:siteI=" + siteId + ",siteUrl=" + siteUrl, e);
        }
    }

    public ScmMetaService addMetaService(int siteId, ScmSiteUrl metaUrl) throws ScmServerException {
        if (!metaUrl.getType().equals(ScmDataSourceType.SEQUOIADB.getName())) {
            throw new ScmInvalidArgumentException(
                    "meta service only support SequoiaDB:type=" + metaUrl.getType());
        }

        metaService = createMetaService(siteId, metaUrl);
        return metaService;
    }

    public ScmService addDataService(int siteId, ScmSiteUrl dataUrl) throws ScmServerException {
        dataService = createDataService(siteId, dataUrl);
        return dataService;
    }

    public ScmDataOpFactory getOpFactory() {
        return opFactory;
    }

    public void setOpFactory(String datasourceType) throws ScmServerException {
        String pluginDir = null;
        DatasourcePlugin plugin = null;
        try {
            if (datasourceType.equals(ScmDataSourceType.SEQUOIADB.getName())) {
                plugin = DataSourcePluginMgr.getInstance().initPlugin(null,
                        "com.sequoiacm.sequoiadb.SequoiadbPlugin");
                opFactory = plugin.createDataOpFactory();
            } else {
                pluginDir = PropertiesUtils.getJarPath() + File.separator + datasourceType;
                logger.info("loading plugin:pluginDir={}, datasourceType={}", pluginDir,
                        datasourceType);

                if (datasourceType.equals(ScmDataSourceType.HBASE.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.hbase.HbasePlugin");
                    opFactory = plugin.createDataOpFactory();
                } else if (datasourceType.equals(ScmDataSourceType.CEPH_S3.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.cephs3.CephS3Plugin");
                    opFactory = plugin.createDataOpFactory();
                } else if (datasourceType.equals(ScmDataSourceType.CEPH_SWIFT.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.cephswift.CephSwiftPlugin");
                    opFactory = plugin.createDataOpFactory();
                } else if (datasourceType.equals(ScmDataSourceType.HDFS.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.hdfs.HdfsPlugin");
                    opFactory = plugin.createDataOpFactory();
                } else if (datasourceType.equals(ScmDataSourceType.SFTP.getName())) {
                    plugin = DataSourcePluginMgr.getInstance().initPlugin(pluginDir,
                            "com.sequoiacm.sftp.SftpPlugin");
                    opFactory = plugin.createDataOpFactory();
                }
                else {
                    logger.error("data source is unrecognized:type={}", datasourceType);
                    throw new ScmSystemException(
                            "data source is unrecognized:type=" + datasourceType);
                }
            }
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to create DataOpFactory", e);
        }
    }
}

