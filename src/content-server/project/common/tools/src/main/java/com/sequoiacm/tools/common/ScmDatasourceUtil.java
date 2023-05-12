package com.sequoiacm.tools.common;

import com.sequoiacm.common.CommonDefine.DataSourceType;
import com.sequoiacm.datasource.DatasourcePlugin;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.HadoopSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrlWithConf;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.datasource.DatasourceOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ScmDatasourceUtil {
    private static Logger logger = LoggerFactory.getLogger(ScmDatasourceUtil.class);
    private static Map<String, String> pluginFullNames = new HashMap<>();
    static {
        pluginFullNames.put(DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR,
                "com.sequoiacm.sequoiadb.SequoiadbPlugin");
        pluginFullNames.put(DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR,
                "com.sequoiacm.cephs3.CephS3Plugin");
        pluginFullNames.put(DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR,
                "com.sequoiacm.cephswift.CephSwiftPlugin");
        pluginFullNames.put(DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR,
                "com.sequoiacm.hbase.HbasePlugin");
        pluginFullNames.put(DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR,
                "com.sequoiacm.hdfs.HdfsPlugin");
        pluginFullNames.put(DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR,
                "com.sequoiacm.sftp.SftpPlugin");
    }

    public static void validateDatasourceUrl(ScmSiteInfo siteInfo) throws ScmToolsException {
        DatasourcePlugin plugin = newPluginInstanceByType(siteInfo.getDataType());
        try {
            plugin.createService(siteInfo.getId(), createSiteUrl(siteInfo));
        }
        catch (ScmDatasourceException e) {
            logger.error("failed to connect datasource:type={},url={},conf={}",
                    siteInfo.getDataType(), siteInfo.getDataUrl(), siteInfo.getDataConf(), e);
            throw new ScmToolsException(
                    "failed to connect datasource:type=" + siteInfo.getDataType() + ",url="
                            + siteInfo.getDataUrlStr() + ",conf=" + siteInfo.getDataConf(),
                            ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    private static DatasourcePlugin newPluginInstanceByType(String type) throws ScmToolsException {
        String pluginFullName = pluginFullNames.get(type);
        if (pluginFullName == null) {
            logger.error("invalid datasource type:{}", type);
            throw new ScmToolsException("invalid datasource type:" + type, ScmExitCode.INVALID_ARG);
        }

        String jarDir = ScmContentCommon.getScmLibAbsolutePath() + type;
        URL[] urls = getURLsByPath(jarDir);

        URLClassLoader clazzLoader = (URLClassLoader) ScmDatasourceUtil.class.getClassLoader();

        try {
            addUrlsToClazzLoader(clazzLoader, urls);
        }
        catch (Exception e) {
            logger.error("failed load class", e);
            throw new ScmToolsException("failed to load class", ScmExitCode.SYSTEM_ERROR, e);
        }

        try {
            Class<?> clazz = clazzLoader.loadClass(pluginFullName);
            return (DatasourcePlugin) clazz.newInstance();
        }
        catch (Exception e) {
            logger.error("failed to load class:jarDir={},class={}", jarDir, pluginFullName, e);
            throw new ScmToolsException(
                    "failed to load class:jarDir=" + jarDir + ", class=" + pluginFullName,
                    ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    private static void addUrlsToClazzLoader(URLClassLoader clazzLoader, URL[] urls)
            throws Exception {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        boolean isAccessible = method.isAccessible();
        if (!isAccessible) {
            method.setAccessible(true);
        }

        try {
            for (URL url : urls) {
                method.invoke(clazzLoader, url);
            }
        }
        finally {
            method.setAccessible(isAccessible);
        }
    }

    private static ScmSiteUrl createSiteUrl(ScmSiteInfo siteInfo)
            throws ScmDatasourceException, ScmToolsException {

        switch (siteInfo.getDataType()) {
            case DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR:
                return new SdbSiteUrl(siteInfo.getDataType(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), siteInfo.getDataPasswd(), new ConfigOptions(),
                        new DatasourceOptions());
            case DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR:
                return new ScmSiteUrlWithConf(siteInfo.getDataType(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), siteInfo.getDataPasswd(), Collections.EMPTY_MAP);
            case DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR:
                return new ScmSiteUrl(siteInfo.getDataType(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), siteInfo.getDataPasswd());
            case DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR:
                HashMap<String, String> tmpHbaseConf = new HashMap<>(siteInfo.getDataConf());
                tmpHbaseConf.put("hbase.client.retries.number", "2");
                tmpHbaseConf.put("hbase.client.pause", "100");
                return new HadoopSiteUrl(siteInfo.getDataType(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), siteInfo.getDataPasswd(), tmpHbaseConf);
            case DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR:
                HashMap<String, String> tmpHdfsConf = new HashMap<>(siteInfo.getDataConf());
                tmpHdfsConf.put("dfs.client.failover.max.attempts", "2");
                tmpHdfsConf.put("dfs.client.failover.sleep.max.millis", "1000");
                return new HadoopSiteUrl(siteInfo.getDataType(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), siteInfo.getDataPasswd(), tmpHdfsConf);
            case DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR:
                return new ScmSiteUrlWithConf(siteInfo.getDataType(), siteInfo.getDataUrl(),
                        siteInfo.getDataUser(), siteInfo.getDataPasswd(), null);
            default:
                logger.error("Unknow DataType=" + siteInfo.getDataType() + ",siteName="
                        + siteInfo.getName());
                throw new ScmToolsException("Unknow DataType=" + siteInfo.getDataType()
                + ",siteName=" + siteInfo.getName(), ScmExitCode.INVALID_ARG);
        }
    }

    private static URL[] getURLsByPath(String jarDir) throws ScmToolsException {
        if (jarDir == null) {
            return new URL[] {};
        }
        File jarDirFile = new File(jarDir);

        if (jarDirFile.isFile()) {
            try {
                return new URL[] { jarDirFile.toURI().toURL() };
            }
            catch (MalformedURLException e) {
                throw new ScmToolsException("failed to get URL:file=" + jarDirFile,
                        ScmExitCode.SYSTEM_ERROR, e);
            }
        }
        else {

            File[] files = jarDirFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar") || name.endsWith(".zip");
                }
            });

            if (files == null) {
                return new URL[] {};
            }

            List<URL> urls = new ArrayList<>(files.length);
            for (File file : files) {
                try {
                    urls.add(file.toURI().toURL());
                }
                catch (MalformedURLException e) {
                    throw new ScmToolsException("failed to get URL:file=" + file,
                            ScmExitCode.SYSTEM_ERROR, e);
                }
            }
            return urls.toArray(new URL[urls.size()]);
        }

    }
}
