package com.sequoiacm.contentserver.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.InputStreamWithCalcMd5;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class FileCreatorDao implements IFileCreatorDao {
    private static final Logger logger = LoggerFactory.getLogger(FileCreatorDao.class);
    private final boolean isNeedMd5;
    private int siteId;
    private String fileId;
    private int majorVersion;
    private int minorVersion;
    private BSONObject fileInfo;
    private ScmWorkspaceInfo wsInfo;
    private Date createDate;
    private ScmDataWriter fileWriter;
    private ScmDataInfo dataInfo;

    public FileCreatorDao(int siteId, ScmWorkspaceInfo wsInfo, BSONObject fileInfo,
            ScmDataInfo dataInfo, boolean isNeedMd5) throws ScmServerException {
        this.siteId = siteId;
        this.wsInfo = wsInfo;
        this.fileInfo = fileInfo;
        this.fileId = (String) fileInfo.get(FieldName.FIELD_CLFILE_ID);
        this.majorVersion = (Integer) fileInfo.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        this.minorVersion = (Integer) fileInfo.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        this.createDate = dataInfo.getCreateTime();
        this.dataInfo = dataInfo;
        this.isNeedMd5 = isNeedMd5;

        try {
            fileWriter = ScmDataOpFactoryAssit.getFactory().createWriter(
                    ScmContentServer.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e);
        }
    }

    private void write(byte[] content, int off, int len) throws ScmServerException {
        try {
            fileWriter.write(content, off, len);
        }
        catch (ScmDatasourceException e) {
            logger.error("write file failed:fileId={},dataId={}", getFileId(), getDataId());
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to write file data", e);
        }
    }

    public void writeData(InputStream is) throws ScmServerException {
        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        try {
            while (true) {
                int len = CommonHelper.readAsMuchAsPossible(is, buf);
                if (len <= -1) {
                    break;
                }

                this.write(buf, 0, len);

                if (len < buf.length) {
                    break;
                }
            }
            writeFinish();
            updateFileInfo();
            FileCommonOperator.recordDataTableName(wsInfo.getName(), fileWriter);
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.FILE_IO,
                    "write file failed:fileId=" + getFileId() + ",dataId=" + getDataId(), e);
        }
        finally {
            // GC may be more efficiently
            buf = null;
        }
    }

    public void write(InputStream is) throws ScmServerException {
        if (!isNeedMd5) {
            writeData(is);
            return;
        }
        InputStreamWithCalcMd5 md5Is = new InputStreamWithCalcMd5(is);
        writeData(md5Is);
        String md5 = md5Is.calcMd5();
        fileInfo.put(FieldName.FIELD_CLFILE_FILE_MD5, md5);
    }

    private void writeFinish() throws ScmServerException {
        FileCommonOperator.closeWriter(fileWriter);
    }

    public void rollback() {
        FileCommonOperator.cancelWriter(fileWriter);
        FileCommonOperator.recordDataTableName(wsInfo.getName(), fileWriter);
    }

    private void updateFileInfo() throws ScmServerException {
        fileInfo.put(FieldName.FIELD_CLFILE_FILE_SIZE, fileWriter.getSize());
        fileInfo.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE, fileWriter.getType());
    }

    @Override
    public BSONObject insert() throws ScmServerException {
        String parentId = (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        ScmMetaService metaservice = ScmContentServer.getInstance().getMetaService();
        BSONObject parentDirMatcher = new BasicBSONObject();
        parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, parentId);

        ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(wsInfo.getName(), parentId);
        ScmLock rLock = null;
        if (!parentId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            rLock = readLock(lockPath);
        }
        try {
            if (metaservice.getDirCount(wsInfo.getName(), parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exists:preantDirectoryId=" + parentId);
            }
            metaservice.insertFile(wsInfo, fileInfo);
        }
        finally {
            unlock(rLock, lockPath);
        }
        return fileInfo;
    }

    private void unlock(ScmLock lock, ScmLockPath lockPath) {
        try {
            if (lock != null) {
                lock.unlock();
            }
        }
        catch (Exception e) {
            logger.warn("failed to unlock:path={}", lockPath, e);
        }
    }

    private ScmLock readLock(ScmLockPath lockPath) throws ScmServerException {
        return ScmLockManager.getInstance().acquiresReadLock(lockPath);
    }

    @Override
    public void processException() {
        // delete lob
        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentServer.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
            deletor.delete();
        }
        catch (Exception e) {
            logger.warn("delete file data failed:siteId=" + siteId + ",wsName=" + wsInfo.getName()
                    + ",fileId=" + dataInfo.getId(), e);
        }
    }

    public String getFileId() {
        return fileId;
    }

    public String getDataId() {
        return dataInfo.getId();
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public long getSize() {
        return fileWriter.getSize();
    }

    public int getSiteId() {
        return siteId;
    }

    public Date getCreateTime() {
        return createDate;
    }

    public Date getUpdateTime() {
        return createDate;
    }

    @Override
    public String getWorkspaceName() {
        return wsInfo.getName();
    }

    public String getFileName() {
        return (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME);
    }
}
