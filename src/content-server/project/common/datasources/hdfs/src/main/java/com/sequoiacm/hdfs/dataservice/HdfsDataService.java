package com.sequoiacm.hdfs.dataservice;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.HadoopSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.hdfs.HdfsCommonDefine;
import com.sequoiacm.hdfs.HdfsException;

public class HdfsDataService extends ScmService {
    private static final Logger logger = LoggerFactory.getLogger(HdfsDataService.class);
    private Configuration conf = null;
    private Map<String, String> dataConf = new HashMap<String, String>();

    public HdfsDataService(int siteId, ScmSiteUrl siteUrl) throws HdfsException {
        super(siteId, siteUrl);
        try {
            HadoopSiteUrl hdfsSiteUrl = (HadoopSiteUrl) siteUrl;
            dataConf = hdfsSiteUrl.getDataConf();
            String user = siteUrl.getUser();
            System.setProperty(HdfsCommonDefine.HDFS_USER_NAME, user);
            logger.info("create HdfsDataService , site_user : " + user);
            parseConf();

            // try to connect.
            FileSystem fs = getFileSystem();
            try {
                fs.getStatus();
            }
            finally {
                fs.close();
            }
        }
        catch (Exception e) {
            throw new HdfsException("create HdfsDataService failed:siteId=" + siteId + ",siteUrl="
                    + siteUrl + ", dataConf=" + dataConf, e);
        }

    }

    private void parseConf() throws HdfsException {
        logger.info("parse hdfs Configuration, dataConf=" + dataConf);
        conf = new Configuration();
        if (null != dataConf) {
            for (Map.Entry<String, String> entry : dataConf.entrySet()) {
                conf.set(entry.getKey(), entry.getValue());
            }
        }
        else {
            throw new HdfsException("create HdfsDataService failed:siteId=" + siteId + ",siteUrl="
                    + siteUrl + ", dataConf=" + dataConf);
        }
    }

    public FileSystem getFileSystem() throws HdfsException {
        try {
            return FileSystem.get(conf);
        }
        catch (Exception e) {
            throw new HdfsException("get FileSystem failed:siteId=" + siteId + ",siteUrl=" + siteUrl
                    + ", dataConf=" + dataConf, e);
        }
    }

    @Override
    public String getType() {
        return "hdfs";
    }

    @Override
    public void clear() {
        conf = null;
    }

    public void delete(FileSystem fs, Path filePath) throws HdfsException {

        if (!isExist(fs, filePath)) {
            logger.error("file is not exist,delete file failed:siteId=" + siteId + ",fileName="
                    + filePath.getName());
            throw new HdfsException(HdfsException.HDFS_ERROR_FILE_NOT_EXIST,
                    "file is not exist,delete file failed:siteId=" + siteId + ",fileName="
                            + filePath.getName());
        }

        try {
            fs.delete(filePath, false);
        }
        catch (Exception e) {

            logger.error("delete file failed:siteId=" + siteId + ",fileName=" + filePath.getName(),
                    e);
            throw new HdfsException(
                    "delete file failed:siteId=" + siteId + ",fileName=" + filePath.getName(), e);
        }
    }

    public boolean isExist(FileSystem fileSystem, Path filePath) throws HdfsException {
        try {
            return fileSystem.exists(filePath);
        }
        catch (Exception e) {
            throw new HdfsException("check file isExist failed:filePath=" + filePath.getName(), e);
        }
    }

    public long getFileSize(FileSystem fileSystem, Path filePath) throws HdfsException {
        long len = 0;

        if (!isExist(fileSystem, filePath)) {
            logger.error("file is not exist,get file size failed:siteId=" + siteId + ",fileName="
                    + filePath.getName());
            throw new HdfsException(HdfsException.HDFS_ERROR_FILE_NOT_EXIST,
                    "file is not exist,get file size failed:siteId=" + siteId + ",fileName="
                            + filePath.getName());
        }

        try {
            len = fileSystem.getFileStatus(filePath).getLen();
        }
        catch (Exception e) {
            throw new HdfsException("get file size failed:filePath=" + filePath.getName(), e);
        }

        return len;
    }

}
