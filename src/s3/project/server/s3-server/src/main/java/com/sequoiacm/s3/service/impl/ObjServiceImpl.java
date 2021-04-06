package com.sequoiacm.s3.service.impl;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.context.S3ListObjContext;
import com.sequoiacm.s3.context.S3ListObjContextMeta;
import com.sequoiacm.s3.context.S3ListObjContextMgr;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.core.ObjectMeta;
import com.sequoiacm.s3.core.Range;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.Batch;
import com.sequoiacm.s3.model.CopyObjectResult;
import com.sequoiacm.s3.model.GetResult;
import com.sequoiacm.s3.model.InputStreamWithCalc;
import com.sequoiacm.s3.model.ListObjRecord;
import com.sequoiacm.s3.model.ListObjectsResult;
import com.sequoiacm.s3.model.ListObjectsResultV1;
import com.sequoiacm.s3.model.ListVersionsResult;
import com.sequoiacm.s3.model.ObjectMatcher;
import com.sequoiacm.s3.model.ObjectUri;
import com.sequoiacm.s3.model.PutDeleteResult;
import com.sequoiacm.s3.model.ScmDirPath;
import com.sequoiacm.s3.model.Version;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.remote.ScmContentServerClient;
import com.sequoiacm.s3.remote.ScmFileInfo;
import com.sequoiacm.s3.service.BucketService;
import com.sequoiacm.s3.service.ObjectService;
import com.sequoiacm.s3.utils.CommonUtil;
import com.sequoiacm.s3.utils.DataFormatUtils;

@Component
public class ObjServiceImpl implements ObjectService {
    private static final Logger logger = LoggerFactory.getLogger(ObjServiceImpl.class);
    @Autowired
    private S3ListObjContextMgr contextMgr;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private ScmClientFactory clientFactory;

    @Override
    public PutDeleteResult putObject(ScmSession session, ObjectMeta meta, InputStream inputStream)
            throws S3ServerException {
        CommonUtil.checkKey(meta.getKey());
        Bucket bucket = bucketService.getBucket(session, meta.getBucketName());
        ScmContentServerClient client = clientFactory.getContentServerClient(session,
                bucket.getWorkspace());
        InputStreamWithCalc is = new InputStreamWithCalc(inputStream);
        String breakFileName = UUID.randomUUID().toString();
        logger.debug("creating scm breakpointfile() for object()", breakFileName, meta.getKey());
        client.createBreakpointFile(breakFileName, is);
        try {
            if (null != meta.getMd5() && !meta.getMd5().equals(is.getMd5())) {
                throw new S3ServerException(S3Error.OBJECT_BAD_DIGEST,
                        "The Content-MD5 you specified does not match what we received.");
            }
            meta.setEtag(is.getEtag());
            if (meta.getSize() != is.getLength()) {
                throw new S3ServerException(S3Error.OBJECT_INCOMPLETE_BODY, "content length is "
                        + meta.getSize() + " and receive " + is.getLength() + " bytes");
            }

            // 使用断点文件上传除了要计算MD5外， 还有就是创建SCM文件需要重试（目录不存在） 
            try {
                client.createScmFileWithOverwrite(breakFileName, bucket.getBucketDir(), meta);
            }
            catch (ScmFeignException e) {
                throw new S3ServerException(S3Error.OBJECT_PUT_FAILED,
                        "failed to create scm file: obj=" + meta.getKey() + ", bucket="
                                + bucket.getBucketName(),
                        e);
            }
        }
        catch (Exception e) {

            client.deleteBreakpointFileSilence(breakFileName);
            throw e;
        }
        PutDeleteResult res = new PutDeleteResult();
        res.seteTag(is.getEtag());
        res.setVersionId(ObjectMeta.NULL_VERSION_ID);
        return res;
    }

