package com.sequoiacm.cephs3.dataoperation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3BreakpointFileContext;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.common.CephS3UserInfo;
import com.sequoiacm.common.memorypool.ScmPoolWrapper;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;

public class CephS3MultipartUploader implements CephS3DataUploader {
    private static final Logger logger = LoggerFactory.getLogger(CephS3MultipartUploader.class);
    private static final int PART_SIZE = 5 * 1024 * 1024;
    private long writeOffset;

    private byte[] buffer = null;
    private int bufferOff = 0;

    private BucketNameOption bucketNameOption;
    private String key;
    private int writingPartNum = -1;
    private CephS3DataService dataService;
    private CephS3ConnWrapper conn;
    private int fileSize;
    private List<PartETag> eTags = new ArrayList<>();
    private ScmPoolWrapper poolWrapper;
    private boolean useInnerBuffer;

    private CephS3UserInfo primaryUserInfo;

    private CephS3UserInfo standbyUserInfo;

    private String wsName;

    private int siteId;

    private ScmDataWriterContext context;
    private CephS3BucketManager bucketManager;
    private String targetBucketName;
    private boolean hasCreateActiveBucket;
    private CephS3BreakpointFileContext breakpointContext;
    private String newCreateBucket;

    public CephS3MultipartUploader(ScmService service, BucketNameOption bucketNameOp, String key,
            CephS3BreakpointFileContext cephS3BreakpointFileContext, long writeOffset,
            CephS3DataLocation cephS3DataLocation, String wsName, ScmDataWriterContext context)
            throws CephS3Exception {
        this(service, bucketNameOp, key, cephS3BreakpointFileContext, writeOffset, null, 0, wsName,
                context, cephS3DataLocation);
    }

    public CephS3MultipartUploader(ScmService service, BucketNameOption bucketNameOp, String key,
            CephS3BreakpointFileContext cephS3BreakpointFileContext, long writeOffset,
            byte[] dataBuffer, int dataBufferOffset,
            String workspaceName, ScmDataWriterContext context,
            CephS3DataLocation cephS3DataLocation) throws CephS3Exception {
        try {
            this.primaryUserInfo = cephS3DataLocation.getPrimaryUserInfo();
            this.standbyUserInfo = cephS3DataLocation.getStandbyUserInfo();
            this.wsName = workspaceName;
            this.context = context;
            this.siteId = cephS3DataLocation.getSiteId();
            initInstance(service, bucketNameOp, key, cephS3BreakpointFileContext, writeOffset,
                    dataBuffer, dataBufferOffset);
        }
        catch (Exception e) {
            releaseResource();
            throw e;
        }
    }

    private void initInstance(ScmService service, BucketNameOption bucketNameOption, String key,
            CephS3BreakpointFileContext breakpointContext, long writeOffset, byte[] dataBuffer,
            int dataBufferOffset)
            throws CephS3Exception {
        this.bucketNameOption = bucketNameOption;
        this.key = key;
        this.breakpointContext = breakpointContext;
        this.writeOffset = writeOffset;
        this.dataService = (CephS3DataService) service;
        this.conn = dataService.getConn(primaryUserInfo, standbyUserInfo);
        if (conn == null) {
            throw new CephS3Exception(
                    "construct CephS3MultipartUploader failed, cephs3 is down:bucketName="
                            + bucketNameOption + ",key=" + key);
        }
        this.bucketManager = CephS3BucketManager.getInstance();
        if (dataBuffer == null) {
            try {
                this.poolWrapper = ScmPoolWrapper.getInstance();
                this.buffer = poolWrapper.getBytes(PART_SIZE);
            }
            catch (Exception e) {
                throw new CephS3Exception(
                        "failed to acquire buffer: bucket=" + bucketNameOption
                                + ", key=" + key,
                        e);
            }
            this.useInnerBuffer = true;
            this.bufferOff = 0;
            this.fileSize = 0;
        }
        else {
            this.buffer = dataBuffer;
            if (buffer.length != PART_SIZE) {
                throw new CephS3Exception(
                        "construct CephS3MultipartUploader failed, buffer length is invalid:bucketName="
                                + bucketNameOption + ",key=" + key
                                + ", bufferLength=" + buffer.length
                                + ", expectedLength=" + PART_SIZE);
            }
            this.bufferOff = dataBufferOffset;
            if (bufferOff > buffer.length || bufferOff < 0) {
                throw new CephS3Exception(
                        "construct CephS3MultipartUploader failed, buffer off is invalid:bucketName="
                                + bucketNameOption + ",key=" + key + ", bufferOff="
                                + bufferOff
                                + ", bufferLength=" + buffer.length);
            }
            this.fileSize = bufferOff;
            this.useInnerBuffer = false;
        }

        try {
            initMultipart(key);
        }
        catch (Exception e) {
            conn = dataService.releaseAndTryGetAnotherConn(conn, primaryUserInfo, standbyUserInfo);
            if (conn == null) {
                throw e;
            }
            logger.warn(
                    "write data failed, get another ceph conn to try again: bucketName={}, key={}, conn={}",
                    bucketNameOption, key, conn.getUrl(), e);
            initMultipart(key);
        }
    }

