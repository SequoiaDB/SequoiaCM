package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.*;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.model.ClientUploadConf;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsFileMeta;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.dropwizard.DropwizardMetricServices;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

@RestController
@RequestMapping("/api/v1")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final IFileService fileService;

    private final IDirService dirService;

    private final DropwizardMetricServices metricServices;

    @Autowired
    private BucketInfoManager bucketInfoManager;

    @Autowired
    private ScmAudit audit;

    @Autowired
    public FileController(IFileService fileService, IDirService dirService,
            DropwizardMetricServices metricServices) {
        this.fileService = fileService;
        this.dirService = dirService;
        this.metricServices = metricServices;
    }

    @RequestMapping(value = "/files/{type}/**", method = RequestMethod.HEAD)
    public ResponseEntity<String> file(@PathVariable("type") String type,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(name = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(name = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        BSONObject file;
        if (type.equals("path")) {
            String ignoreStr = "/api/v1/files/path";
            String filePath = RestUtils.getDecodePath(request.getRequestURI(), ignoreStr.length());
            filePath = ScmSystemUtils.formatFilePath(filePath);
            file = fileService.getFileInfoByPath(user, workspaceName, filePath,
                    version.getMajorVersion(), version.getMinorVersion(), false);
        }
        else if (type.equals("id")) {
            String ignoreStr = "/api/v1/files/id/";
            String fileId = request.getRequestURI().substring(ignoreStr.length());
            file = fileService.getFileInfoById(user, workspaceName, fileId,
                    version.getMajorVersion(), version.getMinorVersion(), false);
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }

        file.removeField("_id");
        Number bucketId = BsonUtils.getNumber(file, FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (bucketId != null) {
            ScmBucket bucket = bucketInfoManager.getBucketById(bucketId.longValue());
            if (bucket != null) {
                file.put(CommonDefine.RestArg.BUCKET_NAME, bucket.getName());
            }
            else {
                file.put(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, null);
            }
        }
        String fileInfo = RestUtils.urlEncode(file.toString());
        response.setHeader(CommonDefine.RestArg.FILE_RESP_FILE_INFO, fileInfo);
        return ResponseEntity.ok("");
    }

    @RequestMapping(value = "/files/{file_id}", method = RequestMethod.PUT)
    public void updateFile(@PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_BREAKPOINT_FILE, required = false) String newBreakPointFileContent,
            @RequestParam(value = CommonDefine.RestArg.FILE_INFO, required = false) BSONObject newFileProperties,
            @RequestParam(value = CommonDefine.RestArg.FILE_UPDATE_CONTENT_OPTION, required = false) BSONObject updateContentOption,
            Authentication auth, HttpServletRequest request, HttpServletResponse response)
            throws ScmServerException, IOException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        InputStream inputStreamEntity = null;
        BSONObject updatedFileInfo;
        try {
            ScmVersion version = new ScmVersion(majorVersion, minorVersion);

            if (newFileProperties != null) {
                updatedFileInfo = fileService.updateFileInfo(user, workspaceName, fileId,
                        newFileProperties, version.getMajorVersion(), version.getMinorVersion());
            }
            else if (newBreakPointFileContent != null) {
                // update content by breakPoint
                updatedFileInfo = fileService.updateFileContent(user, workspaceName, fileId,
                        newBreakPointFileContent, version.getMajorVersion(),
                        version.getMinorVersion(), new ScmUpdateContentOption(updateContentOption));
            }
            else {
                // update content by InputStreamEntity file
                inputStreamEntity = request.getInputStream();
                updatedFileInfo = fileService.updateFileContent(user, workspaceName, fileId,
                        inputStreamEntity, version.getMajorVersion(), version.getMinorVersion(),
                        new ScmUpdateContentOption(updateContentOption));
            }
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(inputStreamEntity);
        }

        audit.info(ScmAuditType.UPDATE_FILE, auth, workspaceName, 0,
                "update file by fileId=" + fileId);
        String fileInfoUtf8 = RestUtils.urlEncode(updatedFileInfo.toString());
        response.setHeader(CommonDefine.RestArg.FILE_INFO, fileInfoUtf8);
    }

    @RequestMapping(value = "/files", method = RequestMethod.GET)
    public void fileList(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            @RequestParam(CommonDefine.RestArg.FILE_LIST_SCOPE) int scope,
            @RequestParam(value = CommonDefine.RestArg.FILE_ORDERBY, required = false) BSONObject orderby,
            @RequestParam(value = CommonDefine.RestArg.FILE_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIMIT, required = false, defaultValue = "-1") long limit,
            @RequestParam(value = CommonDefine.RestArg.FILE_SELECTOR, required = false) BSONObject select,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();

        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = fileService.getFileList(user, workspace_name, condition, scope, orderby,
                skip, limit, select);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }

    // 文件上传接口在网关 UploadFileStatisticsDeciderImpl 中对其进行了捕获，修改下载接口需要考虑网关
    @PostMapping("/files")
    public ResponseEntity<BSONObject> uploadFile(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestHeader(value = CommonDefine.RestArg.FILE_DESCRIPTION) String desc,
            @RequestParam(value = CommonDefine.RestArg.FILE_BREAKPOINT_FILE, required = false) String breakpointFileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_UPLOAD_CONFIG, required = false) BSONObject uploadConfig,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(value = ScmStatisticsDefine.STATISTICS_HEADER, required = false) String statisticsType,
            HttpServletRequest request, Authentication auth)
            throws ScmServerException, IOException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        // statistical traffic
        incrementTraffic(CommonDefine.Metrics.PREFIX_FILE_UPLOAD + workspaceName);

        BasicBSONObject fileInfo = new BasicBSONObject();
        fileInfo.append(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, MimeType.OCTET_STREAM);

        BSONObject fullFileInfo;
        String message = "";
        // it's always return a not null InputStream whether the client set
        // InputStreamEntity or not
        InputStream is = request.getInputStream();

        String username = auth.getName();
        try {
            BSONObject description = (BSONObject) JSON.parse(RestUtils.urlDecode(desc));
            fileInfo.putAll(description);
            logger.debug("file description:{}", fileInfo);

            // check properties create by zhangping
            MetaDataManager.getInstence().checkPropeties(workspaceName,
                    (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_CLASS_ID),
                    (BSONObject) fileInfo.get(FieldName.FIELD_CLFILE_PROPERTIES));

            ScmFileServicePriv privService = ScmFileServicePriv.getInstance();

            // overwrite file priv
            ClientUploadConf uploadConf = new ClientUploadConf(uploadConfig);

            ScmUserPasswordType userPasswordType = ((ScmUser) auth.getPrincipal())
                    .getPasswordType();
            if (null != breakpointFileName) {
                fullFileInfo = fileService.uploadFile(user, workspaceName, breakpointFileName,
                        fileInfo, sessionId, userDetail, userPasswordType, uploadConf);
            }
            else {
                fullFileInfo = fileService.uploadFile(user, workspaceName, is, fileInfo, sessionId,
                        userDetail, userPasswordType, uploadConf);
                message = "create file , file id=" + fullFileInfo.get(FieldName.FIELD_CLFILE_ID)
                        + ", file name="
                        + String.valueOf(fileInfo.get(FieldName.FIELD_CLFILE_NAME));
            }
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }

        BSONObject body = new BasicBSONObject(CommonDefine.RestArg.FILE_RESP_FILE_INFO,
                fullFileInfo);
        ResponseEntity.BodyBuilder e = ResponseEntity.ok();
        if (ScmStatisticsType.FILE_UPLOAD.equals(statisticsType)) {
            ScmStatisticsFileMeta staticsExtra = ScmFileOperateUtils.createStatisticsFileMeta(fullFileInfo,
                    workspaceName, username, -1, breakpointFileName);
            e.header(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER, staticsExtra.toJSON());
        }
        return e.body(body);
    }

    // @PostMapping("/files")
    // public BSONObject uploadFile(
    // @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
    // @RequestParam(value = CommonDefine.RestArg.FILE_MULTIPART_FILE, required
    // = false) MultipartFile file,
    // @RequestParam(value = CommonDefine.RestArg.FILE_BREAKPOINT_FILE, required
    // = false) String breakpointFileName,
    // @RequestParam(CommonDefine.RestArg.FILE_DESCRIPTION) BSONObject
    // description,
    // Authentication auth) throws ScmServerException {
    // if (file == null && !StringUtils.hasText(breakpointFileName)) {
    // throw new ScmInvalidArgumentException(
    // "Missing " + CommonDefine.RestArg.FILE_MULTIPART_FILE + " or "
    // + CommonDefine.RestArg.FILE_BREAKPOINT_FILE);
    // }
    //
    // if (file != null && StringUtils.hasText(breakpointFileName)) {
    // throw new ScmInvalidArgumentException(
    // "Specify both " + CommonDefine.RestArg.FILE_MULTIPART_FILE + " and "
    // + CommonDefine.RestArg.FILE_BREAKPOINT_FILE);
    // }
    //
    // BasicBSONObject fileInfo = new BasicBSONObject();
    // if (file != null) {
    // fileInfo.append(FieldName.FIELD_CLFILE_NAME, file.getOriginalFilename());
    // fileInfo.append(FieldName.FIELD_CLFILE_FILE_AUTHOR, "");
    // fileInfo.append(FieldName.FIELD_CLFILE_FILE_TITLE, "");
    // fileInfo.append(FieldName.FIELD_CLFILE_FILE_MIME_TYPE,
    // file.getContentType());
    // }
    //
    // if (null != description && !description.isEmpty()) {
    // fileInfo.putAll(description);
    // }
    //
    // logger.debug("file description:{}", fileInfo);
    // // check properties create by zhangping
    // MetaDataManager.getInstence().checkPropeties(workspaceName,
    // (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_CLASS_ID),
    // (BSONObject) fileInfo.get(FieldName.FIELD_CLFILE_PROPERTIES));
    //
    // String username = auth.getName();
    // ScmFileServicePriv.getInstance().checkDirPriorityById(auth.getName(),
    // workspaceName,
    // dirService, (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID),
    // ScmPrivilegeDefine.CREATE, "create file");
    //
    // InputStream is = null;
    // BSONObject fullFileInfo;
    // String message = "";
    // try {
    // if (file != null) {
    // is = RestUtils.getInputStream(file);
    // fullFileInfo = fileService.uploadFile(workspaceName, username, is,
    // fileInfo);
    // message = "create file , file id=" +
    // fullFileInfo.get(FieldName.FIELD_CLFILE_ID)
    // + ", file name="
    // + String.valueOf(fileInfo.get(FieldName.FIELD_CLFILE_NAME));
    // }
    // else {
    // fullFileInfo = fileService.uploadFile(workspaceName, username,
    // breakpointFileName,
    // fileInfo);
    // message = "create breakpointFile , file id="
    // + fullFileInfo.get(FieldName.FIELD_CLFILE_ID) + ", breakpointFileName="
    // + breakpointFileName;
    // }
    // }
    // finally {
    // ScmSystemUtils.closeResource(is);
    // }
    //
    // audit.info(ScmAuditType.CREATE_FILE, auth, workspaceName, 0,
    // message);
    // return new BasicBSONObject(CommonDefine.RestArg.FILE_RESP_FILE_INFO,
    // fullFileInfo);
    // }


    // 文件下载接口在网关 DownloadFileStatisticsDeciderImpl 中对其进行了捕获，修改下载接口需要考虑网关
    @GetMapping("/files/{file_id}")
    public void downloadFile(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_FLAG, required = false, defaultValue = "0") Integer readFlag,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_OFFSET, required = false, defaultValue = "0") long offset,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_LENGTH, required = false, defaultValue = CommonDefine.File.UNTIL_END_OF_FILE
                    + "") int length,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(value = ScmStatisticsDefine.STATISTICS_HEADER, required = false) String statisticsType,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        // statistical traffic
        incrementTraffic(CommonDefine.Metrics.PREFIX_FILE_DOWNLOAD + workspace_name);

        if (offset < 0) {
            throw new ScmInvalidArgumentException("invlid arg:offset=" + offset);
        }

        if (length < -1) {
            throw new ScmInvalidArgumentException("invlid arg:length=" + length);
        }

        ScmVersion version = new ScmVersion(majorVersion, minorVersion);

        BSONObject fileInfo = fileService.getFileInfoById(user, workspace_name, fileId,
                version.getMajorVersion(), version.getMinorVersion(), false);

        response.setHeader("Content-Type",
                String.valueOf(fileInfo.get(FieldName.FIELD_CLFILE_FILE_MIME_TYPE)));
        response.setHeader("Content-Disposition",
                "attachment; filename=" + fileInfo.get(FieldName.FIELD_CLFILE_NAME));

        ServletOutputStream os = RestUtils.getOutputStream(response);

        if (offset > 0) {
            readFlag |= CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK;
        }

        // TODO:有可能服务端抛异常后再发了一次消息，考虑其它办法
        FileReaderDao dao = fileService.downloadFile(sessionId, userDetail, user, workspace_name,
                fileInfo, readFlag);
        try {
            dao.seek(offset);
            if (length == CommonDefine.File.UNTIL_END_OF_FILE) {
                long readLen = dao.getSize() - offset;
                if (ScmStatisticsType.FILE_DOWNLOAD.equals(statisticsType)) {
                    response.setHeader(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER,
                            ScmFileOperateUtils.createStatisticsFileMeta(fileInfo, workspace_name, auth.getName(),
                                    readLen, null).toJSON());
                }

                // read all file data
                response.setHeader(CommonDefine.RestArg.DATA_LENGTH, readLen + "");
                dao.read(os);
            }
            else {
                // client specified length, maxReadLength is
                // Const.TRANSMISSION_LEN
                int expectedLength;
                if (length > Const.TRANSMISSION_LEN) {
                    expectedLength = Const.TRANSMISSION_LEN;
                }
                else {
                    expectedLength = length;
                }

                if (ScmStatisticsType.FILE_DOWNLOAD.equals(statisticsType)) {
                    response.setHeader(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER,
                            ScmFileOperateUtils.createStatisticsFileMeta(fileInfo, workspace_name, auth.getName(),
                                    expectedLength, null).toJSON());
                }

                byte[] buffer = new byte[expectedLength];
                int actualReadLength = dao.read(buffer, 0, expectedLength);
                response.setHeader(CommonDefine.RestArg.DATA_LENGTH, actualReadLength + "");
                os.write(buffer, 0, actualReadLength);
            }
            try {
                FlowRecorder.getInstance().addDownloadSize(workspace_name,
                        CommonHelper.toLongValue(fileInfo.get(FieldName.FIELD_CLFILE_FILE_SIZE)));
            }
            catch (Exception e) {
                logger.error("add flow record failed", e);
            }
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.NETWORK_IO, "failed to download file:workspace="
                    + workspace_name + ",fileId=" + fileId + ",version=" + version, e);
        }
        finally {
            dao.close();
        }

        RestUtils.flush(os);
    }

    // statistical traffic
    private void incrementTraffic(String namePrefix) {
        Date today = new Date();
        metricServices.increment(namePrefix + "." + CommonHelper.getCurrentDay(today));
        // remove 30 days ago records
        metricServices.reset(namePrefix + "." + CommonHelper.getDateBeforeDays(today, 30));
    }

    @DeleteMapping("/files/{file_id}")
    public void deleteFile(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_IS_PHYSICAL, required = false, defaultValue = "false") Boolean isPhysical,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        fileService.deleteFile(sessionId, userDetail, user, workspaceName, fileId,
                version.getMajorVersion(), version.getMinorVersion(), isPhysical);
    }

    @DeleteMapping(value = "/files", params = "action=" +
            CommonDefine.RestArg.ACTION_DELETE_FILE_BY_PATH)
    public void deleteFileByPath(@RequestParam("file_path") String filePath,
                                 @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
                                 @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false, defaultValue = "-1") Integer majorVersion,
                                 @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false, defaultValue = "-1") Integer minorVersion,
                                 @RequestParam(value = CommonDefine.RestArg.FILE_IS_PHYSICAL, required = false, defaultValue = "false") Boolean isPhysical,
                                 @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
                                 @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
                                 HttpServletRequest request,
                                 HttpServletResponse response, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        filePath = ScmSystemUtils.formatFilePath(filePath);
        fileService.deleteFileByPath(sessionId, userDetail, user, workspaceName, filePath,
                version.getMajorVersion(), version.getMinorVersion(), isPhysical);
    }

    @RequestMapping(value = "/files", method = RequestMethod.HEAD)
    public ResponseEntity<String> countFile(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, required = false, defaultValue = CommonDefine.Scope.SCOPE_CURRENT
                    + "") Integer scope,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        long count = fileService.countFiles(user, workspaceName, scope, condition);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok("");
    }

    @RequestMapping("/files/{file_id}/async-transfer")
    public ResponseEntity<String> asyncTransferFile(@PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_ASYNC_TRANSFER_TARGET_SITE, required = false) String targetSite,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        fileService.asyncTransferFile(user, workspaceName, fileId, version.getMajorVersion(),
                version.getMinorVersion(), targetSite);
        return ResponseEntity.ok("");
    }

    @RequestMapping("/files/{file_id}/async-cache")
    public ResponseEntity<String> asyncCacheFile(@PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        fileService.asyncCacheFile(user, workspaceName, fileId, version.getMajorVersion(),
                version.getMinorVersion());
        return ResponseEntity.ok("");
    }

    @PostMapping(value = "/files/{file_id}", params = "action="
            + CommonDefine.RestArg.ACTION_CALC_MD5)
    public BSONObject calcMd5(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false, defaultValue = "-1") int majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false, defaultValue = "-1") int minorVersion,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException, UnsupportedEncodingException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        String md5 = fileService.calcFileMd5(sessionId, userDetail, user, workspaceName, fileId,
                majorVersion, minorVersion);

        return new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_MD5, md5);
    }

    @RequestMapping(value = "/files/{file_id}", params = "action="
            + CommonDefine.RestArg.ACTION_GET_CONTENT_LOCATION, method = RequestMethod.GET)
    public ResponseEntity<BasicBSONList> getFileContentLocations(
            @PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            Authentication auth) throws ScmServerException {
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        BSONObject file = fileService.getFileInfoById(workspaceName, fileId,
                version.getMajorVersion(), version.getMinorVersion(), false);
        ScmUser user = (ScmUser) auth.getPrincipal();

        BasicBSONList result = fileService.getFileContentLocations(user, file, workspaceName);
        ResponseEntity.BodyBuilder e = ResponseEntity.ok();
        return e.body(result);
    }

    @DeleteMapping(value = "/files/{file_id}", params = "action="
            + CommonDefine.RestArg.ACTION_DELETE_VERSION)
    public void deleteVersion(@PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            Authentication auth) throws ScmServerException {
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        ScmUser user = (ScmUser) auth.getPrincipal();
        fileService.deleteVersion(user, workspaceName, fileId, version.getMajorVersion(),
                version.getMinorVersion());
    }
}
