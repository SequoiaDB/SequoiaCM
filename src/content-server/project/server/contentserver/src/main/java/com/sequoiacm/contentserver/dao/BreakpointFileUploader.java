package com.sequoiacm.contentserver.dao;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.Checksum;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.sequoiacm.common.checksum.ChecksumException;
import com.sequoiacm.common.checksum.ChecksumFactory;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;

public class BreakpointFileUploader {
    private static final Logger logger = LoggerFactory.getLogger(BreakpointFileUploader.class);

    private static final int ONCE_WRITE_BYTES = 1024 * 1024; // 1MB
    private static final int FLUSH_WRITE_BYTES = 1024 * 1024 * 50; // 50MB
    private ScmWorkspaceInfo workspaceInfo;
    private BreakpointFile file;
    private String uploadUser;
    private ScmBreakpointDataWriter dataWriter;
    private boolean dirty = false;
    private Checksum checksum;

    public BreakpointFileUploader(String uploadUser, ScmWorkspaceInfo wsInfo, BreakpointFile file)
            throws ScmServerException {
        this.uploadUser = uploadUser;
        this.workspaceInfo = wsInfo;
        this.file = file;

        file.setUploadUser(uploadUser);

        try {
            if (file.getUploadSize() > 0) {
                this.checksum = ChecksumFactory.getChecksum(file.getChecksumType(),
                        file.getChecksum());
            }
            else {
                this.checksum = ChecksumFactory.getChecksum(file.getChecksumType());
            }
        }
        catch (ChecksumException e) {
            throw new ScmSystemException("Failed to get checksum object", e);
        }

        boolean createData = false;
        if (!StringUtils.hasText(file.getDataId())) {
            file.setDataId(ScmIdGenerator.FileId.get(new Date(file.getCreateTime())));
            createData = true;
            dirty = true;
        }

        try {
            dataWriter = ScmDataOpFactoryAssit.getFactory().createBreakpointWriter(
                    workspaceInfo.getDataLocation(),
                    ScmContentServer.getInstance().getDataService(), file.getWorkspaceName(),
                    file.getFileName(), file.getDataId(), new Date(file.getCreateTime()),
                    createData);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_BREAKPOINT_WRITE_ERROR),
                    "Failed to create breakpoint data writer", e);
        }

        if (!createData) {
            try {
                dataWriter.truncate(file.getUploadSize());
            }
            catch (ScmDatasourceException e) {
                try {
                    closeDataWriter();
                }
                catch (ScmServerException ex) {
                    logger.warn("Failed to close ScmBreakpointDataWriter", ex);
                }
                throw new ScmServerException(e.getScmError(ScmError.DATA_BREAKPOINT_WRITE_ERROR),
                        "Failed to truncate breakpoint file", e);
            }
        }
    }

    public void write(InputStream stream, boolean isLastContent) throws ScmServerException {
        if (file.isCompleted()) {
            throw new ScmServerException(ScmError.FILE_IO,
                    String.format("BreakpointFile is completed: /%s/%s", file.getWorkspaceName(),
                            file.getFileName()));
        }

        try {
            innerWrite(stream, isLastContent);
        }
        catch (ScmServerException e) {
            dirty = false;
            closeDataWriter();
            throw e;
        }
        catch (ScmDatasourceException e) {
            dirty = false;
            closeDataWriter();
            throw new ScmServerException(e.getScmError(ScmError.DATA_BREAKPOINT_WRITE_ERROR),
                    "Failed to write breakpoint file data", e);
        }
    }

    private void innerWrite(InputStream stream, boolean isLastContent)
            throws ScmServerException, ScmDatasourceException {
        long dataOffset = file.getUploadSize();
        long writeSize = 0;
        long flushSize = 0;
        byte[] buffer = new byte[ONCE_WRITE_BYTES];

        int size = read(stream, buffer);

        while (size > 0) {
            dataWriter.write(dataOffset, buffer, 0, size);

            dataOffset += size;
            writeSize += size;
            flushSize += size;
            checksum.update(buffer, 0, size);

            // read next bytes to see if data remain
            size = read(stream, buffer);

            // write meta data if write enough bytes,
            // or no data remain
            if (flushSize >= FLUSH_WRITE_BYTES || size <= -1) {
                if (size <= -1 && isLastContent) {
                    dataWriter.close();
                    dataWriter = null;
                    file.setCompleted(true);
                    if (file.isNeedMd5()) {
                        ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(),
                                file.getDataId(), new Date(file.getCreateTime()));
                        String md5 = ScmSystemUtils.calcMd5(workspaceInfo, dataInfo);
                        file.setMd5(md5);
                    }
                }
                else {
                    dataWriter.flush();
                }
                file.setUploadTime(System.currentTimeMillis());
                file.setUploadSize(dataOffset);
                file.setChecksum(checksum.getValue());
                dirty = true;
                ScmContentServer.getInstance().getMetaService().updateBreakpointFile(file);
                dirty = false;
                flushSize = 0;
            }
        }

        // no data write, only update file.completed
        if (writeSize == 0 && isLastContent) {
            dataWriter.close();
            file.setCompleted(true);
            file.setUploadTime(System.currentTimeMillis());
            if (file.isNeedMd5()) {
                ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(),
                        file.getDataId(), new Date(file.getCreateTime()));
                String md5 = ScmSystemUtils.calcMd5(workspaceInfo, dataInfo);
                file.setMd5(md5);
            }
            dirty = true;
            ScmContentServer.getInstance().getMetaService().updateBreakpointFile(file);
            dirty = false;
        }
    }

    private int read(InputStream stream, byte[] buffer) throws ScmServerException {
        try {
            return stream.read(buffer);
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.FILE_IO,
                    String.format("Failed to read breakpoint data: /%s/%s", file.getWorkspaceName(),
                            file.getFileName()),
                    e);
        }
    }

    public BreakpointFile done() throws ScmServerException {
        closeDataWriter();

        if (dirty) {
            ScmContentServer.getInstance().getMetaService().updateBreakpointFile(file);
        }

        return file;
    }

    private void closeDataWriter() throws ScmServerException {
        if (dataWriter != null) {
            try {
                dataWriter.close();
            }
            catch (ScmDatasourceException e) {
                throw new ScmServerException(e.getScmError(ScmError.DATA_BREAKPOINT_WRITE_ERROR),
                        "Failed to close breakpoint data writer", e);
            }

            FileCommonOperator.recordDataTableName(workspaceInfo.getName(), dataWriter);

            dataWriter = null;
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update("a".getBytes());
        byte[] b = md5.digest();
        System.out.println(Arrays.toString(b));

    }
}