    private void initMultipart(String key) throws CephS3Exception {
        targetBucketName = bucketNameOption.getTargetBucketName();
        try {
            sendRequestAndInit(targetBucketName, key);
        }
        catch (CephS3Exception e) {
            if (bucketNameOption.shouldHandleBucketNotExistException()
                    && e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)) {
                logger.info(
                        "failed to init multipart cause by no such bucket, try create bucket an init again: bucket={}, key={}, uploadId={}",
                        targetBucketName, key, breakpointContext.getUploadId(), e);
                if (bucketManager.createSpecifiedBucket(conn, targetBucketName)){
                    newCreateBucket = targetBucketName;
                }
                sendRequestAndInit(targetBucketName, key);
            }
            else if (bucketNameOption.shouldHandleQuotaExceedException()
                    && e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_QUOTA_EXCEEDED)) {
                // init 阶段不会发生配额异常，但是还是处理下，防止其它版本s3有不一样的表现
                logger.info(
                        "failed to init multipart cause by QUOTA_EXCEEDED, try create bucket an init again: bucket={}, key={}, uploadId={}",
                        targetBucketName, key, breakpointContext.getUploadId(), e);
                BucketCreateInfo createInfo = bucketManager.createNewActiveBucket(conn, targetBucketName,
                        bucketNameOption.getOriginBucketName(), wsName, siteId, dataService);
                targetBucketName = createInfo.getBucketName();
                if (createInfo.isCreate()){
                    newCreateBucket =  targetBucketName;
                }
                sendRequestAndInit(targetBucketName, key);
            }
            else {
                throw e;
            }
        }

        // 写入的目标桶是规则桶，无需登记
        if (!targetBucketName.equals(bucketNameOption.getOriginBucketName())) {
            context.recordTableName(targetBucketName);
        }
    }

    private void sendRequestAndInit(String bucketName, String key)
            throws CephS3Exception {
        List<PartSummary> parts;
        if (breakpointContext.getUploadId() != null) {
            parts = conn.listPart(bucketName, key, breakpointContext.getUploadId());
        }
        else {
            InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucketName,
                    key);
            InitiateMultipartUploadResult resp = conn.initiateMultipartUpload(req);
            this.breakpointContext.setUploadId(resp.getUploadId());
            parts = new ArrayList<>();
        }
        for (PartSummary ps : parts) {
            eTags.add(new PartETag(ps.getPartNumber(), ps.getETag()));
        }
    }

    @Override
    public void write(byte[] content, int offset, int len) throws CephS3Exception {
        try {
            fileSize += len;
            while (len >= buffer.length - bufferOff) {
                int writeSize = buffer.length - bufferOff;
                System.arraycopy(content, offset, buffer, bufferOff, writeSize);

                bufferOff = buffer.length;
                sendAndClearBuffer();

                len -= writeSize;
                offset += writeSize;
            }

            System.arraycopy(content, offset, buffer, bufferOff, len);
            bufferOff += len;
        }
        catch (CephS3Exception e) {
            throw e;
        }
        catch (Exception e) {
            throw new CephS3Exception(
                    "write data failed:bucketName=" + targetBucketName + ",key=" + key,
                    e);
        }
    }

    // 检查 writeOffset 必须是5m的倍数，这意味着如果上一片数据不足5M，本次writeOffset必然不会是5m的倍数，此时进行报错
    private void initPartNum() throws CephS3Exception {
        if (writeOffset % PART_SIZE != 0) {
            throw new CephS3Exception("write offset must be a multiple of " + PART_SIZE);
        }
        this.writingPartNum = (int) (writeOffset / PART_SIZE + 1);
        if (writingPartNum < 1 || writingPartNum > eTags.size() + 1) {
            throw new CephS3Exception("unexpected start partNum: bucketName=" + targetBucketName
                    + ", key=" + key + ", uploadId=" + breakpointContext.getUploadId()
                    + ", uploadedParts=" + eTags.size()
                    + ", requestStartPartNum=" + writingPartNum);
        }
    }

    private void sendAndClearBuffer() throws CephS3Exception {
        if (writingPartNum == -1) {
            initPartNum();
        }
        try {
            UploadPartResult resp = conn.uploadPart(new UploadPartRequest()
                    .withBucketName(targetBucketName)
                    .withInputStream(new ByteArrayInputStream(buffer, 0, bufferOff)).withKey(key)
                    .withPartNumber(writingPartNum).withPartSize(bufferOff)
                    .withUploadId(breakpointContext.getUploadId()));
            eTags.add(resp.getPartETag());
        }
        catch (CephS3Exception e) {
            logger.error("upload part failed:bucketName=" + targetBucketName + ",key=" + key
                    + ",part=" + writingPartNum);
            // init part 是不占用配额的，所以中间分段发生超限需要创建一个新桶，用以保证下一次分段上传不会因为配额超限失败
            if (!hasCreateActiveBucket
                    && e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_QUOTA_EXCEEDED)) {
                if (bucketNameOption.shouldHandleQuotaExceedException()) {
                    BucketCreateInfo createInfo = bucketManager.createNewActiveBucket(conn, targetBucketName,
                            bucketNameOption.getOriginBucketName(), wsName, siteId, dataService);
                    if (createInfo.isCreate()){
                        newCreateBucket = createInfo.getBucketName();
                    }
                    hasCreateActiveBucket = true;
                }
            }
            throw e;
        }
        writingPartNum++;
        bufferOff = 0;
    }

    public void flush() throws CephS3Exception {
        if (bufferOff <= 0) {
            return;
        }
        sendAndClearBuffer();
    }

    @Override
    public void cancel() {
        try {
            conn.abortMultipartUpload(
                    new AbortMultipartUploadRequest(targetBucketName, key,
                            breakpointContext.getUploadId()));
        }
        catch (Exception e) {
            logger.warn("cancel writer failed:bucketName=" + targetBucketName + ",key=" + key, e);
        }
        bufferOff = 0;
    }

    @Override
    public void close() throws CephS3Exception {
        if (conn == null) {
            return;
        }
        try {
            flush();
        }
        finally {
            releaseResource();
        }
    }

    @Override
    public void complete() throws CephS3Exception {
        flush();
        if (eTags.size() <= 0) {
            // 空文件也需要有一个空段
            sendAndClearBuffer();
        }
        conn.completeMultipartUpload(
                new CompleteMultipartUploadRequest(targetBucketName, key,
                        breakpointContext.getUploadId(), eTags));
    }

    private void releaseResource() {
        if (useInnerBuffer && buffer != null) {
            poolWrapper.releaseBytes(buffer);
            buffer = null;
        }

        eTags = null;
        if (conn != null) {
            dataService.releaseConn(conn);
            conn = null;
        }
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public String getCreatedBucketName() {
        return newCreateBucket;
    }

    public CephS3BreakpointFileContext getBreakpointContext() {
        return breakpointContext;
    }


}