package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.checksum.ChecksumFactory;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.util.Date;
import java.util.zip.Checksum;

// 这个对象用于修正断点文件的元数据，当断点文件同时满足如下条件时，可以调用本对象来进行尝试修正：
// 1. 断点文件元数据显示断点文件尚未传输完毕
// 2. 底层存储的断点文件数据已不存在（ScmBreakpointDataWriter对象无法找到数据进行写入）
//
// 满足上述条件可以推测底层存储的断点文件数据已经调用过ScmBreakpointDataWriter.complete，不支持再用ScmBreakpointDataWriter打开写入
//
// BreakpointFileMetaCorrector
// 主要行为就是读取断点文件合并后的数据（使用ScmDataReader进行读取），如果能找到则表明上述推测成立，
// 本对象会根据合并后的数据修改断点文件元数据：状态（调整为已完成）、checksum、uploadsize、md5
// 推测不成立本对象会进行报错
public class BreakpointFileMetaCorrector {
    private final BreakpointFile file;
    private static final Logger logger = LoggerFactory.getLogger(BreakpointFileMetaCorrector.class);

    public BreakpointFileMetaCorrector(BreakpointFile file) {
        this.file = file;
    }

    public boolean canCorrect() throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(file.getWorkspaceName());
        ScmDataReader reader = null;
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(file.getSiteId(),
                    file.getWorkspaceName(), ws.getDataLocation(file.getWsVersion()), contentModule.getDataService(),
                    new ScmDataInfo(ENDataType.Normal.getValue(), file.getDataId(),
                            new Date(file.getCreateTime()), file.getWsVersion()));
            if (reader.getSize() < file.getUploadSize()) {
                logger.warn("breakpoint file data is corrupted: file={}", file);
                return false;
            }
            return true;
        }
        catch (Exception e) {
            logger.warn("failed to check breakpoint file data status: file={}", file, e);
            return false;
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public BreakpointFile correct() throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(file.getWorkspaceName());
        ScmDataReader reader = null;
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(file.getSiteId(),
                    file.getWorkspaceName(), ws.getDataLocation(file.getWsVersion()), contentModule.getDataService(),
                    new ScmDataInfo(ENDataType.Normal.getValue(), file.getDataId(),
                            new Date(file.getCreateTime()), file.getWsVersion()));
            if (reader.getSize() < file.getUploadSize()) {
                throw new ScmServerException(ScmError.DATA_CORRUPTED,
                        "breakpoint file data is corrupted: file=" + file);
            }
            logger.info("breakpoint file data is complete, try correct meta: file={}", file);
            if (file.isNeedMd5()) {
                correctChecksumAndMd5(file, reader);
            }
            else {
                correctChecksum(file, reader);
            }
            file.setUploadSize(reader.getSize());
            file.setCompleted(true);
            contentModule.getMetaService().updateBreakpointFile(file);
            logger.info(" correct breakpoint file meta success: file={}", file);
        }
        catch (ScmDatasourceException e) {
            if (e.getScmError(ScmError.DATA_READ_ERROR) == ScmError.DATA_NOT_EXIST) {
                throw new ScmServerException(ScmError.DATA_CORRUPTED,
                        "breakpoint file data loss: file=" + file, e);
            }
            throw new ScmServerException(ScmError.DATA_CORRUPTED,
                    "failed to check breakpoint file data status: file=" + file);
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }

        return file;
    }

    private void correctChecksum(BreakpointFile file, ScmDataReader reader)
            throws ScmServerException {
        byte[] buf = new byte[1024 * 64];
        try {
            // 使用断点文件元数据中的Checksum作为基础值
            Checksum checksum = ChecksumFactory.getChecksum(file.getChecksumType(),
                    file.getChecksum());
            // seek 到断点文件元数据之后的大小，开始增量计算Checksum
            reader.seek(file.getUploadSize());
            while (true) {
                int readLen = reader.read(buf, 0, buf.length);
                if (readLen <= -1) {
                    break;
                }
                checksum.update(buf, 0, readLen);
            }
            file.setChecksum(checksum.getValue());
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to get breakpoint data checksum: file=" + file, e);
        }
    }

    private void correctChecksumAndMd5(BreakpointFile file, ScmDataReader reader)
            throws ScmServerException {
        byte[] buf = new byte[1024 * 64];
        try {
            Checksum checksum = ChecksumFactory.getChecksum(file.getChecksumType(),
                    file.getChecksum());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            long totalReadLen = 0;
            while (true) {
                int readLen = reader.read(buf, 0, buf.length);
                if (readLen <= -1) {
                    break;
                }
                md5.update(buf, 0, readLen);

                long newTotalReadLen = totalReadLen + readLen;

                // checksum 只需要增量计算 file.getUploadSize() 以后的数据
                // 如下判断用于发现读取的数据是否超过 file.getUploadSize()，如果超过，则需要增量计算checksum
                if (newTotalReadLen > file.getUploadSize()) {
                    int offset = (int) (file.getUploadSize() - totalReadLen);
                    if (offset > 0) {
                        // buf 中的一部分数据需要参与计算 checksum
                        checksum.update(buf, offset, readLen - offset);
                    }
                    else {
                        // buf 中所有数据都需要参与计算 checksum
                        checksum.update(buf, 0, readLen);
                    }
                }
                // buf 中的数据不需要参与计算 checksum

                totalReadLen = newTotalReadLen;
            }
            file.setChecksum(checksum.getValue());
            byte[] md5Bytes = md5.digest();
            file.setMd5(DatatypeConverter.printBase64Binary(md5Bytes));
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to get breakpoint data checksum: file=" + file, e);
        }
    }

}
