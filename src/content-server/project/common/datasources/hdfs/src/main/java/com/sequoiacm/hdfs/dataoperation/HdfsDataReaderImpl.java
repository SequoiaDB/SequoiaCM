package com.sequoiacm.hdfs.dataoperation;

import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.hdfs.HdfsException;
import com.sequoiacm.hdfs.dataservice.HdfsDataService;

public class HdfsDataReaderImpl implements ScmDataReader {
    private static final Logger logger = LoggerFactory.getLogger(HdfsDataReaderImpl.class);

    private FileSystem fileSystem;
    private String filePath;
    private HdfsDataService dataService;
    private FSDataInputStream inputStream = null;
    private long size = 0;
    private boolean isEof;
    private Path path;

    @SlowLog(operation = "openReader", extras = {
            @SlowLogExtra(name = "readHdfsFilePath", data = "filePath"),
            @SlowLogExtra(name = "readHdfsFileName", data = "dataInfo.getId()") })
    public HdfsDataReaderImpl(String wsName, HdfsDataLocation dataLocation,
            ScmService service, ScmDataInfo dataInfo)
                    throws HdfsException {
        try {
            this.filePath = dataLocation.getFileDir(wsName, dataInfo.getCreateTime(),
                    dataInfo.getId(), ScmIdParser.getTimezoneName(dataInfo.getId()));
            this.path = new Path(filePath);
            this.dataService = (HdfsDataService) service;
            this.fileSystem = dataService.getFileSystem();
            this.size = dataService.getFileSize(this.fileSystem, this.path);
            openFile();
        }
        catch (HdfsException e) {
            logger.error("construct HdfsFileContentReader failed:filePath=" + this.filePath, e);
            releaseReource();
            throw e;
        }
        catch (Exception e) {
            logger.error("construct HdfsFileContentReader failed:filePath=" + this.filePath, e);
            releaseReource();
            throw new HdfsException(
                    "construct HdfsFileContentReader failed:filePath=" + this.filePath, e);
        }
    }

    private void openFile() throws HdfsException {
        // 判断文件是否存在
        if (dataService.isExist(fileSystem, path)) {
            try {
                inputStream = fileSystem.open(path);
            }
            catch (Exception e) {
                throw new HdfsException(
                        "create FSDataInputStream failed:filePath=" + filePath, e);
            }
        }
        else {
            throw new HdfsException(HdfsException.HDFS_ERROR_FILE_NOT_EXIST,
                    "file data not exist:filePath=" + filePath);
        }
    }

    @Override
    @SlowLog(operation = "closeReader")
    public void close() {
        releaseReource();
    }

    @Override
    @SlowLog(operation = "readData")
    public int read(byte[] buff, int offset, int len) throws HdfsException {
        try {
            int readLen = inputStream.read(buff, offset, len);
            if (readLen == -1) {
                this.isEof = true;
            }
            return readLen;
        }
        catch (Exception e) {
            throw new HdfsException(
                    "read file failed:filePath=" + filePath + ",offset=" + offset + ",len=" + len,
                    e);
        }
    }

    @Override
    @SlowLog(operation = "seekData")
    public void seek(long size) throws HdfsException {
        try {
            inputStream.seek(size);
        }
        catch (Exception e) {
            throw new HdfsException(
                    "read file failed:filePath=" + filePath + ",size=" + size, e);
        }
    }

    @Override
    public boolean isEof() {
        return isEof;
    }

    @Override
    public long getSize() {
        return size;
    }

    private void closeFSstream() {

        if (null != inputStream) {
            try {
                inputStream.close();
                inputStream = null;
            }
            catch (Exception e) {
                logger.warn("close FSDataOutputStream failed:filePath=" + filePath, e);
            }
        }
    }

    private void closeFS() {
        try {
            if (null != fileSystem) {
                fileSystem.close();
            }
        }
        catch (Exception e) {
            logger.warn("close fileSystem failed:filePath=" + filePath, e);
        }
    }

    private void releaseReource() {
        closeFSstream();
        closeFS();
        dataService = null;
    }
}
