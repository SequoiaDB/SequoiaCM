package com.sequoiacm.s3.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.IndexName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.model.SessionInfoWrapper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.FileMappingUtil;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.context.S3ListObjContextMgr;
import com.sequoiacm.s3.context.S3ListObjectContext;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.core.CopyObjectRequest;
import com.sequoiacm.s3.core.Range;
import com.sequoiacm.s3.core.S3BasicObjectMeta;
import com.sequoiacm.s3.core.S3ObjectMeta;
import com.sequoiacm.s3.core.S3PutObjectRequest;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.CopyObjectResult;
import com.sequoiacm.s3.model.DeleteError;
import com.sequoiacm.s3.model.DeleteObjectResult;
import com.sequoiacm.s3.model.DeleteObjects;
import com.sequoiacm.s3.model.DeleteObjectsResult;
import com.sequoiacm.s3.model.GetObjectResult;
import com.sequoiacm.s3.model.ListObjectCommonPrefix;
import com.sequoiacm.s3.model.ListObjectsResult;
import com.sequoiacm.s3.model.ListObjectsResultV1;
import com.sequoiacm.s3.model.ListVersionsResult;
import com.sequoiacm.s3.model.ObjectDeleted;
import com.sequoiacm.s3.model.ObjectMatcher;
import com.sequoiacm.s3.model.ObjectTagResult;
import com.sequoiacm.s3.model.ObjectToDel;
import com.sequoiacm.s3.model.PutObjectResult;
import com.sequoiacm.s3.scan.BasicS3ScanCommonPrefixParser;
import com.sequoiacm.s3.scan.BasicS3ScanMatcher;
import com.sequoiacm.s3.scan.BasicS3ScanRecordCursorProvider;
import com.sequoiacm.s3.scan.ListObjVersionScanOffset;
import com.sequoiacm.s3.scan.ListObjectScanOffset;
import com.sequoiacm.s3.scan.ListObjectVersionRecordCursorProvider;
import com.sequoiacm.s3.scan.ListVersionRecordWrapper;
import com.sequoiacm.s3.scan.RecordWrapper;
import com.sequoiacm.s3.scan.S3ResourceScanner;
import com.sequoiacm.s3.scan.S3ScanResult;
import com.sequoiacm.s3.service.BucketService;
import com.sequoiacm.s3.service.ObjectService;
import com.sequoiacm.s3.utils.CommonUtil;
import com.sequoiacm.s3.utils.DataFormatUtils;
import com.sequoiacm.s3.utils.VersionUtil;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ObjServiceImpl implements ObjectService {
    private static final Logger logger = LoggerFactory.getLogger(ObjServiceImpl.class);
    @Autowired
    private S3ListObjContextMgr contextMgr;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private IDatasourceService datasourceService;

    @Autowired
    private IScmBucketService scmBucketService;

    @Autowired
    private IFileService fileService;

    @Autowired
    private ScmAudit audit;

    @Autowired
    private MetaSourceService metaSourceService;

    private final int MAX_KEYS = 1000;

    @Override
    public PutObjectResult putObject(ScmSession session, S3PutObjectRequest req)
            throws S3ServerException {
        ScmDataInfoDetail dataDetail = null;
        Bucket s3Bucket = null;
        try {
            s3Bucket = bucketService.getBucket(session, req.getObjectMeta().getBucket());
            long objCreateTime = req.getObjectCreateTime() <= -1 ? System.currentTimeMillis()
                    : req.getObjectCreateTime();
            dataDetail = datasourceService.createData(s3Bucket.getRegion(), req.getObjectData(),
                    objCreateTime);

            if (req.getMd5() != null) {
                if (req.getMd5().length() % 4 != 0) {
                    throw new S3ServerException(S3Error.OBJECT_INVALID_DIGEST,
                            "decode md5 failed, contentMd5:" + req.getMd5());
                }
                if (!dataDetail.getMd5().equals(req.getMd5())) {
                    throw new S3ServerException(S3Error.OBJECT_BAD_DIGEST,
                            "The Content-MD5 you specified does not match what we received.");
                }
            }
            if (req.getObjectMeta().getSize() != -1
                    && dataDetail.getSize() != req.getObjectMeta().getSize()) {
                throw new S3ServerException(S3Error.OBJECT_INCOMPLETE_BODY,
                        "content length is " + req.getObjectMeta().getSize() + " and receive "
                                + dataDetail.getSize() + " bytes");
            }

            BSONObject fileInfo = FileMappingUtil.buildFileInfo(req.getObjectMeta());
            fileInfo.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, objCreateTime);
            FileMeta fileMeta = FileMeta.fromUser(s3Bucket.getRegion(), fileInfo,
                    session.getUser().getUsername());
            fileMeta.resetDataInfo(dataDetail.getDataInfo().getId(),
                    dataDetail.getDataInfo().getCreateTime().getTime(),
                    dataDetail.getDataInfo().getType(), dataDetail.getSize(), dataDetail.getMd5(),
                    dataDetail.getSiteId(), dataDetail.getDataInfo().getWsVersion(),
                    dataDetail.getDataInfo().getTableName());

            FileMeta createdFile = scmBucketService.createFile(session.getUser(),
                    s3Bucket.getBucketName(), fileMeta, (TransactionCallback) null, false);
            audit.info(ScmAuditType.CREATE_S3_OBJECT, session.getUser(), s3Bucket.getRegion(), 0,
                    "create s3 object meta: bucketName=" + s3Bucket.getBucketName() + ", key="
                            + req.getObjectMeta().getKey());

            S3ObjectMeta s3ObjMeta = FileMappingUtil.buildS3ObjectMeta(s3Bucket.getBucketName(),
                    createdFile.toBSONObject());
            PutObjectResult res = new PutObjectResult();
            res.seteTag(s3ObjMeta.getEtag());
            if (!s3Bucket.getVersionStatus().equals(ScmBucketVersionStatus.Disabled.name())) {
                res.setVersionId(s3ObjMeta.getVersionId());
            }
            return res;
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.COMMIT_UNCERTAIN_STATE) {
                deleteDataSilence(s3Bucket, dataDetail);
            }
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "bucket not exist:" + req.getObjectMeta().getBucket(), e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED,
                        "You can not access the specified bucket. bucket name = "
                                + req.getObjectMeta().getBucket(),
                        e);
            }
            throw new S3ServerException(S3Error.OBJECT_PUT_FAILED,
                    "failed to put object: bucket=" + req.getObjectMeta().getBucket()
                            + ", objectKey=" + req.getObjectMeta().getKey(),
                    e);
        }
        catch (Exception e) {
            deleteDataSilence(s3Bucket, dataDetail);
            throw e;
        }
    }

    private void deleteDataSilence(Bucket s3Bucket, ScmDataInfoDetail dataDetail) {
        if (s3Bucket == null || dataDetail == null) {
            return;
        }
        try {
            datasourceService.deleteData(s3Bucket.getRegion(), dataDetail.getDataInfo(), dataDetail.getSiteId());
        }
        catch (Exception e) {
            logger.warn("failed to delete data: ws={}, dataId={}, siteId={}", s3Bucket.getRegion(),
                    dataDetail.getDataInfo().getId(), dataDetail.getSiteId(), e);
        }
    }

    @Override
    public S3ObjectMeta getObjectMeta(ScmSession session, String bucket, String objectName,
            String versionId, ObjectMatcher matchers) throws S3ServerException {
        try {
            Bucket s3Bucket = bucketService.getBucket(session, bucket);

            BSONObject fileInfo = getFileInfo(session, bucket, objectName, versionId);
            S3ObjectMeta objectMeta = FileMappingUtil.buildS3ObjectMeta(bucket, fileInfo);
            if (!objectMeta.isDeleteMarker()) {
                match(matchers, objectMeta);
            }
            audit.info(ScmAuditType.S3_OBJECT_DQL, session.getUser(), s3Bucket.getRegion(), 0,
                    "get s3 object meta: bucketName=" + bucket + ", key=" + objectName);
            return objectMeta;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "object not exist: bucket=" + bucket + ", object=" + objectName, e);
            }
            throw new S3ServerException(S3Error.OBJECT_GET_FAILED,
                    "failed to get object: bucket=" + bucket + ", object=" + objectName, e);
        }
    }

    private String trimQuotes(String str) {
        if (str == null) {
            return str;
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

    private static Date parseDate(String dateString) throws S3ServerException {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "EEE, dd MMM yyyy HH:mm:ss ZZZ", Locale.ENGLISH);
            return simpleDateFormat.parse(dateString);
        }
        catch (ParseException e) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_TIME,
                    "dateString is invalid. dateString:" + dateString, e);
        }
    }

    private long getSecondTime(long millionSecond) {
        return millionSecond / 1000;
    }

    private void match(ObjectMatcher matchers, S3ObjectMeta objectMeta) throws S3ServerException {
        String eTag = objectMeta.getEtag();
        long lastModifiedTime = objectMeta.getLastModified();
        boolean isMatch = false;
        boolean isNoneMatch = false;

        String matchEtag = matchers.getIfMatch();
        if (null != matchEtag) {
            String matchEtagString = trimQuotes(matchEtag);
            if (!matchEtagString.equals(eTag)) {
                throw new S3ServerException(S3Error.OBJECT_IF_MATCH_FAILED,
                        "if-match failed: if-match value:" + matchEtag + ", current object eTag:"
                                + eTag);
            }
            isMatch = true;
        }
        String noneMatchEtag = matchers.getIfNoneMatch();
        if (null != noneMatchEtag) {
            String noneMatchEtagString = trimQuotes(noneMatchEtag);
            if (noneMatchEtagString.equals(eTag)) {
                throw new S3ServerException(S3Error.OBJECT_IF_NONE_MATCH_FAILED,
                        "if-none-match failed: if-none-match value:" + noneMatchEtag
                                + ", current object eTag:" + eTag);
            }
            isNoneMatch = true;
        }

        String unModifiedSince = matchers.getIfUnmodifiedSince();
        if (null != unModifiedSince) {
            Date date = parseDate(unModifiedSince);
            if (getSecondTime(date.getTime()) < getSecondTime(lastModifiedTime)) {
                if (!isMatch) {
                    throw new S3ServerException(S3Error.OBJECT_IF_UNMODIFIED_SINCE_FAILED,
                            "if-unmodified-since failed: if-unmodified-since value:"
                                    + unModifiedSince + ", current object lastModifiedTime:"
                                    + new Date(lastModifiedTime));
                }
            }
        }

        String modifiedSince = matchers.getIfModifiedSince();
        if (null != modifiedSince) {
            Date date = parseDate(modifiedSince);
            if (getSecondTime(date.getTime()) >= getSecondTime(lastModifiedTime)) {
                if (!isNoneMatch) {
                    throw new S3ServerException(S3Error.OBJECT_IF_MODIFIED_SINCE_FAILED,
                            "if-modified-since failed: if-modified-since value:" + modifiedSince
                                    + ", current object lastModifiedTime:"
                                    + new Date(lastModifiedTime));
                }
            }
        }
    }

    @Override
    public GetObjectResult getObject(ScmSession session, String bucketName, String objectName,
            String versionId, ObjectMatcher matcher, Range range) throws S3ServerException {
        try {
            Bucket s3Bucket = bucketService.getBucket(session, bucketName);
            BSONObject fileInfo = getFileInfo(session, bucketName, objectName, versionId);
            S3ObjectMeta s3ObjectMeta = FileMappingUtil.buildS3ObjectMeta(bucketName, fileInfo);
            if (s3ObjectMeta.isDeleteMarker()) {
                return new GetObjectResult(s3ObjectMeta, null);
            }
            match(matcher, s3ObjectMeta);
            InputStream objectData;
            if (range != null) {
                CommonUtil.analyseRangeWithFileSize(range, s3ObjectMeta.getSize());
                objectData = new ScmFileInputStreamAdapter(session, fileService,
                        s3Bucket.getRegion(), fileInfo, range.getStart(), range.getContentLength());
            }
            else {
                objectData = new ScmFileInputStreamAdapter(session, fileService,
                        s3Bucket.getRegion(), fileInfo, 0, Long.MAX_VALUE);
            }
            audit.info(ScmAuditType.S3_OBJECT_DQL, session.getUser(), s3Bucket.getRegion(), 0,
                    "get s3 object: bucketName=" + bucketName + ", key=" + objectName);
            return new GetObjectResult(s3ObjectMeta, objectData);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "object not exist: bucket=" + bucketName + ", object=" + objectName, e);
            }
            throw new S3ServerException(S3Error.OBJECT_GET_FAILED,
                    "failed to get object: bucket=" + bucketName + ", object=" + objectName, e);

        }
    }

    private BSONObject getFileInfo(ScmSession session, String bucketName, String objectName,
            String versionId) throws ScmServerException, S3ServerException {
        if (versionId == null) {
            return scmBucketService.getFileVersion(session.getUser(), bucketName, objectName, -1,
                    -1);
        }
        try {
            if (versionId.equals(S3CommonDefine.NULL_VERSION_ID)) {
                return scmBucketService.getFileNullVersion(session.getUser(), bucketName,
                        objectName);
            }
            ScmVersion version = VersionUtil.parseVersion(versionId);
            return scmBucketService.getFileVersion(session.getUser(), bucketName, objectName,
                    version.getMajorVersion(), version.getMinorVersion());
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_VERSION,
                        "no such version: bucket=" + bucketName + ", key=" + objectName
                                + ", version=" + versionId,
                        e);
            }
            throw e;
        }

    }

    @Override
    public CopyObjectResult copyObject(ScmSession session, CopyObjectRequest request)
            throws S3ServerException {
        try {
            Bucket srcBucket = bucketService.getBucket(session, request.getSourceObjectBucket());
            BSONObject fileInfo = getFileInfo(session, request.getSourceObjectBucket(),
                    request.getSourceObjectKey(), request.getSourceObjectVersion());
            S3ObjectMeta sourceObjectMeta = FileMappingUtil
                    .buildS3ObjectMeta(request.getSourceObjectBucket(), fileInfo);
            if (sourceObjectMeta.isDeleteMarker()) {
                if (request.getSourceObjectVersion() != null) {
                    throw new S3ServerException(S3Error.OBJECT_COPY_DELETE_MARKER,
                            "source object with versionId is a deleteMarker: bucket="
                                    + request.getSourceObjectBucket() + ", key="
                                    + request.getSourceObjectKey() + ", version="
                                    + request.getSourceObjectVersion());
                }
                else {
                    throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                            "source object is a deleteMarker bucket="
                                    + request.getSourceObjectBucket() + ", key="
                                    + request.getSourceObjectKey());
                }
            }
            checkIsValidCopy(request, sourceObjectMeta);
            S3BasicObjectMeta destObjectMeta = buildDestObjectMeta(request, sourceObjectMeta);
            ScmFileInputStreamAdapter fileReader = new ScmFileInputStreamAdapter(session,
                    fileService, srcBucket.getRegion(), fileInfo, 0, Long.MAX_VALUE);
            PutObjectResult result;
            try {
                result = putObject(session,
                        new S3PutObjectRequest(destObjectMeta,
                                BsonUtils.getString(fileInfo, FieldName.FIELD_CLFILE_FILE_MD5),
                                fileReader));
            }
            finally {
                fileReader.close();
            }
            CopyObjectResult copyObjectResult = new CopyObjectResult();
            copyObjectResult.seteTag(result.geteTag());
            copyObjectResult.setLastModified(
                    DataFormatUtils.formatISO8601Date(sourceObjectMeta.getLastModified()));
            copyObjectResult.setVersionId(result.getVersionId());
            if (sourceObjectMeta.getVersionId() != null) {
                copyObjectResult.setSourceVersionId(sourceObjectMeta.getVersionId());
            }

            audit.info(ScmAuditType.CREATE_S3_OBJECT, session.getUser(), "null", 0,
                    "copy s3 object: srcBucket=" + request.getSourceObjectBucket() + ", srcObject="
                            + request.getSourceObjectKey() + ", destBucket="
                            + request.getDestObjectMeta().getBucket() + ", destObject="
                            + request.getDestObjectMeta().getKey());
            return copyObjectResult;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "failed to copy object, object not exist: bucket="
                                + request.getSourceObjectBucket() + ", object="
                                + request.getSourceObjectKey(),
                        e);
            }
            throw new S3ServerException(S3Error.OBJECT_COPY_FAILED,
                    "failed to copy object: req=" + request, e);
        }
    }

    private S3BasicObjectMeta buildDestObjectMeta(CopyObjectRequest request,
            S3ObjectMeta sourceObjectMeta) {
        if (request.isUseSourceObjectMeta()) {
            S3BasicObjectMeta ret = new S3BasicObjectMeta(sourceObjectMeta);
            ret.setBucket(request.getDestObjectMeta().getBucket());
            ret.setKey(request.getDestObjectMeta().getKey());
            if (request.isUseSourceObjectTagging()) {
                ret.setTagging(sourceObjectMeta.getTagging());
            } else {
                ret.setTagging(request.getNewObjectTagging());
            }
            return ret;
        }
        request.getDestObjectMeta().setSize(sourceObjectMeta.getSize());
        return request.getDestObjectMeta();
    }

    private void checkIsValidCopy(CopyObjectRequest request, S3ObjectMeta objectMeta)
            throws S3ServerException {
        match(request.getSourceObjectMatcher(), objectMeta);
        // check if itself change, 参照s3顺序，先判断对象合法存在
        if (request.getDestObjectMeta().getBucket().equals(request.getSourceObjectBucket())
                && request.getDestObjectMeta().getKey().equals(request.getSourceObjectKey())
                && request.getSourceObjectVersion() == null) {
            if (request.isUseSourceObjectMeta()) {
                throw new S3ServerException(S3Error.OBJECT_COPY_WITHOUT_CHANGE,
                        "copy an object to itself without changing the object's metadata.");
            }
        }
        if (!request.isUseSourceObjectMeta()) {
            if (request.getDestObjectMeta()
                    .getMetaListLength() > RestParamDefine.X_AMZ_META_LENGTH) {
                throw new S3ServerException(S3Error.OBJECT_METADATA_TOO_LARGE,
                        "metadata headers exceed the maximum. xMeta:"
                                + request.getDestObjectMeta().getMetaList());
            }
        }
    }

    @Override
    public DeleteObjectResult deleteObject(ScmSession session, String bucketName, String objectName)
            throws S3ServerException {
        try {
            Bucket s3Bucket = bucketService.getBucket(session, bucketName);
            FileMeta deleteMarker = scmBucketService.deleteFile(session.getUser(), bucketName,
                    objectName, false,
                    new SessionInfoWrapper(session.getSessionId(), session.getUserDetail()));
            audit.info(ScmAuditType.DELETE_S3_OBJECT, session.getUser(), s3Bucket.getRegion(), 0,
                    "delete s3 object: bucketName=" + bucketName + ", key=" + objectName);
            if (deleteMarker == null) {
                return null;
            }
            S3ObjectMeta s3ObjMeta = FileMappingUtil.buildS3ObjectMeta(bucketName,
                    deleteMarker.toBSONObject());
            DeleteObjectResult ret = new DeleteObjectResult();
            ret.setDeleteMarker(s3ObjMeta.isDeleteMarker());
            if (!s3Bucket.getVersionStatus().equals(ScmBucketVersionStatus.Disabled.name())) {
                ret.setVersionId(s3ObjMeta.getVersionId());
            }
            return ret;
        }
        catch (ScmServerException e) {
            throw new S3ServerException(S3Error.OBJECT_DELETE_FAILED,
                    "failed to delete object: bucket=" + bucketName + ", objectKey=" + objectName,
                    e);
        }

    }

    @Override
    public DeleteObjectResult deleteObject(ScmSession session, String bucketName, String objectKey,
            String versionId) throws S3ServerException {
        try {
            FileMeta deletedVersion = null;
            Bucket s3Bucket = bucketService.getBucket(session, bucketName);
            if (versionId != null) {
                if (versionId.equals(S3CommonDefine.NULL_VERSION_ID)) {
                    deletedVersion = scmBucketService.deleteNullVersionFile(session.getUser(),
                            bucketName, objectKey);
                }
                else {
                    ScmVersion scmVersion = VersionUtil.parseVersionIgnoreInvalidId(versionId);
                    if (scmVersion == null) {
                        return null;
                    }
                    deletedVersion = scmBucketService.deleteFileVersion(session.getUser(),
                            bucketName, objectKey, scmVersion.getMajorVersion(),
                            scmVersion.getMinorVersion());
                }
            }
            else {
                deletedVersion = scmBucketService.deleteFileVersion(session.getUser(), bucketName,
                        objectKey, -1, -1);
            }
            audit.info(ScmAuditType.DELETE_S3_OBJECT, session.getUser(), s3Bucket.getRegion(), 0,
                    "delete s3 object: bucketName=" + bucketName + ", key=" + objectKey
                            + ", version=" + (versionId == null ? "nullMarker" : versionId));
            if (deletedVersion == null) {
                return null;
            }
            S3ObjectMeta s3ObjMeta = FileMappingUtil.buildS3ObjectMeta(bucketName,
                    deletedVersion.toBSONObject());
            DeleteObjectResult ret = new DeleteObjectResult();
            ret.setDeleteMarker(s3ObjMeta.isDeleteMarker());
            ret.setVersionId(s3ObjMeta.getVersionId());
            return ret;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                return null;
            }
            throw new S3ServerException(S3Error.OBJECT_DELETE_FAILED,
                    "failed to delete object: bucket=" + bucketName + ", objectKey=" + objectKey,
                    e);
        }

    }

    @Override
    public DeleteObjectsResult deleteObjects(ScmSession session, String bucketName,
            DeleteObjects deleteObjects) throws S3ServerException {
        bucketService.getBucket(session, bucketName);

        DeleteObjectsResult deleteObjectsResult = new DeleteObjectsResult();
        if (deleteObjects != null && deleteObjects.getObjects() != null) {
            List<ObjectToDel> objects = deleteObjects.getObjects();
            for (ObjectToDel object : objects) {
                try {
                    DeleteObjectResult result;
                    if (object.getVersionId() != null) {
                        result = deleteObject(session, bucketName, object.getKey(),
                                object.getVersionId());
                        logger.debug(
                                "delete object with version id success. bucketName={}, objectName={}, versionId={}",
                                bucketName, object.getKey(), object.getVersionId());
                    }
                    else {
                        result = deleteObject(session, bucketName, object.getKey());
                        logger.debug("delete object success. bucketName={}, objectName={}",
                                bucketName, object.getKey());
                    }
                    if (!deleteObjects.getQuiet()) {
                        ObjectDeleted deleted = new ObjectDeleted();
                        if (result != null && result.isDeleteMarker()) {
                            deleted.setDeleteMarker(true);
                            deleted.setDeleteMarkerVersion(result.getVersionId());
                        }
                        deleted.setVersionId(object.getVersionId());
                        deleted.setKey(object.getKey());
                        deleteObjectsResult.getDeletedObjects().add(deleted);
                    }
                }
                catch (Exception e) {
                    DeleteError error = new DeleteError();
                    if (e instanceof S3ServerException) {
                        S3Error s3Error = ((S3ServerException) e).getError();
                        error.setCode(s3Error.getCode());
                        error.setMessage(s3Error.getErrorMessage());
                    }
                    else {
                        error.setCode(S3Error.OBJECT_DELETE_FAILED.getCode());
                        error.setMessage(e.getMessage());
                    }
                    error.setKey(object.getKey());

                    if (object.getVersionId() != null) {
                        error.setVersionId(object.getVersionId());
                        logger.error(
                                "delete object failed. bucketName={}, objectName={}, versionId={}",
                                bucketName, object.getKey(), object.getVersionId(), e);
                    }
                    else {
                        logger.error("delete object failed. bucketName={}, objectName={}",
                                bucketName, object.getKey(), e);
                    }
                    deleteObjectsResult.getErrors().add(error);
                }
            }
        }
        return deleteObjectsResult;
    }

    @Override
    public ListObjectsResultV1 listObjectsV1(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, int maxKeys, String encodingType)
            throws S3ServerException {
        ListObjectsResultV1 listObjectsResult = new ListObjectsResultV1(bucketName, maxKeys,
                encodingType, prefix, startAfter, delimiter);
        if (maxKeys <= 0) {
            return listObjectsResult;
        }

        Bucket s3Bucket = bucketService.getBucket(session, bucketName);
        S3ScanResult scanResult = scan(bucketName, maxKeys, prefix, startAfter, delimiter);
        for (RecordWrapper content : scanResult.getContent()) {
            listObjectsResult.addContent(
                    FileMappingUtil.buildListObjContent(content.getRecord(), encodingType));
        }
        for (String commonPrefix : scanResult.getCommonPrefixSet()) {
            listObjectsResult.addCommonPrefix(
                    new ListObjectCommonPrefix((S3Codec.encode(commonPrefix, encodingType))));
        }
        listObjectsResult.setIsTruncated(scanResult.isTruncated());
        if (listObjectsResult.getIsTruncated()) {
            ListObjectScanOffset nextOffset = (ListObjectScanOffset) scanResult.getNextScanOffset();
            String nextKeyMarker = nextOffset.getCommonPrefix() == null
                    ? nextOffset.getObjKeyStartAfter()
                    : nextOffset.getCommonPrefix();
            listObjectsResult.setNextMarker(S3Codec.encode(nextKeyMarker, encodingType));
        }
        audit.info(ScmAuditType.S3_OBJECT_DQL, session.getUser(), s3Bucket.getRegion(), 0,
                "list s3 object v1: bucketName=" + bucketName + ", prefix=" + prefix
                        + ", delimiter=" + delimiter + ", startAfter=" + startAfter + ",encodeType="
                        + encodingType);
        return listObjectsResult;
    }

    @Override
    public ListVersionsResult listVersions(ScmSession session, String bucketName, String prefix,
            String delimiter, String keyMarker, String versionIdMarker, int maxKeys,
            String encodingType) throws S3ServerException {

        ListVersionsResult listVersionsResult = new ListVersionsResult(bucketName, maxKeys,
                encodingType, prefix, delimiter, keyMarker, versionIdMarker);
        if (maxKeys == 0) {
            return listVersionsResult;
        }

        if (maxKeys > MAX_KEYS) {
            maxKeys = MAX_KEYS;
        }

        Bucket s3Bucket = bucketService.getBucket(session, bucketName);
        S3ScanResult scanResult = scanVersion(bucketName, maxKeys, prefix, keyMarker,
                versionIdMarker, delimiter);
        for (RecordWrapper content : scanResult.getContent()) {
            S3ObjectMeta s3ObjMeta = FileMappingUtil.buildS3ObjectMeta(bucketName,
                    content.getRecord());
            boolean isLatestVersion = ((ListVersionRecordWrapper) content).isLatestVersion();
            if (s3ObjMeta.isDeleteMarker()) {
                listVersionsResult.addDeleteMarker(FileMappingUtil.buildListDeleteMarker(s3ObjMeta,
                        encodingType, isLatestVersion));
            }
            else {
                listVersionsResult.addVersion(FileMappingUtil.buildListObjVersion(s3ObjMeta,
                        encodingType, isLatestVersion));
            }
        }
        for (String commonPrefix : scanResult.getCommonPrefixSet()) {
            listVersionsResult.addCommonPrefix(
                    new ListObjectCommonPrefix(S3Codec.encode(commonPrefix, encodingType)));
        }
        listVersionsResult.setIsTruncated(scanResult.isTruncated());
        if (scanResult.isTruncated()) {
            ListObjVersionScanOffset nextOffset = (ListObjVersionScanOffset) scanResult
                    .getNextScanOffset();
            String nextKeyMarker = nextOffset.getCommonPrefix() == null
                    ? nextOffset.getObjKeyStartAfter()
                    : nextOffset.getCommonPrefix();
            listVersionsResult.setNextKeyMarker(S3Codec.encode(nextKeyMarker, encodingType));
            if (nextOffset.getCommonPrefix() == null) {
                listVersionsResult.setNextVersionIdMarker(nextOffset.getVersionIdMarker());
            }
            else {
                listVersionsResult.setNextVersionIdMarker(null);
            }
        }
        audit.info(ScmAuditType.S3_OBJECT_DQL, session.getUser(), s3Bucket.getRegion(), 0,
                "list s3 object versions: bucketName=" + bucketName + ", prefix=" + prefix
                        + ", delimiter=" + delimiter + ", keyMarker=" + keyMarker
                        + ", versionIdMarker=" + versionIdMarker + ",encodeType=" + encodingType);
        return listVersionsResult;
    }

    private S3ScanResult scanVersion(String bucketName, int maxKeys, String prefix,
            String keyMarker, String versionIdMarker, String delimiter) throws S3ServerException {
        try {
            BasicS3ScanMatcher scanMatcher = new BasicS3ScanMatcher(FieldName.BucketFile.FILE_NAME,
                    prefix, null);
            BasicS3ScanCommonPrefixParser scanDelimiter = new BasicS3ScanCommonPrefixParser(
                    FieldName.BucketFile.FILE_NAME, delimiter, prefix);
            ListObjVersionScanOffset scanOffset = new ListObjVersionScanOffset(scmBucketService,
                    bucketName, keyMarker, versionIdMarker,
                    scanDelimiter.getCommonPrefix(keyMarker));
            ScmBucket scmbucket = scmBucketService.getBucket(bucketName);
            S3ResourceScanner scanner = new S3ResourceScanner(
                    new ListObjectVersionRecordCursorProvider(scmbucket), scanMatcher, scanOffset,
                    scanDelimiter, maxKeys);
            return scanner.doScan();

        }
        catch (ScmMetasourceException e) {
            throw new S3ServerException(S3Error.OBJECT_LIST_FAILED,
                    "failed to list version: bucket=" + bucketName + ", prefix=" + prefix
                            + ", startAfter=" + keyMarker + ", versionIdMarker=" + versionIdMarker
                            + ", delimiter=" + delimiter,
                    e);

        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "failed to list version: bucket=" + bucketName + ", prefix=" + prefix
                                + ", startAfter=" + keyMarker + ", versionIdMarker="
                                + versionIdMarker + ", delimiter=" + delimiter,
                        e);
            }
            throw new S3ServerException(S3Error.OBJECT_LIST_FAILED,
                    "failed to list version: bucket=" + bucketName + ", prefix=" + prefix
                            + ", startAfter=" + keyMarker + ", versionIdMarker=" + versionIdMarker
                            + ", delimiter=" + delimiter,
                    e);
        }

    }

    @Override
    public ListObjectsResult listObjectsV2(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, int maxKeys, String continueToken,
            String encodingType, boolean fetchOwner) throws S3ServerException {
        ListObjectsResult listObjectsResult = new ListObjectsResult(bucketName, maxKeys,
                encodingType, prefix, startAfter, delimiter, continueToken);
        if (maxKeys == 0) {
            return listObjectsResult;
        }

        S3ListObjectContext context = null;
        if (continueToken != null) {
            context = contextMgr.getContext(continueToken);
            if (!isContextMatch(context, prefix, startAfter, delimiter)) {
                context = null;
            }
        }
        if (context == null) {
            context = contextMgr.createContext(prefix, startAfter, delimiter, bucketName);
        }
        Bucket s3Bucket = bucketService.getBucket(session, bucketName);
        S3ScanResult scanResult = scan(bucketName, maxKeys, prefix, context.getLastMarker(),
                delimiter);
        for (RecordWrapper content : scanResult.getContent()) {
            listObjectsResult.addContent(
                    FileMappingUtil.buildListObjContent(content.getRecord(), encodingType));
        }
        for (String commonPrefix : scanResult.getCommonPrefixSet()) {
            listObjectsResult.addCommonPrefix(
                    new ListObjectCommonPrefix(S3Codec.encode(commonPrefix, encodingType)));
        }
        listObjectsResult.setIsTruncated(scanResult.isTruncated());
        if (scanResult.isTruncated()) {
            listObjectsResult.setNextContinueToken(context.getToken());
            ListObjectScanOffset nextOffset = (ListObjectScanOffset) scanResult.getNextScanOffset();
            context.setLastMarker(nextOffset.getObjKeyStartAfter());
            context.save();
        }
        else {
            context.release();
        }
        listObjectsResult.setKeyCount(scanResult.getSize());
        audit.info(ScmAuditType.S3_OBJECT_DQL, session.getUser(), s3Bucket.getRegion(), 0,
                "list s3 object v2: bucketName=" + bucketName + ", prefix=" + prefix
                        + ", delimiter=" + delimiter + ", startAfter=" + startAfter + ",encodeType="
                        + encodingType + ", continueToken=" + continueToken);
        return listObjectsResult;
    }

    private S3ScanResult scan(String bucketName, int maxKeys, String prefix, String startAfter,
            String delimiter) throws S3ServerException {
        if (maxKeys > MAX_KEYS) {
            maxKeys = MAX_KEYS;
        }

        try {
            BasicS3ScanMatcher scanMatcher = new BasicS3ScanMatcher(FieldName.BucketFile.FILE_NAME,
                    prefix, new BasicBSONObject(FieldName.BucketFile.FILE_DELETE_MARKER, false));
            BasicS3ScanCommonPrefixParser scanDelimiter = new BasicS3ScanCommonPrefixParser(
                    FieldName.BucketFile.FILE_NAME, delimiter, prefix);
            ListObjectScanOffset scanOffset = new ListObjectScanOffset(startAfter,
                    scanDelimiter.getCommonPrefix(startAfter));
            ScmBucket scmbucket = scmBucketService.getBucket(bucketName);
            MetaAccessor accessor = scmbucket.getFileTableAccessor(null);
            BasicS3ScanRecordCursorProvider cursorProvider = new BasicS3ScanRecordCursorProvider(
                    accessor, new BasicBSONObject("", IndexName.BucketFile.FILE_NAME_UNIQUE_IDX));
            S3ResourceScanner scanner = new S3ResourceScanner(cursorProvider, scanMatcher,
                    scanOffset, scanDelimiter, maxKeys);
            return scanner.doScan();

        }
        catch (ScmMetasourceException e) {
            throw new S3ServerException(S3Error.OBJECT_LIST_FAILED,
                    "failed to list object: bucket=" + bucketName + ", prefix=" + prefix
                            + ", startAfter=" + startAfter + ", delimiter=" + delimiter,
                    e);

        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "failed to list object, bucket not exist:  bucket=" + bucketName
                                + ", prefix=" + prefix + ", startAfter=" + startAfter
                                + ", delimiter=" + delimiter,
                        e);
            }
            throw new S3ServerException(S3Error.OBJECT_LIST_FAILED,
                    "failed to list object:  bucket=" + bucketName + ", prefix=" + prefix
                            + ", startAfter=" + startAfter + ", delimiter=" + delimiter,
                    e);
        }

    }

    private boolean isContextMatch(S3ListObjectContext contextMeta, String prefix,
            String startAfter, String delimiter) {
        if (contextMeta.getDelimiter() != null) {
            if (!(contextMeta.getDelimiter().equals(delimiter))) {
                return false;
            }
        }
        else if (delimiter != null) {
            return false;
        }

        if (contextMeta.getPrefix() != null) {
            if (!(contextMeta.getPrefix().equals(prefix))) {
                return false;
            }
        }
        else if (prefix != null) {
            return false;
        }

        if (contextMeta.getStartAfter() != null) {
            if (!(contextMeta.getStartAfter().equals(startAfter))) {
                return false;
            }
        }
        else if (startAfter != null) {
            return false;
        }

        return true;
    }

    @Override
    public String setObjectTag(ScmSession session, String bucketName, String objectName,
            Map<String, String> customTag, String versionId) throws S3ServerException {
        try {
            Bucket s3Bucket = bucketService.getBucket(session, bucketName);
            ScmArgChecker.File.checkFileTag(customTag);
            String fileId = scmBucketService.getFileId(session.getUser(), bucketName, objectName);
            BSONObject newProperties = new BasicBSONObject();
            newProperties.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, new BasicBSONObject(customTag));
            FileMeta fileMeta = null;
            if (versionId == null) {
                fileMeta = fileService.updateFileInfo(session.getUser(), s3Bucket.getRegion(),
                        fileId, newProperties, -1, -1);
            }
            else {
                ScmVersion version = VersionUtil.parseVersion(versionId);
                fileMeta = fileService.updateFileInfo(session.getUser(), s3Bucket.getRegion(),
                        fileId, newProperties, version.getMajorVersion(),
                        version.getMinorVersion());
            }
            S3ObjectMeta s3ObjMeta = FileMappingUtil.buildS3ObjectMeta(bucketName,
                    fileMeta.toBSONObject());
            if (!s3Bucket.getVersionStatus().equals(ScmBucketVersionStatus.Disabled.name())) {
                return s3ObjMeta.getVersionId();
            }
            return null;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "object not exist: bucket=" + bucketName + ", object=" + objectName, e);
            }
            if (e.getError() == ScmError.FILE_INVALID_CUSTOMTAG) {
                throw new S3ServerException(S3Error.OBJECT_INVALID_TAGGING, e.getMessage(), e);
            }
            if (e.getError() == ScmError.FILE_CUSTOMTAG_TOO_LARGE) {
                throw new S3ServerException(S3Error.OBJECT_TAGGING_TOO_LARGE, e.getMessage(), e);
            }
            throw new S3ServerException(S3Error.OBJECT_PUT_TAGGING_FIELD,
                    "failed to set object tagging: bucket=" + bucketName + ", object=" + objectName,
                    e);
        }
    }

    @Override
    public ObjectTagResult getObjectTag(ScmSession session, String bucketName,
            String objectName, String versionId) throws S3ServerException {
        try {
            Bucket s3Bucket = bucketService.getBucket(session, bucketName);
            BSONObject fileInfo = getFileInfo(session, bucketName, objectName, versionId);
            S3ObjectMeta s3ObjMeta = FileMappingUtil.buildS3ObjectMeta(bucketName, fileInfo);
            ObjectTagResult ret = new ObjectTagResult();
            if (!s3Bucket.getVersionStatus().equals(ScmBucketVersionStatus.Disabled.name())) {
                ret.setVersionId(s3ObjMeta.getVersionId());
            }
            BSONObject customTag = BsonUtils.getBSON(fileInfo, FieldName.FIELD_CLFILE_CUSTOM_TAG);
            if (customTag != null) {
                ret.setTagging(customTag.toMap());
            }
            return ret;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "object not exist: bucket=" + bucketName + ", object=" + objectName, e);
            }
            throw new S3ServerException(S3Error.OBJECT_GET_TAGGING_FIELD,
                    "failed to get object tagging: bucket=" + bucketName + ", object=" + objectName,
                    e);
        }
    }

    @Override
    public String deleteObjectTag(ScmSession session, String bucketName, String objectName,
            String versionId) throws S3ServerException {
        try {
            Bucket s3Bucket = bucketService.getBucket(session, bucketName);
            String fileId = scmBucketService.getFileId(session.getUser(), bucketName, objectName);
            BSONObject newProperties = new BasicBSONObject();
            newProperties.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, new BasicBSONObject());
            FileMeta fileMeta = null;
            if (versionId == null) {
                fileMeta = fileService.updateFileInfo(session.getUser(), s3Bucket.getRegion(),
                        fileId, newProperties, -1, -1);
            }
            else {
                ScmVersion version = VersionUtil.parseVersion(versionId);
                fileMeta = fileService.updateFileInfo(session.getUser(), s3Bucket.getRegion(),
                        fileId, newProperties, version.getMajorVersion(),
                        version.getMinorVersion());
            }
            S3ObjectMeta s3ObjMeta = FileMappingUtil.buildS3ObjectMeta(bucketName,
                    fileMeta.toBSONObject());
            if (!s3Bucket.getVersionStatus().equals(ScmBucketVersionStatus.Disabled.name())) {
                return s3ObjMeta.getVersionId();
            }
            return null;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "object not exist: bucket=" + bucketName + ", object=" + objectName, e);
            }
            throw new S3ServerException(S3Error.OBJECT_DELETE_TAGGING_FIELD,
                    "failed to delete object tagging: bucket=" + bucketName + ", object="
                            + objectName,
                    e);
        }
    }
}

class ScmFileInputStreamAdapter extends InputStream {

    private final BSONObject fileInfo;
    private FileReaderDao reader;
    private long readLen = 0;
    private long maxReadLen;

    public ScmFileInputStreamAdapter(ScmSession session, IFileService fileService, String ws,
            BSONObject fileInfo, long start, long len) throws ScmServerException {
        int readFlag = 0;
        maxReadLen = len;
        this.fileInfo = fileInfo;
        if (start > 0) {
            readFlag = CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK;
        }
        reader = fileService.downloadFile(session.getSessionId(), session.getUserDetail(), ws,
                fileInfo, readFlag);
        reader.seek(start);
    }

    @Override
    public int read() throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        long remain = maxReadLen - readLen;
        if (remain <= 0) {
            return -1;
        }
        int currentReadLen;
        if (len > remain) {
            currentReadLen = (int) remain;
        }
        else {
            currentReadLen = len;
        }

        try {
            int ret = reader.read(b, off, currentReadLen);
            readLen += ret;
            return ret;
        }
        catch (ScmServerException e) {
            throw new IOException(
                    "failed to read data: ws= " + reader.getWsName() + ", fileInfo=" + fileInfo, e);
        }
    }

    @Override
    public void close() {
        reader.close();
    }
}
