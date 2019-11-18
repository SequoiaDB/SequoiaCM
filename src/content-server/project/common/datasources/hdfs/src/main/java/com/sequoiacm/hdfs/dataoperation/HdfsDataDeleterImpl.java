package com.sequoiacm.hdfs.dataoperation;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.hdfs.HdfsException;
import com.sequoiacm.hdfs.dataservice.HdfsDataService;

public class HdfsDataDeleterImpl implements ScmDataDeletor {
    private static final Logger logger = LoggerFactory.getLogger(HdfsDataDeleterImpl.class);

    private FileSystem fileSystem;
    private String filePath;
    private HdfsDataService dataService;

    public HdfsDataDeleterImpl(String wsName, HdfsDataLocation dataLocation,
            ScmService service, ScmDataInfo dataInfo)
                    throws HdfsException {
        try {
            this.filePath = dataLocation.getFileDir(wsName, dataInfo.getCreateTime(),
                    dataInfo.getId());
            this.dataService = (HdfsDataService) service;
            this.fileSystem = dataService.getFileSystem();
        }
        catch (Exception e) {
            logger.error("construct HdfsFileContentDeleter failed:filePath=" + filePath, e);
            releaseReource();
            throw new HdfsException(
                    "construct HdfsFileContentDeleter failed:filePath=" + filePath, e);
        }
    }

    @Override
    public void delete() throws HdfsException {
        try {
            dataService.delete(fileSystem, new Path(filePath));
        }
        catch (HdfsException e) {
            logger.error("delete file failed:filePath=" + filePath, e);
            throw e;
        }
        catch (Exception e) {
            logger.error("delete file failed:filePath=" + filePath, e);
            throw new HdfsException("delete file failed:filePath=" + filePath, e);
        }
        finally {
            releaseReource();
        }
    }

    private void releaseReource() {
        try {

            if (null != fileSystem) {
                fileSystem.close();
            }
            dataService = null;
        }
        catch (Exception e) {
            logger.warn("close fileSystem failed:filePath=" + filePath, e);
        }
    }
}
