package com.sequoiacm.s3.service.impl;

import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockTimeoutException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.*;
import com.sequoiacm.s3.dao.IDGeneratorDao;
import com.sequoiacm.s3.dao.PartDao;
import com.sequoiacm.s3.dao.UploadDao;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.lock.S3LockPathFactory;
import com.sequoiacm.s3.model.*;
import com.sequoiacm.s3.processor.MultipartUploadProcessor;
import com.sequoiacm.s3.processor.MultipartUploadProcessorMgr;
import com.sequoiacm.s3.scan.*;
import com.sequoiacm.s3.service.MultiPartService;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class MultiPartServiceImpl implements MultiPartService {
    private static final Logger logger = LoggerFactory.getLogger(MultiPartServiceImpl.class);

    private static final long TIMEOUT = 60 * 1000;

    @Autowired
    IScmBucketService iScmBucketService;

    @Autowired
    IDGeneratorDao idGeneratorDao;

    @Autowired
    MetaSourceService metaSourceService;

    @Autowired
    ScmLockManager lockManager;

    @Autowired
    S3LockPathFactory lockPathFactory;

    @Autowired
    UploadDao uploadDao;

    @Autowired
    PartDao partDao;

    @Autowired
    MultipartUploadProcessorMgr multipartUploadProcessorMgr;

    @Override
    public InitiateMultipartUploadResult initMultipartUpload(ScmSession session, String bucketName,
            UploadMeta uploadMeta) throws S3ServerException {
        try {
            ScmBucket bucket = getBucket(session.getUser(), bucketName);
            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
            ScmSite siteInfo = ScmContentModule.getInstance().getLocalSiteInfo();

            Long uploadId = idGeneratorDao.getNewId(S3CommonDefine.IdType.TYPE_UPLOAD);
            uploadMeta.setBucketId(bucket.getId());
            uploadMeta.setUploadId(uploadId);
            uploadMeta.setUploadStatus(S3CommonDefine.UploadStatus.UPLOAD_INIT);
            uploadMeta.setLastModified(System.currentTimeMillis());
            uploadMeta.setSiteId(siteInfo.getId());
            uploadMeta.setSiteType(siteInfo.getDataUrl().getType());
            uploadMeta.setWsName(bucket.getWorkspace());
            uploadMeta.setWsVersion(wsInfo.getVersion());

            MultipartUploadProcessor processor = multipartUploadProcessorMgr
                    .getProcessor(siteInfo.getDataUrl().getType());
            if (null == processor) {
                throw new S3ServerException(S3Error.INTERNAL_ERROR,
                        "multipart upload is not supported in current site. current site name:"
                                + siteInfo.getName() + ", site id: " + siteInfo.getId()
                                + ", site type:" + siteInfo.getDataUrl().getType());
            }
            processor.initMultipartUpload(bucket.getWorkspace(), uploadId, uploadMeta);

            InitiateMultipartUploadResult result = new InitiateMultipartUploadResult(bucketName,
                    uploadMeta.getKey(), uploadId);
            return result;
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.PART_INIT_MULTIPART_UPLOAD_FAILED,
                    "init upload failed", e);
        }
    }

    @Override
    public Part uploadPart(ScmSession session, ScmBucket bucket, String objectName, long uploadId,
            int partNumber, String contentMD5, InputStream inputStream, long contentLength)
            throws S3ServerException {
        ScmLock lock = null;
        try {
            ScmLockPath lockPath = lockPathFactory.createUploadLockPath(uploadId);
            lock = lockManager.acquiresReadLock(lockPath, TIMEOUT);

            UploadMeta uploadMeta = getUploadMeta(bucket.getId(), objectName, uploadId);

            MultipartUploadProcessor processor = multipartUploadProcessorMgr
                    .getProcessor(uploadMeta.getSiteType());
            if (null == processor) {
                throw new S3ServerException(S3Error.INTERNAL_ERROR,
                        "multipart upload is not supported in current site. siteType:"
                                + uploadMeta.getSiteType() + ", site id: "
                                + uploadMeta.getSiteId());
            }
            return processor.uploadPart(bucket.getWorkspace(), uploadId, partNumber, contentMD5,
                    inputStream, contentLength, uploadMeta.getWsVersion());
        }
        catch (ScmLockTimeoutException e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_CONFLICT, "The uploadId is busy", e);
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_PART_FAILED,
                    "upload part failed. objectName=" + objectName + ", uploadId=" + uploadId
                            + ", partNumber=" + partNumber,
                    e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public CompleteMultipartUploadResult completeUpload(ScmSession session, String bucketName,
            String objectName, long uploadId, List<CompletePart> reqPartList,
            ServletOutputStream outputStream) throws S3ServerException {
        ScmLock lock = null;
        try {
            ScmBucket bucket = getBucket(session.getUser(), bucketName);

            ScmLockPath lockPath = lockPathFactory.createUploadLockPath(uploadId);
            lock = lockManager.acquiresWriteLock(lockPath, TIMEOUT);

            UploadMeta uploadMeta = getUploadMeta(bucket.getId(), objectName, uploadId);

            MultipartUploadProcessor processor = multipartUploadProcessorMgr
                    .getProcessor(uploadMeta.getSiteType());
            if (null == processor) {
                throw new S3ServerException(S3Error.INTERNAL_ERROR,
                        "multipart upload is not supported in current site. site type:"
                                + uploadMeta.getSiteType() + ", site id: "
                                + uploadMeta.getSiteId());
            }
            CompleteMultipartUploadResult result = processor.completeUpload(bucket.getWorkspace(),
                    session, bucketName, uploadMeta, reqPartList, outputStream);
            result.setBucket(bucketName);
            result.setKey(objectName);
            return result;
        }
        catch (ScmLockTimeoutException e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_CONFLICT, "The uploadId is busy", e);
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_PART_FAILED,
                    "upload part failed. objectName=" + objectName + ", uploadId=" + uploadId, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void abortUpload(ScmSession session, String bucketName, String objectName, long uploadId)
            throws S3ServerException {
        ScmLock lock = null;
        try {
            ScmBucket bucket = getBucket(session.getUser(), bucketName);
            ScmLockPath lockPath = lockPathFactory.createUploadLockPath(uploadId);
            lock = lockManager.acquiresWriteLock(lockPath, TIMEOUT);

            UploadMeta upload = uploadDao.queryUpload(bucket.getId(), objectName, uploadId);
            if (upload == null
                    || upload.getUploadStatus() != S3CommonDefine.UploadStatus.UPLOAD_INIT) {
                throw new S3ServerException(S3Error.PART_NO_SUCH_UPLOAD,
                        "no such upload. uploadId:" + uploadId);
            }

            upload.setUploadStatus(S3CommonDefine.UploadStatus.UPLOAD_ABORT);
            uploadDao.updateUploadMeta(null, upload);
        }
        catch (ScmLockTimeoutException e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_CONFLICT, "The uploadId is busy", e);
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_PART_FAILED,
                    "upload part failed. objectName=" + objectName + ", uploadId=" + uploadId, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public ListPartsResult listParts(ScmSession session, String bucketName, String objectName,
            long uploadId, Integer partNumberMarker, Integer maxParts, String encodingType)
            throws S3ServerException {
        MetaCursor partsCursor = null;
        try {
            ScmBucket bucket = getBucket(session.getUser(), bucketName);
            UploadMeta upload = uploadDao.queryUpload(bucket.getId(), objectName, uploadId);
            if (upload == null
                    || upload.getUploadStatus() != S3CommonDefine.UploadStatus.UPLOAD_INIT) {
                throw new S3ServerException(S3Error.PART_NO_SUCH_UPLOAD,
                        "no such upload. uploadId:" + uploadId);
            }

            Owner owner = new Owner();
            owner.setUserId(session.getUser().getUserId());
            owner.setUserName(session.getUser().getUsername());
            int maxNumber = Math.min(maxParts, RestParamDefine.MAX_KEYS_DEFAULT);
            ListPartsResult result = new ListPartsResult(bucketName, objectName, uploadId,
                    maxNumber, partNumberMarker, owner, encodingType);

            partsCursor = partDao.queryPartList(uploadId, 1, partNumberMarker, maxNumber + 1);
            if (partsCursor != null) {
                LinkedHashSet<Part> partList = result.getPartList();
                int count = 0;
                int partMarker = 0;
                while (partsCursor.hasNext() && count < maxNumber) {
                    Part part = new Part(partsCursor.getNext(), encodingType);
                    partList.add(part);
                    partMarker = part.getPartNumber();
                    count++;
                }

                if (partsCursor.hasNext()) {
                    result.setIsTruncated(true);
                    result.setNextPartNumberMarker(partMarker);
                }
            }
            return result;
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.PART_LIST_PARTS_FAILED,
                    "List Parts failed. bucket:" + bucketName + ", uploadId:" + uploadId, e);
        }
        finally {
            if (partsCursor != null) {
                partsCursor.close();
            }
        }
    }

    @Override
    public ListMultipartUploadsResult listUploadLists(ScmSession session, String bucketName,
            String prefix, String delimiter, String keyMarker, Long uploadIdMarker, Integer maxKeys,
            String encodingType) throws S3ServerException {
        try {
            ScmBucket bucket = getBucket(session.getUser(), bucketName);
            Owner owner = new Owner();
            owner.setUserId(session.getUser().getUserId());
            owner.setUserName(session.getUser().getUsername());

            int maxNumber = Math.min(maxKeys, RestParamDefine.MAX_KEYS_DEFAULT);

            ListMultipartUploadsResult result = new ListMultipartUploadsResult(bucketName,
                    maxNumber, encodingType, prefix, delimiter, keyMarker, uploadIdMarker);

            S3ScanResult scanResult = scanUpload(bucket.getId(), maxNumber, prefix, keyMarker,
                    uploadIdMarker, delimiter);
            for (RecordWrapper content : scanResult.getContent()) {
                result.getUploadList().add(new Upload(content.getRecord(), encodingType, owner));
            }
            for (String commonPrefix : scanResult.getCommonPrefixSet()) {
                result.getCommonPrefixList().add(new CommonPrefix(commonPrefix, encodingType));
            }
            result.setIsTruncated(scanResult.isTruncated());
            if (result.getIsTruncated()) {
                ListMultipartScanOffset nextOffset = (ListMultipartScanOffset) scanResult
                        .getNextScanOffset();
                if (nextOffset.getCommonPrefix() != null) {
                    result.setNextKeyMarker(
                            S3Codec.encode(nextOffset.getCommonPrefix(), encodingType));
                }
                else {
                    result.setNextKeyMarker(
                            S3Codec.encode(nextOffset.getObjKeyStartAfter(), encodingType));
                    result.setNextUploadIdMarker(nextOffset.getUploadIdStartAfter());
                }
            }

            return result;
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.PART_LIST_MULTIPART_UPLOADS_FAILED,
                    "List Uploads failed. bucket:" + bucketName, e);
        }
    }

    private UploadMeta getUploadMeta(long bucketId, String objectName, long uploadId)
            throws S3ServerException {
        UploadMeta uploadMeta = uploadDao.queryUpload(bucketId, objectName, uploadId);
        if (uploadMeta == null
                || uploadMeta.getUploadStatus() != S3CommonDefine.UploadStatus.UPLOAD_INIT) {
            throw new S3ServerException(S3Error.PART_NO_SUCH_UPLOAD,
                    "no such upload. uploadId:" + uploadId);
        }

        ScmSite siteInfo = ScmContentModule.getInstance().getLocalSiteInfo();
        if (uploadMeta.getSiteId() != siteInfo.getId()) {
            throw new S3ServerException(S3Error.PART_DIFF_SITE,
                    "The current site is different from the site of init multipart upload, current site id:"
                            + uploadMeta.getSiteId() + ", init site id:" + uploadMeta.getSiteId());
        }
        return uploadMeta;
    }

    private ScmBucket getBucket(ScmUser user, String bucketName)
            throws S3ServerException, ScmServerException {
        try {
            return iScmBucketService.getBucket(user, bucketName);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "bucket not exist:" + bucketName, e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED,
                        "You can not access the specified bucket. bucket name = " + bucketName, e);
            }
            throw e;
        }
    }

    private S3ScanResult scanUpload(long bucketId, int maxKeys, String prefix, String startAfter,
            Long uploadIdMarker, String delimiter) throws S3ServerException {
        try {
            BSONObject additionMatcher = new BasicBSONObject();
            additionMatcher.put(UploadMeta.META_BUCKET_ID, bucketId);
            additionMatcher.put(UploadMeta.META_STATUS, S3CommonDefine.UploadStatus.UPLOAD_INIT);
            BasicS3ScanMatcher scanMatcher = new BasicS3ScanMatcher(UploadMeta.META_KEY_NAME,
                    prefix, additionMatcher);
            BasicS3ScanCommonPrefixParser scanDelimiter = new BasicS3ScanCommonPrefixParser(
                    UploadMeta.META_KEY_NAME, delimiter, prefix);
            S3ScanOffset scanOffset = new ListMultipartScanOffset(startAfter, uploadIdMarker,
                    scanDelimiter.getCommonPrefix(startAfter));

            MetaAccessor accessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.UPLOAD_META_TABLE_NAME);

            S3ResourceScanner scanner = new S3ResourceScanner(
                    new BasicS3ScanRecordCursorProvider(accessor), scanMatcher, scanOffset,
                    new BasicS3ScanCommonPrefixParser(UploadMeta.META_KEY_NAME, delimiter, prefix),
                    maxKeys);
            return scanner.doScan();
        }
        catch (ScmMetasourceException e) {
            throw new S3ServerException(S3Error.PART_LIST_MULTIPART_UPLOADS_FAILED,
                    "failed to list uploads: bucketId=" + bucketId + ", prefix=" + prefix
                            + ", startAfter=" + startAfter + ", uploadIdMarker=" + uploadIdMarker
                            + ", delimiter=" + delimiter,
                    e);
        }
        catch (ScmServerException e) {
            throw new S3ServerException(S3Error.PART_LIST_MULTIPART_UPLOADS_FAILED,
                    "failed to list uploads:  bucketId=" + bucketId + ", prefix=" + prefix
                            + ", startAfter=" + startAfter + ", uploadIdMarker=" + uploadIdMarker
                            + ", delimiter=" + delimiter,
                    e);
        }

    }
}
