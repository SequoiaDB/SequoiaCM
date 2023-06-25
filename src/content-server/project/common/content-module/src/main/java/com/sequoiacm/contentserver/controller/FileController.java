package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.*;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.model.ClientUploadConf;
import com.sequoiacm.contentserver.model.FileFieldExtraDefine;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.model.UpdaterKeyDefine;
import com.sequoiacm.contentserver.pipeline.file.module.FileExistStrategy;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.pipeline.file.module.FileUploadConf;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.KeepAlive;
import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import com.sequoiacm.infrastructure.common.SecurityRestField;
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
import java.util.Map;
import java.util.Set;

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
    private FileMetaFactory fileMetaFactory;

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
        FileMeta file;
        if (type.equals("path")) {
            String ignoreStr = "/api/v1/files/path";
            String filePath = RestUtils.getDecodePath(request.getRequestURI(), ignoreStr.length());
            filePath = ScmSystemUtils.formatFilePath(filePath);
            file = dirService.getFileInfoByPath(user, workspaceName, filePath,
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

        String fileInfo = RestUtils.urlEncode(file.toUserInfoBSON().toString());
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
        FileMeta updatedFileInfo;
        try {
            ScmVersion version = new ScmVersion(majorVersion, minorVersion);

            if (newFileProperties != null) {
                updatedFileInfo = updateFileInfo(user, workspaceName, fileId, version,
                        newFileProperties);
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
        String fileInfoUtf8 = RestUtils.urlEncode(updatedFileInfo.toUserInfoBSON().toString());
        response.setHeader(CommonDefine.RestArg.FILE_INFO, fileInfoUtf8);
    }

    private FileMeta updateFileInfo(ScmUser user, String workspaceName, String fileId,
            ScmVersion version, BSONObject newFileProperties) throws ScmServerException {
        checkUpdateInfoObj(workspaceName, fileId, version.getMajorVersion(),
                version.getMinorVersion(), newFileProperties);
        String moveToDirId = (String) newFileProperties.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        if (moveToDirId != null) {
            return dirService.moveFile(user, workspaceName, moveToDirId, fileId, version);
        }
        String moveToDirPath = (String) newFileProperties
                .get(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);
        if (moveToDirPath != null) {
            return dirService.moveFileByPath(user, workspaceName, moveToDirPath, fileId, version);
        }
        return fileService.updateFileInfo(user, workspaceName, fileId, newFileProperties,
                version.getMajorVersion(), version.getMinorVersion());

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

        boolean isResContainsDeleteMarker = ScmSystemUtils.isDeleteMarkerRequired(scope);
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = fileService.getFileList(user, workspace_name, condition, scope, orderby,
                skip, limit, select, isResContainsDeleteMarker);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    // 文件上传接口在网关 UploadFileStatisticsDeciderImpl 中对其进行了捕获，修改下载接口需要考虑网关
    @PostMapping("/files")
    public ResponseEntity<BSONObject> uploadFile(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestHeader(value = CommonDefine.RestArg.FILE_DESCRIPTION) String desc,
            @RequestParam(value = CommonDefine.RestArg.FILE_BREAKPOINT_FILE, required = false) String breakpointFileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_UPLOAD_CONFIG, required = false) BSONObject uploadConfig,
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(value = ScmStatisticsDefine.STATISTICS_HEADER, required = false) String statisticsType,
            HttpServletRequest request, Authentication auth)
            throws ScmServerException, IOException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        // statistical traffic
        incrementTraffic(CommonDefine.Metrics.PREFIX_FILE_UPLOAD + workspaceName);

        BSONObject description = (BSONObject) JSON.parse(RestUtils.urlDecode(desc));

        InputStream is = request.getInputStream();
        String username = auth.getName();
        try {
            logger.debug("file description:{}", description);
            FileMeta fileMeta = fileMetaFactory.createFileMetaByUserInfo(workspaceName, description,
                    user.getUsername(), true);

            ClientUploadConf clientUploadConf = new ClientUploadConf(uploadConfig);

            FileUploadConf fileUploadConf = new FileUploadConf(
                    clientUploadConf.isOverwrite() ? FileExistStrategy.OVERWRITE
                            : FileExistStrategy.THROW_EXCEPTION,
                    clientUploadConf.isNeedMd5());
            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                    .getWorkspaceInfoCheckExist(workspaceName);
            String parentDirId = BsonUtils.getStringOrElse(description,
                    FieldName.FIELD_CLFILE_DIRECTORY_ID, CommonDefine.Directory.SCM_ROOT_DIR_ID);
            if (!wsInfo.isEnableDirectory()) {
                if (!parentDirId.isEmpty()
                        && !parentDirId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
                    throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                            "directory feature is disable, can not specified directory id for create file");
                }
            }

            FileMeta createdFile;
            if (wsInfo.isEnableDirectory()) {
                createdFile = createFileInDir(user, workspaceName, parentDirId, fileMeta,
                        breakpointFileName, is, fileUploadConf);
            }
            else {
                createdFile = createFile(user, workspaceName, fileMeta, breakpointFileName, is,
                        fileUploadConf);
            }

            BSONObject createdFileBson = createdFile.toUserInfoBSON();
            BSONObject body = new BasicBSONObject(CommonDefine.RestArg.FILE_RESP_FILE_INFO,
                    createdFileBson);
            ResponseEntity.BodyBuilder respBuilder = ResponseEntity.ok();
            if (ScmStatisticsType.FILE_UPLOAD.equals(statisticsType)) {
                ScmStatisticsFileMeta staticsExtra = ScmFileOperateUtils.createStatisticsFileMeta(
                        createdFile, workspaceName, username, -1, breakpointFileName);
                respBuilder.header(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER,
                        staticsExtra.toJSON());
            }
            return respBuilder.body(body);
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }
    }

    private FileMeta createFileInDir(ScmUser user, String ws, String parentDirId, FileMeta fileInfo,
            String breakpointFile, InputStream fileData, FileUploadConf uploadConf)
            throws ScmServerException {
        if (breakpointFile == null) {
            return dirService.createFile(user, ws, parentDirId, fileInfo, fileData, uploadConf);
        }
        else {
            return dirService.createFile(user, ws, parentDirId, fileInfo, breakpointFile,
                    uploadConf);
        }
    }

    private FileMeta createFile(ScmUser user, String ws, FileMeta fileInfo, String breakpointFile,
            InputStream fileData, FileUploadConf uploadConf) throws ScmServerException {
        if (breakpointFile == null) {
            return fileService.createFile(user, ws, fileInfo, uploadConf, fileData);
        }
        else {
            return fileService.createFile(user, ws, fileInfo, uploadConf, breakpointFile);
        }
    }

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
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
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

        FileMeta fileInfo = fileService.getFileInfoById(user, workspace_name, fileId,
                version.getMajorVersion(), version.getMinorVersion(), false);

        response.setHeader("Content-Type", String.valueOf(fileInfo.getMimeType()));
        response.setHeader("Content-Disposition", "attachment; filename=" + fileInfo.getName());

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
                        CommonHelper.toLongValue(fileInfo.getSize()));
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
        metricServices.increment(namePrefix + "." + CommonHelper.getCurrentDay(today, null));
        // remove 30 days ago records
        metricServices.reset(namePrefix + "." + CommonHelper.getDateBeforeDays(today, 30));
    }

    @DeleteMapping("/files/{file_id}")
    public void deleteFile(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_IS_PHYSICAL, required = false, defaultValue = "false") Boolean isPhysical,
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            Authentication auth) throws ScmServerException {
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
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        ScmVersion version = new ScmVersion(majorVersion, minorVersion);
        filePath = ScmSystemUtils.formatFilePath(filePath);
        FileMeta fileInfo = dirService.getFileInfoByPath(user, workspaceName, filePath,
                majorVersion, minorVersion, true);
        fileService.deleteFile(sessionId, userDetail, user, workspaceName, fileInfo.getId(),
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
        boolean isResContainsDeleteMarker = ScmSystemUtils.isDeleteMarkerRequired(scope);
        long count = fileService.countFiles(user, workspaceName, scope, condition,
                isResContainsDeleteMarker);
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

    @KeepAlive
    @PostMapping(value = "/files/{file_id}", params = "action="
            + CommonDefine.RestArg.ACTION_CALC_MD5)
    public BSONObject calcMd5(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false, defaultValue = "-1") int majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false, defaultValue = "-1") int minorVersion,
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
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
        FileMeta file = fileService.getFileInfoById(workspaceName, fileId,
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

    private void checkUpdateInfoObj(String wsName, String fileId, int major, int minor,
            BSONObject updateInfoObj) throws ScmServerException {
        // only one properties now.
        Set<String> objFields = updateInfoObj.keySet();
        if (objFields.size() != 1) {
            if (objFields.size() != 2) {
                throw new ScmInvalidArgumentException(
                        "invlid argument,updates only one properties at a time:updator="
                                + updateInfoObj);
            }

            // key number = 2
            if (!objFields.contains(FieldName.FIELD_CLFILE_PROPERTIES)
                    || !objFields.contains(FieldName.FIELD_CLFILE_FILE_CLASS_ID)) {
                // must contain id and properties
                throw new ScmInvalidArgumentException(
                        "invlid argument,updates only classId and properties at a time:updator="
                                + updateInfoObj);
            }
        }

        UpdaterKeyDefine.checkAvailableFields(objFields);
        FileFieldExtraDefine.checkStringFields(updateInfoObj);

        // value type is bson, check the format
        String fieldName = FieldName.FIELD_CLFILE_PROPERTIES;
        if (updateInfoObj.containsField(fieldName)) {
            BSONObject classValue = (BSONObject) updateInfoObj.get(fieldName);
            updateInfoObj.put(fieldName,
                    ScmArgumentChecker.checkAndCorrectClass(classValue, fieldName));
        }

        fieldName = FieldName.FIELD_CLFILE_TAGS;
        if (updateInfoObj.containsField(fieldName)) {
            BSONObject tagsValue = (BSONObject) updateInfoObj.get(fieldName);
            updateInfoObj.put(fieldName, ScmArgumentChecker.checkAndCorrectTagsAsBson(tagsValue));
        }

        BSONObject correctCustomTag = checkAndCorrectCustomTag(
                updateInfoObj.get(FieldName.FIELD_CLFILE_CUSTOM_TAG));
        if (correctCustomTag != null) {
            updateInfoObj.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, correctCustomTag);
        }
        correctCustomTag = checkAddOrRemoveCustomTag(
                updateInfoObj.get(UpdaterKeyDefine.ADD_CUSTOM_TAG));
        if (correctCustomTag != null) {
            updateInfoObj.put(UpdaterKeyDefine.ADD_CUSTOM_TAG, correctCustomTag);
        }
        correctCustomTag = checkAddOrRemoveCustomTag(
                updateInfoObj.get(UpdaterKeyDefine.REMOVE_CUSTOM_TAG));
        if (correctCustomTag != null) {
            updateInfoObj.put(UpdaterKeyDefine.REMOVE_CUSTOM_TAG, correctCustomTag);
        }

        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
        String fileName = (String) updateInfoObj.get(FieldName.FIELD_CLFILE_NAME);
        if (fileName != null) {
            ScmFileOperateUtils.checkFileName(ws, fileName);
        }

        if (updateInfoObj.containsField(FieldName.FIELD_CLFILE_FILE_CLASS_ID)
                || updateInfoObj.containsField(FieldName.FIELD_CLFILE_PROPERTIES)
                || MetaDataManager.getInstence().isUpdateSingleClassProperty(updateInfoObj,
                        FieldName.FIELD_CLFILE_PROPERTIES)) {
            String classIdKey = FieldName.FIELD_CLFILE_FILE_CLASS_ID;
            String propertiesKey = FieldName.FIELD_CLFILE_PROPERTIES;
            BSONObject oldFileVersion = ScmContentModule.getInstance().getMetaService()
                    .getFileInfo(ws.getMetaLocation(), ws.getName(), fileId, major, minor, false);
            String classId = (String) oldFileVersion.get(classIdKey);
            MetaDataManager.getInstence().checkUpdateProperties(ws.getName(), updateInfoObj,
                    classIdKey, propertiesKey, classId);
        }
    }

    private BSONObject checkAndCorrectCustomTag(Object obj) throws ScmServerException {
        if (obj == null) {
            return null;
        }
        if (!(obj instanceof BasicBSONObject)) {
            throw new ScmInvalidArgumentException(
                    "invalid argument, custom tag must be key value format: " + obj);
        }
        Map<String, String> map = ScmArgumentChecker.checkAndCorrectCustomTag((BSONObject) obj);
        BasicBSONObject ret = new BasicBSONObject();
        ret.putAll(map);
        return ret;
    }

    private BSONObject checkAddOrRemoveCustomTag(Object obj) throws ScmServerException {
        BSONObject ret = checkAndCorrectCustomTag(obj);
        if (ret == null) {
            return ret;
        }

        if (ret.keySet().size() != 1) {
            throw new ScmInvalidArgumentException(
                    "invalid argument, custom tag only support one key value: " + obj);
        }
        return ret;
    }
}
