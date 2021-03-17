package com.sequoiacm.s3.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.s3.core.ScmFileInputStream;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpStatus;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.ObjectMeta;
import com.sequoiacm.s3.cursor.FileDirInfo;
import com.sequoiacm.s3.cursor.ScmFileDirCursor;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ScmDirPath;
import com.sequoiacm.s3.utils.CommonUtil;

import feign.Response;

public class ScmContentServerClient {
    private static final Logger logger = LoggerFactory.getLogger(ScmContentServerClient.class);
    private static final ScmFeignErrorDecoder errDecoder = new ScmFeignErrorDecoder();
    private static final BasicBSONObject ORDERBY_NAME = new BasicBSONObject(
            FieldName.FIELD_CLFILE_NAME, 1);
    private static final BSONObject UPLOAD_FILE_CONFIG = new BasicBSONObject(
            CommonDefine.RestArg.FILE_IS_OVERWRITE, true);
    private static final BSONObject FILE_SELECTOR = new BasicBSONObject();
    static {
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_ID, null);
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_NAME, null);
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, null);
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_FILE_SIZE, null);
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, null);
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, null);
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_INNER_USER, null);
        FILE_SELECTOR.put(FieldName.FIELD_CLFILE_PROPERTIES, null);
    }

    private static final String[] REGEX_SPECIAL_WORD = { "\\", "$", "(", ")", "*", "+", ".", "[",
            "]", "?", "^", "{", "}" };

    private static final String CHARSET_UTF8 = "utf-8";
    private static Map<String, String> CLASS_ID_CACHE = new ConcurrentHashMap<>();

    private ContenServerService service;
    private ScmSession session;
    private String ws;
    private CloseableHttpClient httpClient;
    private LoadBalancerClient loadlancer;
    private String site;

    public String getWs() {
        return ws;
    }

    public ScmContentServerClient(ScmSession session, String ws, String site, ScmFeignClient feign,
            CloseableHttpClient httpClient, LoadBalancerClient loadBalancerClient,
            ContenServerService csService) {
        this.site = site.toLowerCase();
        this.service = csService;
        this.ws = ws;
        this.session = session;
        this.httpClient = httpClient;
        this.loadlancer = loadBalancerClient;
    }

    public ScmDirInfo getDir(String path) throws ScmFeignException {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        Response resp = service.getDirInfoByPath(session.getSessionId(), session.getUserDetail(),
                ws, path);
        checkResponse("getDirInfoByPath", resp);
        String dirJson = resp.headers().get(CommonDefine.Directory.SCM_REST_ARG_DIRECTORY)
                .iterator().next();
        try {
            dirJson = URLDecoder.decode(dirJson, CHARSET_UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("failed to decode dirinfo:" + dirJson, e);
        }
        return new ScmDirInfo((BSONObject) JSON.parse(dirJson));
    }

    public ScmWsInfo getWorkspace(String ws) throws ScmFeignException {
        try {
            return service.getWorkspace(session.getSessionId(), session.getUserDetail(), ws);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.WORKSPACE_NOT_EXIST.getErrorCode()) {
                return null;
            }
            throw e;
        }
    }

    public List<ScmWsInfo> getWorkspaceList() throws ScmFeignException {
        return service.getWorkspace(session.getSessionId(), session.getUserDetail(), null, null, 0,
                -1);
    }

    public ScmWsInfo getWorkspaceById(int id) throws ScmFeignException {
        BSONObject filter = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_ID, id);
        List<ScmWsInfo> wsList = service.getWorkspace(session.getSessionId(),
                session.getUserDetail(), filter, null, 0, 1);
        if (wsList.size() > 0) {
            return wsList.get(0);
        }
        return null;
    }

    public List<ScmFileInfo> getFilesWithNamePrefix(String parentId, String namePrefix, int maxSize)
            throws ScmFeignException {
        BasicBSONObject regex = new BasicBSONObject("$regex", "^" + escapePrefix(namePrefix));
        BasicBSONObject condition = new BasicBSONObject(FieldName.FIELD_CLFILE_NAME, regex);
        return service.listFilesInDir(session.getSessionId(), session.getUserDetail(), parentId, ws,
                condition, maxSize, 0, ORDERBY_NAME, FILE_SELECTOR);
    }

    public List<ScmFileInfo> getFilesWithGtName(String parentId, String nameGreaterThan,
            int maxSize) throws ScmFeignException {
        BasicBSONObject regex = new BasicBSONObject("$gt", nameGreaterThan);
        BasicBSONObject condition = new BasicBSONObject(FieldName.FIELD_CLFILE_NAME, regex);
        return service.listFilesInDir(session.getSessionId(), session.getUserDetail(), parentId, ws,
                condition, maxSize, 0, ORDERBY_NAME, FILE_SELECTOR);
    }

    public ScmFileInfo getFile(String path) throws ScmFeignException {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        Response resp = service.getFileByPath(session.getSessionId(), session.getUserDetail(), path,
                ws);
        checkResponse("getFile", resp);
        String fileJson = resp.headers().get(CommonDefine.RestArg.FILE_RESP_FILE_INFO).iterator()
                .next();
        try {
            fileJson = URLDecoder.decode(fileJson, CHARSET_UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("failed to decode fileinfo:" + fileJson, e);
        }
        return new ScmFileInfo((BSONObject) JSON.parse(fileJson));
    }

    private String escapePrefix(String prefix) {
        for (String key : REGEX_SPECIAL_WORD) {
            if (prefix.contains(key)) {
                prefix = prefix.replace(key, "\\" + key);
            }
        }
        return prefix;
    }

    public List<ScmFileInfo> getFiles(String parentId, String namePrefix, String nameGreaterThean,
            int maxSize) throws ScmFeignException {
        // if (namePrefix == null && nameGreaterThean == null) {
        // throw new RuntimeException("invalid arg for this method");
        // }
        BasicBSONList andArr = new BasicBSONList();
        if (namePrefix != null && !namePrefix.isEmpty()) {
            BasicBSONObject regex = new BasicBSONObject("$regex", "^" + escapePrefix(namePrefix));
            andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_NAME, regex));
        }
        if (nameGreaterThean != null && !nameGreaterThean.isEmpty()) {
            BasicBSONObject gt = new BasicBSONObject("$gt", nameGreaterThean);
            andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_NAME, gt));
        }
        BasicBSONObject condition = new BasicBSONObject("$and", andArr);
        return service.listFilesInDir(session.getSessionId(), session.getUserDetail(), parentId, ws,
                condition, maxSize, 0, ORDERBY_NAME, FILE_SELECTOR);

    }

    public List<ScmDirInfo> getDirs(String parentId, String namePrefix, GreatThanOrEquals gtOrEq,
            int maxSize) throws ScmFeignException {
        BasicBSONList andArr = new BasicBSONList();
        if (namePrefix != null) {
            BasicBSONObject regex = new BasicBSONObject("$regex", "^" + escapePrefix(namePrefix));
            andArr.add(new BasicBSONObject(FieldName.FIELD_CLDIR_NAME, regex));
        }
        if (gtOrEq != null) {
            BasicBSONList orList = new BasicBSONList();
            if (gtOrEq.getGreaterThan() != null) {
                BasicBSONObject gt = new BasicBSONObject("$gt", gtOrEq.getGreaterThan());
                gt = new BasicBSONObject(FieldName.FIELD_CLDIR_NAME, gt);
                orList.add(gt);
            }
            if (gtOrEq.getEqualsList() != null && gtOrEq.getEqualsList().size() > 0) {
                BasicBSONObject in = new BasicBSONObject("$in", gtOrEq.getEqualsList());
                in = new BasicBSONObject(FieldName.FIELD_CLDIR_NAME, in);
                orList.add(in);
            }
            BasicBSONObject or = new BasicBSONObject("$or", orList);
            andArr.add(or);
        }
        andArr.add(new BasicBSONObject(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId));

        BasicBSONObject condition = new BasicBSONObject("$and", andArr);

        List<ScmDirInfo> dirs = service.listDir(session.getSessionId(), session.getUserDetail(), ws,
                condition, maxSize, 0, ORDERBY_NAME);
        return dirs;
    }

    public List<ScmDirInfo> getDirsWithNamePrefix(String parentId, String namePrefix, int maxSize)
            throws ScmFeignException {
        BasicBSONObject regex = new BasicBSONObject("$regex", "^" + escapePrefix(namePrefix));
        BasicBSONObject condition = new BasicBSONObject(FieldName.FIELD_CLDIR_NAME, regex);
        condition.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId);
        List<ScmDirInfo> dirs = service.listDir(session.getSessionId(), session.getUserDetail(), ws,
                condition, maxSize, 0, ORDERBY_NAME);
        return dirs;
    }

    public List<ScmDirInfo> getDirsWithGtName(String parentId, String nameGreaterThan, int maxSize)
            throws ScmFeignException {
        BasicBSONObject regex = new BasicBSONObject("$gt", nameGreaterThan);
        BasicBSONObject condition = new BasicBSONObject(FieldName.FIELD_CLDIR_NAME, regex);
        condition.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId);
        List<ScmDirInfo> dirs = service.listDir(session.getSessionId(), session.getUserDetail(), ws,
                condition, maxSize, 0, ORDERBY_NAME);
        return dirs;
    }

    public ScmDirInfo createDirOrGet(String path) throws ScmFeignException {
        try {
            return createDir(path);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_EXIST.getErrorCode()) {
                return getDir(path);
            }
            throw e;
        }
    }

    public ScmDirInfo createDir(String path) throws ScmFeignException {
        return service.createDir(session.getSessionId(), session.getUserDetail(), ws, path);
    }

    public boolean deleteDirIfEmpty(String path) throws ScmFeignException {
        try {
            deleteDir(path);
            return true;
        }
        catch (ScmFeignException e) {
            if (e.getStatus() != ScmError.DIR_NOT_EMPTY.getErrorCode()) {
                throw e;
            }
            return false;
        }
    }

    public void deleteDirRecursive(String path) throws ScmFeignException, S3ServerException {
        if (deleteDirIfEmpty(path)) {
            return;
        }
        Stack<String> dirStack = new Stack<>();
        dirStack.push(path);
        while (!dirStack.isEmpty()) {
            path = dirStack.peek();
            //  /a   /a/b/f  /a/c/g
            ScmFileDirCursor c = new ScmFileDirCursor(this, path, null, null);
            while (c.hasNext()) {
                FileDirInfo f = c.getNext();
                if (!f.isDir()) {
                    throw new S3ServerException(S3Error.SCM_DELETE_DIR_FAILED,
                            "dir not empty:dir=" + path + ", file=" + f.getFullName());
                }
                if (!deleteDirIfEmpty(f.getFullName())) {
                    dirStack.push(f.getFullName());
                }
            }

            if (path == dirStack.peek()) {
                if (!deleteDirIfEmpty(path)) {
                    throw new S3ServerException(S3Error.SCM_DELETE_DIR_FAILED,
                            "failed to delete path, path is not empty:" + path);
                }
                dirStack.pop();
            }
        }
    }

    public void deleteDir(String path) throws ScmFeignException {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        try {
            service.deleteDirByPath(session.getSessionId(), session.getUserDetail(), ws, path);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() != ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw e;
            }
        }
    }

    public ScmFileInfo deleteFile(String path) throws ScmFeignException {
        ScmFileInfo file = null;
        try {
            file = getFile(path);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()
                    || e.getStatus() == ScmError.FILE_NOT_FOUND.getErrorCode()) {
                return null;
            }
            throw e;
        }

        try {
            service.deleteFile(session.getSessionId(), session.getUserDetail(), ws, file.getId(),
                    true);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() != ScmError.FILE_NOT_FOUND.getErrorCode()) {
                throw e;
            }
        }

        return file;

    }

    public ScmDirInfo getDirById(String id) throws ScmFeignException {
        Response resp = service.getDirInfoById(session.getSessionId(), session.getUserDetail(), ws,
                id);
        String dirJson = null;
        try {
            checkResponse("getDirInfoByPath", resp);
            dirJson = resp.headers().get(CommonDefine.Directory.SCM_REST_ARG_DIRECTORY).iterator()
                    .next();
        }
        finally {
            CommonUtil.closeResource(resp);
        }
        try {
            dirJson = URLDecoder.decode(dirJson, CHARSET_UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("failed to decode dirinfo:" + dirJson, e);
        }
        return new ScmDirInfo((BSONObject) JSON.parse(dirJson));
    }

    public void deleteDirById(String id) throws ScmFeignException {
        service.deleteDirById(session.getSessionId(), session.getUserDetail(), ws, id);
    }

    public static void checkResponse(String methodKey, Response response) throws ScmFeignException {
        if (response.status() >= 200 && response.status() < 300) {
            return;
        }

        Exception e = errDecoder.decode(methodKey, response);
        if (e instanceof ScmFeignException) {
            throw (ScmFeignException) e;
        }
        else {
            throw new ScmFeignException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    private ScmFileInfo overwriteFile(String breakFileName, BSONObject scmFileInfo)
            throws S3ServerException, ScmFeignException {
        int retryCount = 10;
        ScmFeignException lastException = null;
        while (retryCount-- > 0) {
            try {
                return service.uploadFile(session.getSessionId(), session.getUserDetail(), ws,
                        scmFileInfo, breakFileName, UPLOAD_FILE_CONFIG);
            }
            catch (ScmFeignException e) {
                if (e.getStatus() != ScmError.FILE_EXIST.getErrorCode()) {
                    throw e;
                }
                lastException = e;
            }
        }
        throw lastException;
    }

    private ScmFileInfo overwriteFileWithRetry(String breakFileName, BSONObject scmFileInfo)
            throws S3ServerException, ScmFeignException {
        try {
            return overwriteFile(breakFileName, scmFileInfo);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.METADATA_CLASS_NOT_EXIST.getErrorCode()) {
                CLASS_ID_CACHE.remove(ws);
                String classId = getS3ClassId();
                CLASS_ID_CACHE.put(ws, classId);
                scmFileInfo.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID, classId);
                return overwriteFile(breakFileName, scmFileInfo);
            }
            throw e;
        }
    }

    // 覆盖上传的方式写SCM文件
    public ScmFileInfo createScmFileWithOverwrite(String breakFileName, String bucketDir,
            ObjectMeta meta) throws ScmFeignException, S3ServerException {
        String key = meta.getKey();
        ScmDirPath subPathInBucket = new ScmDirPath(key);
        String scmFileName = subPathInBucket.getNamebyLevel(subPathInBucket.getLevel());
        BasicBSONObject scmFileInfo = buildScmFileInfo(meta, scmFileName);
        if (subPathInBucket.getLevel() <= 2) {
            // ex: key = file1.txt ，bucketDir = /s3_buckets/bucket1
            // scmFilePath = /s3_buckets/bucket1/file1.txt 
            ScmDirInfo scmFileParentDir = getDir(bucketDir);
            scmFileInfo.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, scmFileParentDir.getId());
            try {
                return overwriteFileWithRetry(breakFileName, scmFileInfo);
            }
            catch (ScmFeignException e) {
                if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                    throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                            "bucket dir not found:" + bucketDir);
                }
                throw e;
            }
        }

        // key= dir1/dir2/file.txt , bucketDir = /s3_buckets/bucket1
        // scmFilePath = /s3_buckets/bucket1/dir1/dir2/file.txt
        ScmDirInfo scmFileParentDir = null;
        try {
            // get dir: /s3_buckets/bucket1/dir1/dir2
            scmFileParentDir = getDir(CommonUtil.concatPath(bucketDir,
                    subPathInBucket.getPathByLevel(subPathInBucket.getLevel() - 1)));
        }
        catch (ScmFeignException e) {
            if (e.getStatus() != ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw e;
            }
            // create dir:  /s3_buckets/bucket1/dir1/dir2
            scmFileParentDir = createSubPathWithRetry(bucketDir, subPathInBucket);
        }

        scmFileInfo.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, scmFileParentDir.getId());
        try {
            return overwriteFileWithRetry(breakFileName, scmFileInfo);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                scmFileParentDir = createSubPathWithRetry(bucketDir, subPathInBucket);
                scmFileInfo.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, scmFileParentDir.getId());
                return overwriteFileWithRetry(breakFileName, scmFileInfo);
            }
            throw e;
        }
    }

    private BasicBSONObject buildScmFileInfo(ObjectMeta meta, String scmFileName)
            throws S3ServerException, ScmFeignException {
        BasicBSONObject scmFileInfo = new BasicBSONObject();
        scmFileInfo.put(FieldName.FIELD_CLFILE_FILE_TITLE, "");
        scmFileInfo.put(FieldName.FIELD_CLFILE_NAME, scmFileName);
        String classId = CLASS_ID_CACHE.get(ws);
        if (classId == null) {
            classId = getS3ClassId();
            CLASS_ID_CACHE.put(ws, classId);
        }
        scmFileInfo.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID, classId);
        BasicBSONObject customMeta = new BasicBSONObject();
        if (meta.getContentEncoding() != null) {
            customMeta.put(S3CommonDefine.S3_CUSTOM_META_CONENT_ENCODE, meta.getContentEncoding());
        }
        if (meta.getCacheControl() != null) {
            customMeta.put(S3CommonDefine.S3_CUSTOM_META_CACHE_CONTROL, meta.getCacheControl());
        }
        if (meta.getContentLanguage() != null) {
            customMeta.put(S3CommonDefine.S3_CUSTOM_META_CONTENT_LANGUAGE,
                    meta.getContentLanguage());
        }
        if (meta.geteTag() != null) {
            customMeta.put(S3CommonDefine.S3_CUSTOM_META_ETAG, meta.geteTag());
        }
        if (meta.getExpires() != null) {
            customMeta.put(S3CommonDefine.S3_CUSTOM_META_EXPIRES, meta.getExpires());
        }
        if (meta.getMetaList() != null && !meta.getMetaList().isEmpty()) {
            customMeta.put(S3CommonDefine.S3_CUSTOM_META_META_LIST,
                    new BasicBSONObject(meta.getMetaList()).toString());
        }
        if (meta.getContentDisposition() != null) {
            customMeta.put(S3CommonDefine.S3_CUSTOM_META_CONTENT_DISPOSITION,
                    meta.getContentDisposition());
        }
        if (!customMeta.isEmpty()) {
            scmFileInfo.put(FieldName.FIELD_CLFILE_PROPERTIES, customMeta);
        }
        scmFileInfo.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, meta.getContentType());
        return scmFileInfo;
    }

    private ScmDirInfo createSubPathWithRetry(String parentDir, ScmDirPath path)
            throws ScmFeignException, S3ServerException {
        try {
            return createSubPath(parentDir, path);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                return createSubPath(parentDir, path);
            }
            throw e;
        }
    }

    private ScmDirInfo createSubPath(String parentDir, ScmDirPath path)
            throws ScmFeignException, S3ServerException {
        ScmDirInfo scmFileParentDir = null;
        // parentDir = /bucketName
        // path = /dir1/dir2/
        try {
            // create /bucketName/dir1
            scmFileParentDir = createDirOrGet(
                    CommonUtil.concatPath(parentDir, path.getPathByLevel(2)));
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "bucket dir not found:" + parentDir);
            }
            throw e;
        }

        // create /bucketName/dir1/dir2/dir3
        for (int i = 3; i < path.getLevel(); i++) {
            scmFileParentDir = createDirOrGet(
                    CommonUtil.concatPath(parentDir, path.getPathByLevel(i)));
        }
        return scmFileParentDir;
    }

    private String getS3ClassId() throws S3ServerException, ScmFeignException {
        BasicBSONObject filter = new BasicBSONObject(FieldName.Class.FIELD_NAME,
                S3CommonDefine.S3_CUSTOM_CLASS_NAME);
        List<BSONObject> list = service.listClass(session.getSessionId(), session.getUserDetail(),
                ws, filter);
        if (list == null || list.size() == 0) {
            throw new S3ServerException(S3Error.SCM_GET_META_FAILED,
                    "failed to get s3 custom meta class");
        }
        return BsonUtils.getStringChecked(list.get(0), FieldName.Class.FIELD_ID);
    }

    private String getS3AttrId(String attrName) throws S3ServerException, ScmFeignException {
        BasicBSONObject filter = new BasicBSONObject(FieldName.Attribute.FIELD_NAME, attrName);
        List<BSONObject> list = service.listAttr(session.getSessionId(), session.getUserDetail(),
                ws, filter);
        if (list == null || list.size() != 1) {
            throw new S3ServerException(S3Error.SCM_GET_META_FAILED,
                    "failed to s3 get attribute name=" + attrName + ", " + list);
        }
        return BsonUtils.getStringChecked(list.get(0), FieldName.Attribute.FIELD_ID);
    }

    public void deleteBreakpointFileSilence(String name) {
        if (name == null) {
            return;
        }
        try {
            service.deleteBreakpointFile(session.getSessionId(), session.getUserDetail(), ws, name);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() != ScmError.FILE_NOT_FOUND.getErrorCode()) {
                logger.error("failed to delete breakpoint file:{}", name, e);
            }
        }
        catch (Exception e) {
            logger.error("failed to delete breakpoint file:{}", name, e);
        }
    }

    public void createBreakpointFile(String name, InputStream data) throws S3ServerException {
        ServiceInstance intance = loadlancer.choose(site);
        if (intance == null) {
            throw new S3ServerException(S3Error.SCM_CONTENSERER_NO_INSTANCE,
                    "no instance for " + site);
        }
        String uri = String.format(
                "%s%s%s%s%s?workspace_name=%s&create_time=%d&checksum_type=%s&is_last_content=%b",
                "http://", intance.getHost() + ":" + intance.getPort(), "/api/v1/",
                "breakpointfiles/", encode(name), encode(ws), System.currentTimeMillis(), "NONE",
                true);
        HttpPost request = new HttpPost(uri);
        if (data != null) {
            InputStreamEntity isEntity = new InputStreamEntity(data);
            isEntity.setContentType("binary/octet-stream");
            isEntity.setChunked(true);
            request.setEntity(isEntity);
        }
        request.setHeader(RestField.SESSION_ATTRIBUTE, session.getSessionId());
        request.setHeader(RestField.USER_ATTRIBUTE, session.getUserDetail());
        CloseableHttpResponse resp = null;
        try {
            resp = httpClient.execute(request);
            checkResponse("createBreakpointFile", convertToFeignResp(resp));
            EntityUtils.consume(resp.getEntity());
        }
        catch (Exception e) {
            CommonUtil.closeResource(resp);
            throw new S3ServerException(S3Error.SCM_CREATE_BREAKPOINT_FILE_FAILED,
                    "faield to create breakpoint file in scm:" + name, e);
        }
    }

    private static Response convertToFeignResp(CloseableHttpResponse httpResp)
            throws ScmFeignException, IOException {
        Map<String, Collection<String>> headers = new HashMap<>();
        HeaderIterator it = httpResp.headerIterator();
        while (it.hasNext()) {
            Header header = it.nextHeader();
            Collection<String> vs = headers.get(header.getName());
            if (vs == null) {
                vs = new ArrayList<>();
                vs.add(header.getValue());
            }
        }

        return Response.builder()
                .body(EntityUtils.toString(httpResp.getEntity()), Charset.forName("utf-8"))
                .headers(headers).reason(httpResp.getStatusLine().getReasonPhrase())
                .status(httpResp.getStatusLine().getStatusCode()).build();
    }

    private static String encode(String url) throws S3ServerException {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            return URLEncoder.encode(url, CHARSET_UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, e.getMessage(), e);
        }
    }

    public String initWorkspaceS3Meta() throws ScmFeignException, S3ServerException {
        BasicBSONObject classDesc = new BasicBSONObject();
        classDesc.put(FieldName.Class.FIELD_NAME, S3CommonDefine.S3_CUSTOM_CLASS_NAME);
        classDesc.put(FieldName.Class.FIELD_DESCRIPTION, "Custom meta for s3 service");
        String classId = null;
        try {
            BSONObject clazz = service.createClass(session.getSessionId(), session.getUserDetail(),
                    ws, classDesc);
            classId = BsonUtils.getStringChecked(clazz, FieldName.Class.FIELD_ID);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() != ScmError.METADATA_CLASS_EXIST.getErrorCode()) {
                throw e;
            }
            classId = getS3ClassId();
        }
        BasicBSONObject info = new BasicBSONObject();
        info.put(FieldName.Attribute.FIELD_DESCRIPTION, "Attribute for s3 service");
        info.put(FieldName.Attribute.FIELD_TYPE, "STRING");
        info.put(FieldName.Attribute.FIELD_REQUIRED, false);

        String[] attrNames = { S3CommonDefine.S3_CUSTOM_META_META_LIST,
                S3CommonDefine.S3_CUSTOM_META_CACHE_CONTROL,
                S3CommonDefine.S3_CUSTOM_META_CONENT_ENCODE,
                S3CommonDefine.S3_CUSTOM_META_CONTENT_LANGUAGE, S3CommonDefine.S3_CUSTOM_META_ETAG,
                S3CommonDefine.S3_CUSTOM_META_EXPIRES,
                S3CommonDefine.S3_CUSTOM_META_CONTENT_DISPOSITION };
        for (String attrName : attrNames) {
            info.put(FieldName.Attribute.FIELD_NAME, attrName);
            info.put(FieldName.Attribute.FIELD_DISPLAY_NAME, attrName);
            String attrId = null;
            try {
                BSONObject attrBson = service.createAttr(session.getSessionId(),
                        session.getUserDetail(), ws, info);
                attrId = BsonUtils.getStringChecked(attrBson, FieldName.Attribute.FIELD_ID);
            }
            catch (ScmFeignException e) {
                if (e.getStatus() != ScmError.METADATA_ATTR_EXIST.getErrorCode()) {
                    throw e;
                }
                attrId = getS3AttrId(attrName);
            }

            try {
                service.attachAttr(session.getSessionId(), session.getUserDetail(), ws, classId,
                        attrId);
            }
            catch (ScmFeignException e) {
                if (e.getStatus() != ScmError.METADATA_ATTR_ALREADY_IN_CLASS.getErrorCode()) {
                    throw e;
                }
            }
        }
        return classId;
    }

    public InputStream downloadScmFile(String fileId, long off, long len)
            throws ScmFeignException, S3ServerException {
        return new ScmFileInputStream(service, session, ws, fileId, off, len);
    }

}
