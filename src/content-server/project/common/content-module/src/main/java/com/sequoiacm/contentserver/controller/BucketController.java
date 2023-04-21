package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.model.ClientUploadConf;
import com.sequoiacm.contentserver.model.SessionInfoWrapper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.contentserver.service.impl.Converter;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.dropwizard.DropwizardMetricServices;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BucketController {
    @Autowired
    private DropwizardMetricServices metricServices;
    @Autowired
    private IScmBucketService service;

    @PostMapping("/buckets")
    public ScmBucket createBucket(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String ws,
            @RequestParam(CommonDefine.RestArg.BUCKET_NAME) String name, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        return service.createBucket(user, ws, name);
    }

    @DeleteMapping("/buckets/{name}")
    public void deleteBucket(@PathVariable("name") String name, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        service.deleteBucket(user, name);
    }

    @GetMapping(value = "/buckets/{name}", params = "action=get_bucket_by_name")
    public ScmBucket getBucket(@PathVariable("name") String name, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        return service.getBucket(user, name);
    }

    @GetMapping(value = "/buckets/{id}", params = "action=get_bucket_by_id")
    public ScmBucket getBucket(@PathVariable("id") long id, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        return service.getBucket(user, id);
    }

    @GetMapping(path = "/buckets", params = "action=count_bucket")
    public ResponseEntity countBucket(
            @RequestParam(value = CommonDefine.RestArg.FILTER, required = false, defaultValue = "{}") BSONObject condition,
            Authentication auth, HttpServletResponse resp) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        long count = service.countBucket(user, condition);
        return ResponseEntity.ok().header(CommonDefine.RestArg.X_SCM_COUNT, count + "").build();
    }

    @GetMapping("/buckets")
    public void getBuckets(
            @RequestParam(value = CommonDefine.RestArg.FILTER, required = false, defaultValue = "{}") BSONObject condition,
            @RequestParam(value = CommonDefine.RestArg.ORDER_BY, required = false, defaultValue = "{}") BSONObject orderBy,
            @RequestParam(value = CommonDefine.RestArg.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.LIMIT, required = false, defaultValue = "-1") long limit,
            Authentication auth, HttpServletResponse resp) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        final ScmObjectCursor<ScmBucket> cursor = service.listBucket(user, condition, orderBy, skip,
                limit);
        PrintWriter writer = null;
        try {
            writer = ServiceUtils.getWriter(resp);
        }
        catch (Exception e) {
            cursor.close();
            throw e;
        }
        ServiceUtils.putCursorToWriter(cursor, new Converter<ScmBucket>() {
            @Override
            public String toJSON(ScmBucket b) {
                BasicBSONObject ret = new BasicBSONObject();
                ret.put(FieldName.Bucket.ID, b.getId());
                ret.put(FieldName.Bucket.NAME, b.getName());
                ret.put(FieldName.Bucket.WORKSPACE, b.getWorkspace());
                ret.put(FieldName.Bucket.CREATE_USER, b.getCreateUser());
                ret.put(FieldName.Bucket.CREATE_TIME, b.getCreateTime());
                ret.put(FieldName.Bucket.VERSION_STATUS, b.getVersionStatus().name());
                ret.put(FieldName.Bucket.CUSTOM_TAG, b.getCustomTag());
                ret.put(FieldName.Bucket.UPDATE_TIME, b.getUpdateTime());
                ret.put(FieldName.Bucket.UPDATE_USER, b.getUpdateUser());
                return ret.toString();
            }
        }, writer);
    }

    @PostMapping(value = "/buckets/{bucket_name}/files", params = "action=create_file")
    public BSONObject createFile(@PathVariable("bucket_name") String bucketName,
            @RequestHeader(value = CommonDefine.RestArg.FILE_DESCRIPTION) String fileInfoStr,
            @RequestParam(value = CommonDefine.RestArg.FILE_BREAKPOINT_FILE, required = false) String breakpointFileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_UPLOAD_CONFIG, required = false) BSONObject uploadConfig,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth,
            HttpServletRequest request) throws ScmServerException, IOException {
        ScmBucket bucket = service.getBucket(bucketName);

        ScmUser user = (ScmUser) auth.getPrincipal();
        // statistical traffic
        incrementTraffic(CommonDefine.Metrics.PREFIX_FILE_UPLOAD + bucket.getWorkspace());

        BSONObject description = (BSONObject) JSON.parse(RestUtils.urlDecode(fileInfoStr));
        FileMeta fileMeta = FileMeta.fromUser(bucket.getWorkspace(), description,
                user.getUsername());

        ClientUploadConf clientUploadConf = new ClientUploadConf(
                BsonUtils.getBooleanOrElse(uploadConfig, CommonDefine.RestArg.FILE_IS_OVERWRITE,
                        false),
                BsonUtils.getBooleanOrElse(uploadConfig, CommonDefine.RestArg.FILE_IS_NEED_MD5,
                        true));

        FileMeta createdFile;
        InputStream is = request.getInputStream();
        try {
            if (null != breakpointFileName) {
                createdFile = service.createFile(user, bucketName, fileMeta, breakpointFileName,
                        clientUploadConf.isOverwrite());
            }
            else {
                createdFile = service.createFile(user, bucketName, fileMeta, is,
                        clientUploadConf.isOverwrite());
            }
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }

        return createdFile.toBSONObject();
    }

    @PostMapping(path = "/buckets/{name}/files", params = "action=detach_file")
    public void detachFile(@PathVariable("name") String bucketName,
            @RequestParam(CommonDefine.RestArg.FILE_NAME) String fileName, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        service.detachFile(user, bucketName, fileName);
    }

    @PostMapping(path = "/buckets/{name}/files", params = "action=attach_file")
    public List<ScmBucketAttachFailure> attachFile(@PathVariable("name") String bucketName,
            @RequestParam(CommonDefine.RestArg.FILE_ID_LIST) List<String> fileIdList,
            @RequestParam(value = CommonDefine.RestArg.ATTACH_KEY_TYPE, required = false, defaultValue = "FILE_NAME") ScmBucketAttachKeyType type,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        return service.attachFile(user, bucketName, fileIdList, type);
    }

    @GetMapping(path = "/buckets/{name}/files", params = "action=list_file")
    public void listFile(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, required = false) Integer scope,
            @RequestParam(value = CommonDefine.RestArg.FILTER, required = false, defaultValue = "{}") BSONObject condition,
            @RequestParam(value = CommonDefine.RestArg.ORDER_BY, required = false, defaultValue = "{}") BSONObject orderby,
            @RequestParam(value = CommonDefine.RestArg.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse resp, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        boolean isResContainsDeleteMarker;
        if (scope == null) {
            // 兼容旧接口（请求参数不带 scope）: 1. 默认为列取文件的最新版本 2. 返回结果需要携带 deleteMarker
            scope = CommonDefine.Scope.SCOPE_CURRENT;
            isResContainsDeleteMarker = true;
        }
        else {
            // 新接口需要做条件参数的有效性校验
            try {
                ScmArgChecker.File.checkBucketFileMatcher(condition);
                ScmArgChecker.File.checkBucketFileOrderBy(orderby);
            }
            catch (InvalidArgumentException e) {
                throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
            }
            isResContainsDeleteMarker = ScmSystemUtils.isDeleteMarkerRequired(scope);
        }

        MetaCursor cursor = service.listFile(user, bucketName, scope, condition, null, orderby,
                skip, limit, isResContainsDeleteMarker);
        resp.setHeader("Content-Type", "application/json;charset=utf-8");
        ServiceUtils.putCursorToResponse(cursor, resp);
    }

    @GetMapping(path = "/buckets/{name}/files", params = "action=count_file")
    public ResponseEntity countFile(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, required = false) Integer scope,
            @RequestParam(value = CommonDefine.RestArg.FILTER, required = false, defaultValue = "{}") BSONObject condition,
            HttpServletResponse resp, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        boolean isResContainsDeleteMarker;
        if (scope == null) {
            // 兼容旧接口（请求参数不带 scope）: 1. 默认为列取文件的最新版本 2. 返回结果需要携带 deleteMarker
            scope = CommonDefine.Scope.SCOPE_CURRENT;
            isResContainsDeleteMarker = true;
        }
        else {
            // 新接口需要做条件参数的有效性校验
            try {
                ScmArgChecker.File.checkBucketFileMatcher(condition);
            }
            catch (InvalidArgumentException e) {
                throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
            }
            isResContainsDeleteMarker = ScmSystemUtils.isDeleteMarkerRequired(scope);
        }

        long count = service.countFile(user, bucketName, scope, condition,
                isResContainsDeleteMarker);
        resp.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok().header(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count))
                .build();
    }

    @PutMapping(path = "/buckets/{name}", params = "action=update_version_status")
    public ScmBucket updateBucketVersionStatus(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.BUCKET_VERSION_STATUS) String versionStatus,
            HttpServletResponse resp, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();

        ScmBucketVersionStatus bucketVersionStatus = ScmBucketVersionStatus.parse(versionStatus);
        if (bucketVersionStatus == null) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                    "invalid bucket version status:" + versionStatus);
        }
        return service.updateBucketVersionStatus(user, bucketName, bucketVersionStatus);
    }

    @DeleteMapping(path = "/buckets/{name}/files", params = "action=delete_file")
    public BSONObject deleteFile(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_NAME) String fileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_IS_PHYSICAL, required = false, defaultValue = "false") boolean isPhysical,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        FileMeta ret = service.deleteFile(user, bucketName, fileName, isPhysical,
                new SessionInfoWrapper(sessionId, userDetail));
        if (ret != null) {
            return ret.toBSONObject();
        }
        return null;
    }

    @DeleteMapping(path = "/buckets/{name}/files", params = "action=delete_file_version")
    public void deleteFileVersion(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_NAME) String fileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, defaultValue = "-1") int majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, defaultValue = "-1") int minorVersion,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        service.deleteFileVersion(user, bucketName, fileName, majorVersion, minorVersion);
    }

    @GetMapping(path = "/buckets/{name}/files", params = "action=get_file")
    public BSONObject getFile(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_NAME) String fileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, defaultValue = "-1") int majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, defaultValue = "-1") int minorVersion,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject file = service.getFileVersion(user, bucketName, fileName, majorVersion,
                minorVersion);
        if (file == null) {
            throw new ScmFileNotFoundException(
                    "file not exist:bucket=" + bucketName + ",fileName=" + fileName + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        if (isDeleteMarker(file)) {
            throw new ScmFileNotFoundException("the specify version is delete marker:bucket="
                    + bucketName + ",fileName=" + fileName + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }
        return file;
    }

    private boolean isDeleteMarker(BSONObject file) {
        return BsonUtils.getBooleanOrElse(file, FieldName.FIELD_CLFILE_DELETE_MARKER, false);
    }

    @GetMapping(path = "/buckets/{name}/files", params = "action=get_file_null_version")
    public BSONObject getNullVersionFile(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_NAME) String fileName,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject file = service.getFileNullVersion(user, bucketName, fileName);
        if (file == null) {
            throw new ScmFileNotFoundException(
                    "file not exist:bucket=" + bucketName + ",fileName=" + fileName);
        }

        if (isDeleteMarker(file)) {
            throw new ScmFileNotFoundException("the specify version is delete marker:bucket="
                    + bucketName + ",fileName=" + fileName);
        }
        return file;

    }

    // statistical traffic
    private void incrementTraffic(String namePrefix) {
        Date today = new Date();
        metricServices.increment(namePrefix + "." + CommonHelper.getCurrentDay(today));
        // remove 30 days ago records
        metricServices.reset(namePrefix + "." + CommonHelper.getDateBeforeDays(today, 30));
    }

    @PostMapping(path = "/buckets/{name}/tags")
    public void setBucketTag(@PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.BUCKET_CUSTOM_TAG) Map<String, String> customTag,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        service.setBucketTag(user, bucketName, customTag);
    }

    @DeleteMapping(path = "/buckets/{name}/tags")
    public void deleteBucketTag(@PathVariable("name") String bucketName,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        service.deleteBucketTag(user, bucketName);
    }
}
