package com.sequoiacm.s3.processor.impl;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.OutStreamFlushQueue;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.infrastructure.lock.exception.ScmLockTimeoutException;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.FileMappingUtil;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.*;
import com.sequoiacm.s3.dao.PartDao;
import com.sequoiacm.s3.dao.UploadDao;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.lock.S3LockPathFactory;
import com.sequoiacm.s3.model.CompleteMultipartUploadResult;
import com.sequoiacm.s3.model.CompletePart;
import com.sequoiacm.s3.model.InputStreamWithCalc;
import com.sequoiacm.s3.processor.MultipartUploadProcessor;
import com.sequoiacm.s3.transactioncallback.TransactionCallbackUpload;
import com.sequoiacm.s3.utils.MD5Utils;
import org.apache.commons.codec.binary.Hex;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class MultipartUploadProcessorSeekable implements MultipartUploadProcessor {
    private static final Logger logger = LoggerFactory
            .getLogger(MultipartUploadProcessorSeekable.class);

    private static final int ONCE_WRITE_BYTES = 256 * 1024; // 256K
    private static final int DEFAULT_DATA_TYPE = 1;
    private static String emptyEtag = null;

    @Autowired
    IDatasourceService datasourceService;

    @Autowired
    MetaSourceService metaSourceService;

    @Autowired
    private IScmBucketService scmBucketService;

    @Autowired
    PartDao partDao;

    @Autowired
    UploadDao uploadDao;

    @Autowired
    ScmLockManager lockManger;

    @Autowired
    S3LockPathFactory lockPathFactory;

    @Autowired
    OutStreamFlushQueue outStreamFlushQueue;

    @Override
    public void initMultipartUpload(String wsName, long uploadId, UploadMeta meta)
            throws S3ServerException, ScmMetasourceException, ScmServerException {
        MetaSource ms = metaSourceService.getMetaSource();
        DataInfo dataInfo1 = null;
        DataInfo dataInfo2 = null;

        TransactionContext transaction = ms.createTransactionContext();
        try {
            transaction.begin();
            uploadDao.insertUploadMeta(transaction, meta);

            dataInfo1 = createData(wsName, meta.getWsVersion());
            dataInfo2 = createData(wsName, meta.getWsVersion());

            Part nPart1 = new Part(uploadId, 0, dataInfo1.getDataId(), dataInfo1.getCreateTime(), 0,
                    null);
            partDao.insertPart(transaction, nPart1);
            Part nPart2 = new Part(uploadId, -1, dataInfo2.getDataId(), dataInfo2.getCreateTime(),
                    0, null);
            partDao.insertPart(transaction, nPart2);
            transaction.commit();
        }
        catch (Exception e) {
            transaction.rollback();
            if (dataInfo1 != null) {
                deleteData(wsName, dataInfo1.getDataId(), dataInfo1.getCreateTime(), meta.getWsVersion());
            }
            if (dataInfo2 != null) {
                deleteData(wsName, dataInfo2.getDataId(), dataInfo2.getCreateTime(), meta.getWsVersion());
            }
            throw new S3ServerException(S3Error.PART_INIT_MULTIPART_UPLOAD_FAILED,
                    "init upload failed", e);
        }
        finally {
            transaction.close();
        }
    }

    @Override
    public Part uploadPart(String wsName, long uploadId, int partNumber, String contentMD5,
            InputStream inputStream, long contentLength, int wsVersion)
            throws S3ServerException, ScmLockException, ScmDatasourceException, IOException,
            NoSuchAlgorithmException, ScmServerException, ScmMetasourceException {
        PreparedData preparedData = null;
        ScmLock lockPart = null;

        try {
            tryEnableReservedParts(uploadId, contentLength);
            ScmLockPath lockPathPart = lockPathFactory.createPartLockPath(uploadId, partNumber);
            lockPart = lockManger.acquiresWriteLock(lockPathPart, 60 * 1000);
            preparedData = prepareStoreDataForPart(wsName, uploadId, partNumber, contentLength,
                    wsVersion);

            String eTag;
            if (contentLength > 0) {
                InputStreamWithCalc is = null;
                ScmSeekableDataWriter writer = null;
                try {
                    is = new InputStreamWithCalc(inputStream);
                    writer = datasourceService.getScmSeekableDataWriter(wsName,
                            preparedData.getDestDataInfo().getDataId(), DEFAULT_DATA_TYPE,
                            preparedData.getDestDataInfo().getCreateTime(), wsVersion);
                    writePartData(writer, (partNumber - 1) * contentLength, is);
                }
                catch (ScmDatasourceException e) {
                    if (e.getScmError(ScmError.DATA_WRITE_ERROR)
                            .getErrorCode() == ScmError.DATA_PIECES_INFO_OVERFLOW.getErrorCode()) {
                        createNewReservedPart(wsName, uploadId, contentLength, wsVersion);
                    }
                    logger.error("write data failed. partnumber=" + partNumber + ", length="
                            + contentLength);
                    throw e;
                }
                catch (Exception e) {
                    logger.error("write data failed. partnumber=" + partNumber + ", length="
                            + contentLength);
                    throw e;
                }
                finally {
                    if (writer != null) {
                        writer.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                }

                checkPartContent(is, contentMD5, contentLength);
                eTag = is.gethexmd5();
            }
            else {
                // 长度为0时，获得一个无内容的etag
                eTag = getEmptyEtag();
            }

            Part newPart = new Part(uploadId, partNumber,
                    preparedData.getDestDataInfo().getDataId(),
                    preparedData.getDestDataInfo().getCreateTime(), contentLength, eTag);

            MetaSource ms = metaSourceService.getMetaSource();
            TransactionContext transaction = ms.createTransactionContext();
            try {
                transaction.begin();
                if (null == preparedData.getOldPart()) {
                    partDao.insertPart(transaction, newPart);
                }
                else {
                    partDao.updatePart(transaction, newPart);
                    abandonOldPart(transaction, uploadId, preparedData.getOldPart());
                }
                transaction.commit();
            }
            catch (Exception e) {
                transaction.rollback();
                throw e;
            }
            finally {
                transaction.close();
            }

            return newPart;
        }
        catch (ScmLockTimeoutException e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_CONFLICT, "The part is busy", e);
        }
        catch (Exception e) {
            if (preparedData != null && preparedData.getCreateNewData()
                    && preparedData.getDestDataInfo() != null) {
                deleteData(wsName, preparedData.getDestDataInfo().getDataId(),
                        preparedData.getDestDataInfo().getCreateTime(), wsVersion);
            }
            throw e;
        }
        finally {
            if (lockPart != null) {
                lockPart.unlock();
            }
        }
    }

    @Override
    public CompleteMultipartUploadResult completeUpload(String wsName, ScmSession session,
            String bucketName, UploadMeta upload, List<CompletePart> reqPartList,
            ServletOutputStream outputStream) throws S3ServerException, ScmServerException,
            ScmMetasourceException, ScmDatasourceException {
        List<Part> allPartArray = new ArrayList<>();

        // 获取本地的part列表(不包括reserved part和废弃的part)
        getLocalPartList(upload.getUploadId(), allPartArray);

        // 检查并生成待合并的分段列表
        List<Part> completeList = computeComplete(reqPartList, allPartArray);

        // 合并策略选择
        CopyAction copyAction = generateAction(completeList, allPartArray);
        String destDataId;
        long dataCreateTime;
        if (copyAction.getBaseDataId() != null) {
            destDataId = copyAction.getBaseDataId();
            dataCreateTime = copyAction.getBaseDataCreateTime();
        }
        else {
            // create a new data
            DataInfo dataInfo = createData(wsName, upload.getWsVersion());
            destDataId = dataInfo.getDataId();
            dataCreateTime = dataInfo.getCreateTime();
        }

        ScmSeekableDataWriter writer = null;
        try {
            writer = datasourceService.getScmSeekableDataWriter(wsName, destDataId,
                    DEFAULT_DATA_TYPE, dataCreateTime, upload.getWsVersion());
            for (int i = 0; i < copyAction.getCopyList().size(); i++) {
                CopyPartInfo copyPart = copyAction.getCopyList().get(i);

                if (copyPart.getPartSize() > 0) {
                    ScmDataReader dataReader = datasourceService.getScmDataReader(wsName,
                            copyPart.getSrcDataID(), DEFAULT_DATA_TYPE,
                            copyPart.getSrcDataCreateTime(), upload.getWsVersion());
                    try {
                        copyObjectData(writer, copyPart.getDestOffset(), dataReader,
                                (copyPart.getPartNumber() - 1) * copyPart.getPartSize(),
                                copyPart.getPartSize());
                    }
                    finally {
                        dataReader.close();
                    }
                }
            }
            writer.truncate(copyAction.getCompleteSize());
            upload.setDataId(destDataId);
            upload.setUploadStatus(S3CommonDefine.UploadStatus.UPLOAD_COMPLETE);
            upload.setLastModified(System.currentTimeMillis());
            TransactionCallbackUpload callbackUpload = new TransactionCallbackUpload(upload,
                    uploadDao);

            String eTag = completeList.get(0).getEtag();
            String completeEtag;
            if (reqPartList.size() == 1) {
                completeEtag = trimQuotes(eTag);
            }
            else {
                completeEtag = trimQuotes(eTag) + "-f";
            }
            CompleteMultipartUploadResult response = new CompleteMultipartUploadResult();
            response.seteTag(completeEtag);

            // update uploadMeta and write object meta
            BSONObject file = buildFileInfoFromUpload(upload);
            FileMeta fileMeta = FileMeta.fromUser(upload.getWsName(), file,
                    session.getUser().getUsername());
            fileMeta.resetDataInfo(destDataId, dataCreateTime, DEFAULT_DATA_TYPE,
                    copyAction.getCompleteSize(), null, upload.getSiteId(), upload.getWsVersion());
            fileMeta.setEtag(completeEtag);

            FileMeta newFileMeta = scmBucketService.createFile(session.getUser(), bucketName,
                    fileMeta, callbackUpload, false);
            S3ObjectMeta objMeta = FileMappingUtil.buildS3ObjectMeta(bucketName,
                    newFileMeta.toBSONObject());
            response.setVersionId(objMeta.getVersionId());
            return response;
        }
        catch (Exception e) {
            if (copyAction.getBaseDataId() == null) {
                deleteData(wsName, destDataId, dataCreateTime, upload.getWsVersion());
            }
            throw new S3ServerException(S3Error.PART_COMPLETE_MULTIPART_UPLOAD_FAILED,
                    "complete upload failed. bucket:" + bucketName + ", uploadId:"
                            + upload.getUploadId(),
                    e);
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public void cleanInvalidUpload(String wsName, MetaSource ms, UploadMeta uploadMeta)
            throws S3ServerException, ScmMetasourceException, ScmServerException {
        MetaCursor partCursor = partDao.queryPartList(uploadMeta.getUploadId(), null, null, null);
        try {
            if (uploadMeta.getUploadStatus() == S3CommonDefine.UploadStatus.UPLOAD_COMPLETE) {
                while (partCursor.hasNext()) {
                    Part part = new Part(partCursor.getNext());
                    if (null == part.getDataId()) {
                        continue;
                    }
                    if (uploadMeta.getDataId().equals(part.getDataId())) {
                        continue;
                    }

                    deleteDataIgnoreNotExist(wsName,
                            new ScmDataInfo(DEFAULT_DATA_TYPE, part.getDataId(),
                                    new Date(part.getDataCreateTime()), uploadMeta.getWsVersion()));
                }
            }
            else {
                while (partCursor.hasNext()) {
                    Part part = new Part(partCursor.getNext());
                    if (null == part.getDataId()) {
                        continue;
                    }

                    deleteDataIgnoreNotExist(wsName,
                            new ScmDataInfo(DEFAULT_DATA_TYPE, part.getDataId(),
                                    new Date(part.getDataCreateTime()), uploadMeta.getWsVersion()));
                }
            }
        }
        finally {
            partCursor.close();
        }
        partDao.deletePart(null, uploadMeta.getUploadId(), null);
        uploadDao.deleteUploadByUploadId(null, uploadMeta.getBucketId(), uploadMeta.getKey(),
                uploadMeta.getUploadId());
    }

    private void deleteDataIgnoreNotExist(String wsName, ScmDataInfo dataInfo) {
        try {
            datasourceService.deleteDataLocal(wsName, dataInfo);
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.DATA_NOT_EXIST) {
                logger.error("delete data failed. wsName:{}, dataInfo:{}", wsName,
                        dataInfo.toString(), e);
            }
        }
    }

    private void tryEnableReservedParts(long uploadId, long contentLength) {
        int partNumber = 0;
        try {
            // 初始化时建了两个reserved part，part:0和part:-1，尝试启动一个
            if (!enableReservedPart(uploadId, partNumber, contentLength)) {
                partNumber--;
                enableReservedPart(uploadId, partNumber, contentLength);
            }
        }
        catch (Exception e) {
            logger.warn("tryEnableReservedPart failed, uploadId:" + uploadId + ", partNumber:"
                    + partNumber, e);
        }
    }

    private Boolean enableReservedPart(long uploadId, int partNumber, long contentLength)
            throws ScmServerException, ScmLockTimeoutException, ScmLockException, S3ServerException,
            ScmMetasourceException {
        ScmLockPath lockPath = lockPathFactory.createPartLockPath(uploadId, partNumber);
        ScmLock lock = lockManger.acquiresWriteLock(lockPath, 60 * 1000);
        try {
            Part reservedPart = partDao.queryPart(uploadId, partNumber);
            if (null == reservedPart) {
                throw new S3ServerException(S3Error.INTERNAL_ERROR,
                        "part " + partNumber + " does not exist");
            }
            if (reservedPart.getSize() == contentLength) {
                return true;
            }
            if (reservedPart.getSize() == 0) {
                reservedPart.setSize(contentLength);
                partDao.updatePart(null, reservedPart);
                return true;
            }
        }
        finally {
            lock.unlock();
        }
        return false;
    }

    private PreparedData prepareStoreDataForPart(String wsName, long uploadId, int partNumber,
            long contentLength, int wsVersion)
            throws S3ServerException, ScmServerException, ScmMetasourceException {
        Boolean createNewData = false;
        PreparedData preparedData = new PreparedData();
        DataInfo dataInfo = null;

        Part oldPart = partDao.queryPart(uploadId, partNumber);
        if (oldPart != null) {
            if (contentLength == oldPart.getSize()) {
                // 如果与oldpart写入相同的位置，写入异常时无法回退，会导致旧的分块和新的分块都无法使用，
                // 因此需要新建一个data写入新分块
                createNewData = true;
            }
            else {
                Part sameSizePart = partDao.queryOnePart(uploadId, contentLength,
                        S3CommonDefine.PartNumberRange.RESERVED_PART_NUM_BEGIN, null);
                if (sameSizePart != null) {
                    dataInfo = new DataInfo(sameSizePart.getDataId(),
                            sameSizePart.getDataCreateTime());
                }
                else {
                    createNewData = true;
                }
            }
        }
        else {
            Part sameSizePart = partDao.queryOnePart(uploadId, contentLength,
                    S3CommonDefine.PartNumberRange.RESERVED_PART_NUM_BEGIN, null);
            if (sameSizePart != null) {
                dataInfo = new DataInfo(sameSizePart.getDataId(), sameSizePart.getDataCreateTime());
            }
            else {
                createNewData = true;
            }
        }

        if (createNewData) {
            dataInfo = createData(wsName, wsVersion);
        }

        preparedData.setDestDataInfo(dataInfo);
        preparedData.setOldPart(oldPart);
        preparedData.setCreateNewData(createNewData);
        return preparedData;
    }

    private void createNewReservedPart(String wsName, long uploadId, long size, int wsVersion)
            throws ScmServerException {
        DataInfo dataInfo = createData(wsName, wsVersion);
        int partNumber = -2;
        try {
            Part partReserved = partDao.queryOnePart(uploadId, null,
                    S3CommonDefine.PartNumberRange.RESERVED_PART_NUM_BEGIN,
                    S3CommonDefine.PartNumberRange.VALID_PART_NUM_BEGIN);
            if (partReserved != null && partReserved.getPartNumber() < -1) {
                partNumber = partReserved.getPartNumber() - 1;
            }

            if (partNumber < S3CommonDefine.PartNumberRange.RESERVED_PART_NUM_BEGIN) {
                throw new S3ServerException(S3Error.INTERNAL_ERROR,
                        "The reserved parts has been used up. partNumber=" + partNumber);
            }

            Part nPart = new Part(uploadId, partNumber, dataInfo.getDataId(),
                    dataInfo.getCreateTime(), size, null);
            partDao.insertPart(null, nPart);
        }
        catch (Exception e) {
            logger.warn("create new reserved part fail. uploadId:" + uploadId
                    + ", reserved partNumber:" + partNumber, e);
            deleteData(wsName, dataInfo.getDataId(), dataInfo.getCreateTime(), wsVersion);
        }
    }

    private void writePartData(ScmSeekableDataWriter dataWriter, long offset,
            InputStreamWithCalc is) throws IOException, ScmDatasourceException {
        try {
            dataWriter.seek(offset);
            byte[] buffer = new byte[ONCE_WRITE_BYTES];

            int size = 0;
            while (true) {
                if ((size = CommonHelper.readAsMuchAsPossible(is, buffer, 0, buffer.length)) > 0) {
                    // write lob
                    dataWriter.write(buffer, 0, size);
                }
                else {
                    break;
                }
            }

        }
        catch (Exception e) {
            logger.error("write part data failed. offset=" + offset);
            throw e;
        }
    }

    private void copyObjectData(ScmSeekableDataWriter dataWriter, long writeOffset,
            ScmDataReader dataReader, long readOffset, long readLength)
            throws ScmDatasourceException {
        try {
            long curOffset = 0L;
            dataWriter.seek(writeOffset);
            dataReader.seek(readOffset);

            byte[] buffer = new byte[ONCE_WRITE_BYTES];
            int size = dataReader.read(buffer, 0, (int) Math.min(readLength, ONCE_WRITE_BYTES));
            while (size > 0) {
                dataWriter.write(buffer, 0, size);

                curOffset += size;
                if (curOffset < readLength) {
                    size = dataReader.read(buffer, 0,
                            (int) Math.min(readLength - curOffset, ONCE_WRITE_BYTES));
                }
                else {
                    break;
                }
            }
        }
        catch (Exception e) {
            throw e;
        }
    }

    private void getLocalPartList(long uploadId, List<Part> partArray)
            throws S3ServerException, ScmMetasourceException {
        MetaCursor partCursor = partDao.queryPartList(uploadId, 1, null, null);
        try {
            if (!partCursor.hasNext()) {
                throw new S3ServerException(S3Error.PART_INVALID_PART,
                        "does not found any uploaded part. uploadId:" + uploadId);
            }

            while (partCursor.hasNext()) {
                Part part = new Part(partCursor.getNext());
                partArray.add(part);
            }
        }
        finally {
            partCursor.close();
        }
    }

    private String trimQuotes(String str) {
        if (str == null) {
            return null;
        }

        str = str.trim();
        if (str.startsWith("\"")) {
            str = str.substring(1);
        }

        if (str.endsWith("\"")) {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    private List<Part> computeComplete(List<CompletePart> reqList, List<Part> allList)
            throws S3ServerException {
        List<Part> completeList = new ArrayList<>();
        int allIndex = 0;
        int lastPartNumber = 0;

        for (int i = 0; i < reqList.size(); i++) {
            CompletePart reqPart = reqList.get(i);
            int reqPartNum = reqPart.getPartNumber();

            // ascending order 1,2,3,5,...
            if (reqPartNum <= lastPartNumber) {
                throw new S3ServerException(S3Error.PART_INVALID_PARTORDER, "The partNumber is "
                        + reqPartNum + ", lastPartNumber is " + lastPartNumber);
            }

            Part existPart = null;
            while (allIndex < allList.size()) {
                existPart = allList.get(allIndex);

                if (existPart.getPartNumber() > reqPartNum) {
                    // request part is not exist
                    throw new S3ServerException(S3Error.PART_INVALID_PART,
                            "partNumber " + reqPartNum + " is not exist.");
                }

                if (existPart.getPartNumber() < reqPartNum) {
                    // continue to find next existPart
                    ++allIndex;
                    existPart = null;
                    continue;
                }

                // etag match check
                String reqEtag = trimQuotes(reqPart.getEtag());
                if (null == reqEtag || !reqEtag.equals(existPart.getEtag())) {
                    throw new S3ServerException(S3Error.PART_INVALID_PART,
                            "the tag not matched. partNumber:" + reqPartNum + " . reqEtag:"
                                    + reqPart.getEtag() + ", locEtag:" + existPart.getEtag());
                }

                break;
            }

            if (existPart == null) {
                throw new S3ServerException(S3Error.PART_INVALID_PART,
                        "partNumber " + reqPartNum + " is not exist.");
            }

            if (i < reqList.size() - 1) {
                if (existPart.getSize() < 5 * 1024 * 1024L) {
                    throw new S3ServerException(S3Error.PART_ENTITY_TOO_SMALL,
                            "part size is invalid. size:" + existPart.getSize());
                }

                if (existPart.getSize() > 5 * 1024 * 1024 * 1024L) {
                    throw new S3ServerException(S3Error.PART_ENTITY_TOO_LARGE,
                            "part size is invalid. size:" + existPart.getSize());
                }
            }

            lastPartNumber = reqPartNum;
            completeList.add(existPart);
        }

        return completeList;
    }

    // 复用lob的原则
    // 1.必须从 offset = 0 开始
    // 2.合并分块的数据必须能够连续，无空洞
    // 为防止合并异常后，baseData 中的已有 Part 数据被破坏
    // 3.不允许覆盖 baseData 已有的 Part 数据
    // 4.baseData 在合并的最终长度之后不允许存在 Part 数据（合并后会做 truncate）
    // 违反上面4点的，统一创建一个全新lob，做数据全拷贝
    private CopyAction generateAction(List<Part> completeList, List<Part> allList) {
        List<Part> baseList = new ArrayList<>();

        // 必须从 offset = 0 开始
        if (completeList.get(0).getPartNumber() != 1) {
            return generateAllCopyAction(completeList);
        }

        Part basePart = completeList.get(0);
        if (basePart.getSize() == 0) {
            return generateAllCopyAction(completeList);
        }

        String baseDataID = basePart.getDataId();
        for (int i = 0; i < allList.size(); i++) {
            if (allList.get(i).getDataId().equals(baseDataID)) {
                baseList.add(allList.get(i));
            }
        }

        long baseOffset = basePart.getSize();
        long baseSize = basePart.getSize();
        CopyAction copyAction = new CopyAction();
        copyAction.setBaseDataId(baseDataID);
        copyAction.setBaseDataCreateTime(basePart.getDataCreateTime());

        for (int i = 1; i < completeList.size(); i++) {
            Part part = completeList.get(i);
            if (part.getDataId().equals(baseDataID)) {
                long partStart = (part.getPartNumber() - 1) * part.getSize();
                if (partStart != baseOffset) {
                    // 合并分块的数据必须能够连续，无空洞
                    return generateAllCopyAction(completeList);
                }
                baseOffset += part.getSize();
                continue;
            }

            // 不允许覆盖已有的数据
            if (!conflictWithBaseList(baseList, baseSize, baseOffset, part.getSize())) {
                copyAction.addCopyInfo(part.getPartNumber(), part.getDataId(),
                        part.getDataCreateTime(), baseOffset, part.getSize());
                baseOffset += part.getSize();
            }
            else {
                // data is conflict, must copy all parts.
                return generateAllCopyAction(completeList);
            }
        }

        // 不允许在合并的最终长度之后 baseData 内仍有有效的 Part 数据（合并后会做 truncate）
        if (redundantBaseList(baseList, baseSize, baseOffset)) {
            return generateAllCopyAction(completeList);
        }

        copyAction.setCompleteSize(baseOffset);
        return copyAction;
    }

    private CopyAction generateAllCopyAction(List<Part> completeList) {
        CopyAction copyAction = new CopyAction();

        long offset = 0;
        for (int i = 0; i < completeList.size(); i++) {
            // all parts need copy
            Part part = completeList.get(i);
            copyAction.addCopyInfo(part.getPartNumber(), part.getDataId(), part.getDataCreateTime(),
                    offset, part.getSize());
            offset += part.getSize();
        }
        copyAction.setCompleteSize(offset);
        return copyAction;
    }

    private boolean conflictWithBaseList(List<Part> baseList, long baseSize, long offset,
            long size) {
        int firstNum = (int) (offset / baseSize) + 1;
        int lastNum = (int) ((offset + size) / baseSize) + 1;

        for (int i = 0; i < baseList.size(); i++) {
            Part part = baseList.get(i);
            if (part.getPartNumber() > lastNum) {
                return false;
            }

            // [firstNum, lastNum] baseList 中的 part 在此区间内就是冲突
            if (part.getPartNumber() >= firstNum && part.getPartNumber() <= lastNum) {
                return true;
            }
        }

        return false;
    }

    private boolean redundantBaseList(List<Part> baseList, long baseSize, long offset) {
        int startNum = (int) (offset / baseSize) + 1;
        Part part = baseList.get(baseList.size() - 1);
        if (part.getPartNumber() >= startNum) {
            return true;
        }

        return false;
    }

    private void deleteData(String wsName, String dataId, long dataCreateTime, int wsVersion) {
        try {
            datasourceService.deleteDataLocal(wsName, new ScmDataInfo(DEFAULT_DATA_TYPE, dataId, new Date(dataCreateTime), wsVersion) );
        }
        catch (Exception e) {
            logger.error("delete data failed. wsName:{}, dataId:{}, createTime：{}", wsName, dataId,
                    dataCreateTime, e);
        }
    }

    private DataInfo createData(String wsName, int wsVersion) throws ScmServerException {
        Date createTime = new Date();
        String dataId = ScmIdGenerator.FileId.get(createTime);
        datasourceService.createDataInLocal(wsName,
                new ScmDataInfo(DEFAULT_DATA_TYPE, dataId, createTime, wsVersion));
        return new DataInfo(dataId, createTime.getTime());
    }

    private String getEmptyEtag() throws NoSuchAlgorithmException {
        if (null == emptyEtag) {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            emptyEtag = new String(Hex.encodeHex(MD5.digest()));
        }
        return emptyEtag;
    }

    private void abandonOldPart(TransactionContext transaction, long uploadId, Part oldPart)
            throws S3ServerException, ScmLockTimeoutException, ScmLockException {
        if (oldPart != null && oldPart.getDataId() != null) {
            int tryTime = 10000;
            int abandonedPartnumber = -1001; // [() - (-1001)) 范围存储的是废弃的part
            Part abandonedPart = partDao.queryOnePart(uploadId, null, null,
                    S3CommonDefine.PartNumberRange.RESERVED_PART_NUM_BEGIN);
            if (abandonedPart != null) {
                abandonedPartnumber = abandonedPart.getPartNumber() - 1;
            }
            oldPart.setPartNumber(abandonedPartnumber);
            while (tryTime > 0) {
                ScmLockPath lockPathPart = lockPathFactory.createPartLockPath(uploadId,
                        abandonedPartnumber);
                ScmLock lockPart = lockManger.acquiresWriteLock(lockPathPart, 60 * 1000);
                try {
                    if (null == partDao.queryPart(uploadId, oldPart.getPartNumber())) {
                        partDao.insertPart(transaction, oldPart);
                        break;
                    }
                    else {
                        oldPart.setPartNumber(
                                oldPart.getPartNumber() - RestParamDefine.PART_NUMBER_MAX);
                        tryTime--;
                    }
                }
                finally {
                    lockPart.unlock();
                }
            }
        }
    }

    private void checkPartContent(InputStreamWithCalc is, String contentMD5, long contentLength)
            throws S3ServerException {
        if (null != contentMD5 && !MD5Utils.isMd5EqualWithETag(contentMD5, is.gethexmd5())) {
            throw new S3ServerException(S3Error.OBJECT_BAD_DIGEST,
                    "The Content-MD5 you specified does not match what we received.");
        }
        if (contentLength != is.getLength()) {
            throw new S3ServerException(S3Error.OBJECT_INCOMPLETE_BODY, "content length is "
                    + contentLength + " and receive " + is.getLength() + " bytes");
        }
    }

    private BSONObject buildFileInfoFromUpload(UploadMeta upload) {
        S3BasicObjectMeta objectMeta = new S3BasicObjectMeta();
        objectMeta.setKey(upload.getKey());
        objectMeta.setContentType(upload.getContentType());
        objectMeta.setCacheControl(upload.getCacheControl());
        objectMeta.setContentDisposition(upload.getContentDisposition());
        objectMeta.setContentEncoding(upload.getContentEncoding());
        objectMeta.setContentLanguage(upload.getContentLanguage());
        objectMeta.setExpires(upload.getExpires());
        objectMeta.setMetaList(upload.getMetaList());
        objectMeta.setTagging(upload.getTagging());

        BSONObject ret = FileMappingUtil.buildFileInfo(objectMeta);
        return ret;
    }
}
