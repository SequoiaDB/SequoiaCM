package com.sequoiacm.hdfs.dataoperation;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.hdfs.HdfsException;
import com.sequoiacm.hdfs.dataservice.HdfsDataService;

public class HdfsDataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(HdfsDataWriterImpl.class);

    private long offset = 0;
    private FileSystem fileSystem;
    private String filePath;
    private HdfsDataService dataService;
    private FSDataOutputStream outputStream = null;

    public HdfsDataWriterImpl(String wsName, HdfsDataLocation dataLocation, ScmService service,
            ScmDataInfo dataInfo) throws HdfsException {
        try {
            this.filePath = dataLocation.getFileDir(wsName, dataInfo.getCreateTime(),
                    dataInfo.getId());
            this.dataService = (HdfsDataService) service;
            this.fileSystem = dataService.getFileSystem();
            createFile();
        }
        catch (HdfsException e) {
            logger.error("construct HdfsFileContentWriter failed:filePath=" + this.filePath, e);
            releaseReource();
            throw e;
        }
        catch (Exception e) {
            logger.error("construct HdfsFileContentWriter failed:filePath=" + this.filePath, e);
            releaseReource();
            throw new HdfsException(
                    "construct HdfsFileContentWriter failed:filePath=" + this.filePath, e);
        }
    }

    private void createFile() throws HdfsException {
        Path file = new Path(filePath);
        if (dataService.isExist(fileSystem, file)) {
            throw new HdfsException(HdfsException.HDFS_ERROR_FILE_ALREADY_EXISTS,
                    "create FSDataOutputStream failed ,file data exist :filePath=" + this.filePath);
        }
        try {
            outputStream = fileSystem.create(file, false);
        }
        catch (FileAlreadyExistsException e) {
            throw new HdfsException(HdfsException.HDFS_ERROR_FILE_ALREADY_EXISTS,
                    "create FSDataOutputStream failed ,file data exist :filePath=" + this.filePath,
                    e);
        }
        catch (Exception e) {
            throw new HdfsException("create FSDataOutputStream failed:filePath=" + this.filePath,
                    e);
        }
    }

    @Override
    public void write(byte[] content) throws HdfsException {
        write(content, 0, content.length);
    }

    @Override
    public void write(byte[] content, int offset, int len) throws HdfsException {
        try {
            outputStream.write(content, offset, len);
            this.offset += len;
        }
        catch (Exception e) {
            throw new HdfsException("write file failed:filePath=" + this.filePath, e);
        }
    }

    @Override
    public void cancel() {
        try {
            if (null != outputStream) {
                outputStream.close();
                outputStream = null;
            }
            dataService.delete(fileSystem, new Path(filePath));

        }
        catch (Exception e) {
            logger.warn("cancel failed:filePath=" + this.filePath, e);
        }
        releaseReource();
    }

    @Override
    public void close() throws HdfsException {
        releaseReource();
    }

    @Override
    public long getSize() {
        return offset;
    }

    private void closeFSstream() {

        if (null != outputStream) {
            try {
                outputStream.close();
                outputStream = null;
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

    @Override
    public String getCreatedTableName() {
        // no need record path, we have record the path when create workspace.
        return null;
    }
}