    public ObjectMeta getObjectMeta(ScmSession session, Bucket bucket, String objectName,
            Long versionId, boolean isNoVersion, ObjectMatcher matchers) throws S3ServerException {
        try {
            CommonUtil.checkKey(objectName);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                    "no such key. objectName:" + objectName, e);
        }
        ScmContentServerClient client = clientFactory.getContentServerClient(session,
                bucket.getRegion());
        String scmFilePath = CommonUtil.concatPath(bucket.getBucketDir(), objectName);
        ScmFileInfo scmFile = null;
        try {
            scmFile = client.getFile(scmFilePath);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()
                    || e.getStatus() == ScmError.FILE_NOT_FOUND.getErrorCode()) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "no such key. objectName:" + objectName, e);
            }
            throw new S3ServerException(S3Error.OBJECT_GET_FAILED,
                    "failed to get scm file:ws=" + bucket.getRegion() + ", file=" + scmFilePath, e);
        }
        if (!isNoVersion) {
            throw new S3ServerException(S3Error.OBJECT_NO_SUCH_VERSION,
                    "no such version. objectName:" + objectName + ",version:" + versionId);
        }
        ObjectMeta ret = new ObjectMeta();
        ret.setScmFileId(scmFile.getId());
        ret.setBucketName(bucket.getBucketName());
        ret.setCacheControl(scmFile.getCustomMetaCacheControl());
        ret.setContentDisposition(scmFile.getCustomMetaContentDisposition());
        ret.setContentEncoding(scmFile.getCustomMetaEnconde());
        ret.setContentLanguage(scmFile.getCustomMetaContentLanguage());
        ret.setContentType(scmFile.getMimeType());
        ret.setDeleteMarker(false);
        ret.setEtag(scmFile.getCustomMetaEtag());
        ret.setExpires(scmFile.getCustomMetaExpires());
        ret.setKey(objectName);
        ret.setLastModified(scmFile.getUpdateTime());
        ret.setMetaList(scmFile.getCustomMetaMetaList());
        ret.setNoVersionFlag(true);
        ret.setSize(scmFile.getSize());

        match(matchers, ret);

        return ret;
    }

    @Override
    public ObjectMeta getObjectMeta(ScmSession session, String bucketName, String objectName,
            Long versionId, boolean isNoVersion, ObjectMatcher matchers) throws S3ServerException {
        Bucket bucket = bucketService.getBucket(session, bucketName);
        return getObjectMeta(session, bucket, objectName, versionId, isNoVersion, matchers);
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

    public static Date parseDate(String dateString) throws S3ServerException {
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

    private void match(ObjectMatcher matchers, ObjectMeta objectMeta) throws S3ServerException {
        String eTag = objectMeta.geteTag();
        long lastModifiedTime = objectMeta.getLastModified();
        boolean isMatch = false;
        boolean isNoneMatch = false;

        String matchEtag = matchers.getIfMatch();
        if (null != matchEtag) {
            String matchEtagString = trimQuotes(matchEtag);
            if (!matchEtagString.equals(eTag)) {
                throw new S3ServerException(S3Error.OBJECT_IF_MATCH_FAILED,
                        "if-match failed: if-match value:" + matchEtag.toString()
                                + ", current object eTag:" + eTag);
            }
            isMatch = true;
        }

        String noneMatchEtag = matchers.getIfNoneMatch();
        if (null != noneMatchEtag) {
            String noneMatchEtagString = trimQuotes(noneMatchEtag);
            if (noneMatchEtagString.equals(eTag)) {
                throw new S3ServerException(S3Error.OBJECT_IF_NONE_MATCH_FAILED,
                        "if-none-match failed: if-none-match value:" + noneMatchEtag.toString()
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
                                    + unModifiedSince.toString()
                                    + ", current object lastModifiedTime:"
                                    + new Date(lastModifiedTime));
                }
            }
        }

        Object modifiedSince = matchers.getIfModifiedSince();
        if (null != modifiedSince) {
            Date date = parseDate(modifiedSince.toString());
            if (getSecondTime(date.getTime()) >= getSecondTime(lastModifiedTime)) {
                if (!isNoneMatch) {
                    throw new S3ServerException(S3Error.OBJECT_IF_MODIFIED_SINCE_FAILED,
                            "if-modified-since failed: if-modified-since value:"
                                    + modifiedSince.toString()
                                    + ", current object lastModifiedTime:"
                                    + new Date(lastModifiedTime));
                }
            }
        }
    }

    @Override
    public GetResult getObject(ScmSession session, String bucketName, String objectName,
            Long versionId, boolean isNoVersion, ObjectMatcher matcher, Range range)
            throws S3ServerException {
        Bucket bucket = bucketService.getBucket(session, bucketName);
        ObjectMeta obj = getObjectMeta(session, bucket, objectName, versionId, isNoVersion,
                matcher);
        ScmContentServerClient client = clientFactory.getContentServerClient(session,
                bucket.getRegion());

        try {
            InputStream is = null;
            if (range == null) {
                is = client.downloadScmFile(obj.getScmFileId(), 0,
                        CommonDefine.File.UNTIL_END_OF_FILE);
            }
            else {
                CommonUtil.analyseRangeWithFileSize(range, obj.getSize());
                is = client.downloadScmFile(obj.getScmFileId(), range.getStart(),
                        range.getContentLength());
            }
            return new GetResult(obj, is);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.FILE_NOT_FOUND.getErrorCode()) {
                throw new S3ServerException(S3Error.OBJECT_NO_SUCH_KEY,
                        "no such key. objectName:" + objectName, e);
            }
            throw new S3ServerException(S3Error.OBJECT_GET_FAILED, "failed to get scm file:ws="
                    + bucket.getRegion() + ", file=" + obj.getScmFileId(), e);
        }
    }

    @Override
    public CopyObjectResult copyObject(ScmSession session, ObjectMeta dest, ObjectUri sourceUri,
            ObjectMatcher matcher, boolean directiveCopy) throws S3ServerException {
        CommonUtil.checkKey(dest.getKey());
        Bucket bucket = bucketService.getBucket(session, dest.getBucketName());
        CopyObjectResult copyObjectResult = new CopyObjectResult();

        // get source object meta
        ObjectMeta sourceMeta = getObjectMeta(session, sourceUri.getBucketName(),
                sourceUri.getObjectName(), sourceUri.getVersionId(), sourceUri.isNoVersion(),
                matcher);

        // check if itself change, 参照s3顺序，先判断对象合法存在
        if (dest.getBucketName().equals(sourceUri.getBucketName())
                && dest.getKey().equals(sourceUri.getObjectName()) && sourceUri.isNoVersion()) {
            if (directiveCopy) {
                throw new S3ServerException(S3Error.OBJECT_COPY_WITHOUT_CHANGE,
                        "copy an object to itself without changing the object's metadata.");
            }
        }

        sourceMeta.setBucketName(dest.getBucketName());
        sourceMeta.setKey(dest.getKey());

        if (!directiveCopy) {
            // merge basic metadata from sourceMeta to dest
            mergeObjectMeta(sourceMeta, dest);
            if (dest.getMetaListlength() > RestParamDefine.X_AMZ_META_LENGTH) {
                throw new S3ServerException(S3Error.OBJECT_METADATA_TOO_LARGE,
                        "metadata headers exceed the maximum. xMeta:" + sourceMeta.getMetaList());
            }
        }
        else {
            dest = sourceMeta;
        }

        ScmContentServerClient client = clientFactory.getContentServerClient(session,
                bucket.getWorkspace());
        InputStream is = null;
        ScmFileInfo scmFile = null;
        String breakFileName = null;
        try {
            is = client.downloadScmFile(sourceMeta.getScmFileId(), 0,
                    CommonDefine.File.UNTIL_END_OF_FILE);
            breakFileName = UUID.randomUUID().toString();
            logger.debug("creating scm breakpointfile() for object()", breakFileName,
                    dest.getKey());
            client.createBreakpointFile(breakFileName, is);
            scmFile = client.createScmFileWithOverwrite(breakFileName, bucket.getBucketDir(), dest);
        }
        catch (Exception e) {
            client.deleteBreakpointFileSilence(breakFileName);
            throw new S3ServerException(S3Error.OBJECT_COPY_FAILED,
                    "failed to create scm file: obj=" + dest.getKey() + ", bucket="
                            + bucket.getBucketName(),
                    e);
        }
        finally {
            CommonUtil.closeResource(is);
        }
        copyObjectResult.seteTag(sourceMeta.geteTag());
        copyObjectResult.setLastModified(DataFormatUtils.formatDate(scmFile.getUpdateTime()));
        return copyObjectResult;
    }

    private void mergeObjectMeta(ObjectMeta sourceMeta, ObjectMeta dest) {
        Class objectMetaClass = sourceMeta.getClass();
        Field[] fields = objectMetaClass.getDeclaredFields();
        // 复制sourceMeta所有的基本属性到dest中（不包括metaListlength、metaList）
        for (Field field : fields) {
            String fieldName = field.getName();
            if (fieldName.equals("metaListlength") || fieldName.equals("metaList")) {
                continue;
            }
            // 是否可以访问
            boolean flag = field.isAccessible();
            try {
                field.setAccessible(true);
                field.set(dest, field.get(sourceMeta));
            }
            catch (IllegalAccessException e) {
                logger.info("convert the value fail, cause by:", e);
            }
            // 还原访问权限
            field.setAccessible(flag);
        }
    }

    @Override
    public PutDeleteResult deleteObject(ScmSession session, String bucketName, String objectName)
            throws S3ServerException {
        try {
            CommonUtil.checkKey(objectName);
        }
        catch (Exception e) {
            logger.info("invalid object key, assume not exist:" + objectName, e);
            return null;
        }

        Bucket bucket = bucketService.getBucket(session, bucketName);
        ScmContentServerClient client = clientFactory.getContentServerClient(session,
                bucket.getWorkspace());
        try {
            ScmFileInfo file = client
                    .deleteFile(CommonUtil.concatPath(bucket.getBucketDir(), objectName));
            if (file != null) {
                try {
                    removeEmptyParentDir(bucket, client, objectName);
                }
                catch (Exception e) {
                    logger.warn("failed to remove obj parent dir:obj={}" + objectName, e);
                }
            }
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.OBJECT_DELETE_FAILED,
                    "failed to delete obj:bucket=" + bucketName + ", obj=" + objectName, e);
        }
        // obj no version, return null
        return null;

    }

    private void removeEmptyParentDir(Bucket bucket, ScmContentServerClient client,
            String objectName) throws ScmFeignException, S3ServerException {
        ScmDirPath subPathInBucket = new ScmDirPath(objectName);
        for (int i = subPathInBucket.getLevel() - 1; i >= 2; i--) {
            boolean deleted = client.deleteDirIfEmpty(CommonUtil.concatPath(bucket.getBucketDir(),
                    subPathInBucket.getPathByLevel(i)));
            if (!deleted) {
                break;
            }
        }
    }

    @Override
    public PutDeleteResult deleteObject(ScmSession session, String bucketName, String objectName,
            Long versionId, boolean isNoVersion) throws S3ServerException {
        if (isNoVersion) {
            deleteObject(session, bucketName, objectName);
        }
        return null;
    }

    @Override
    public ListObjectsResultV1 listObjectsV1(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, Integer maxKeys, String encodingType)
            throws S3ServerException {
        CommonUtil.checkStartAfter(startAfter);
        ListObjectsResultV1 listObjectsResult = new ListObjectsResultV1(bucketName, maxKeys,
                encodingType, prefix, startAfter, delimiter);
        if (maxKeys == 0) {
            return listObjectsResult;
        }

        if (!CommonUtil.isValidPrefix(prefix)) {
            return listObjectsResult;
        }
        Bucket bucket = bucketService.getBucket(session, bucketName);
        S3ListObjContext context = contextMgr.createContext(session, bucketName,
                bucket.getWorkspace(), bucket.getBucketDir(), prefix, startAfter, delimiter);
        Batch batch = context.query(maxKeys, true, encodingType);
        listObjectsResult.setCommonPrefixList(batch.getCmPrefix());
        listObjectsResult.setContentList(batch.getContent());
        if (context.hasMore()) {
            listObjectsResult.setIsTruncated(true);
            listObjectsResult.setNextMarker(context.getLastMarker());
        }
        return listObjectsResult;
    }

    @Override
    public ListVersionsResult listVersions(ScmSession session, String bucketName, String prefix,
            String delimiter, String keyMarker, String versionIdMarker, Integer maxKeys,
            String encodingType) throws S3ServerException {
        CommonUtil.checkStartAfter(keyMarker);
        ;
        Bucket bucket = bucketService.getBucket(session, bucketName);
        ListVersionsResult listVersionsResult = new ListVersionsResult(bucketName, maxKeys,
                encodingType, prefix, delimiter, keyMarker, versionIdMarker);
        if (maxKeys == 0) {
            return listVersionsResult;
        }
        if (!CommonUtil.isValidPrefix(prefix)) {
            return listVersionsResult;
        }

        S3ListObjContext context = contextMgr.createContext(session, bucketName,
                bucket.getWorkspace(), bucket.getBucketDir(), prefix, keyMarker, delimiter);
        Batch batch = context.query(maxKeys, true, encodingType);
        listVersionsResult.setCommonPrefixList(batch.getCmPrefix());
        if (context.hasMore()) {
            listVersionsResult.setIsTruncated(true);
            listVersionsResult.setNextKeyMarker(context.getLastMarker());
            listVersionsResult.setNextVersionIdMarker(null);
        }
        List<ListObjRecord> contents = batch.getContent();
        List<Version> versions = new ArrayList<>();
        for (ListObjRecord content : contents) {
            versions.add(new Version(content));
        }
        listVersionsResult.setVersionList(versions);

        return listVersionsResult;
    }

    @Override
    public ListObjectsResult listObjectsV2(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, Integer maxKeys, String continueToken,
            String encodingType, boolean fetchOwner) throws S3ServerException {
        CommonUtil.checkStartAfter(startAfter);
        ListObjectsResult listObjectsResult = new ListObjectsResult(bucketName, maxKeys,
                encodingType, prefix, startAfter, delimiter, continueToken);
        if (maxKeys == 0) {
            return listObjectsResult;
        }
        if (!CommonUtil.isValidPrefix(prefix)) {
            return listObjectsResult;
        }
        S3ListObjContext context = null;
        if (continueToken != null) {
            context = contextMgr.getContext(session, continueToken);
            if (!isContextMatch(context, prefix, startAfter, delimiter)) {
                context = null;
            }
        }
        if (context == null) {
            Bucket bucket = bucketService.getBucket(session, bucketName);
            context = contextMgr.createContext(session, bucketName, bucket.getWorkspace(),
                    bucket.getBucketDir(), prefix, startAfter, delimiter);
        }
        Batch batch = context.query(maxKeys, fetchOwner, encodingType);
        listObjectsResult.setKeyCount(batch.getCount());
        listObjectsResult.setCommonPrefixList(batch.getCmPrefix());
        listObjectsResult.setContentList(batch.getContent());
        if (context.hasMore()) {
            listObjectsResult.setIsTruncated(true);
            listObjectsResult.setNextContinueToken(context.getMeta().getId());
            context.save();
        }
        else if (!context.isNewContext()) {
            context.release();
        }
        return listObjectsResult;
    }

    private boolean isContextMatch(S3ListObjContext context, String prefix, String startAfter,
            String delimiter) {
        S3ListObjContextMeta contextMeta = context.getMeta();
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
    public boolean isEmptyBucket(ScmSession session, Bucket bucket) throws S3ServerException {
        ListObjectsResultV1 result = listObjectsV1(session, bucket.getBucketName(), null, null,
                null, 1, null);
        return result.getCommonPrefixList().size() + result.getContentList().size() <= 0;
    }

}
