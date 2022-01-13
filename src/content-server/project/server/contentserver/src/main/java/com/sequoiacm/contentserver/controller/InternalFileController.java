package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/internal/v1")
public class InternalFileController {
    private static final Logger logger = LoggerFactory.getLogger(InternalFileController.class);

    @Autowired
    private IFileService fileService;

    @RequestMapping(value = "/files", method = RequestMethod.HEAD)
    public ResponseEntity<String> getFileDelta(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, required = false, defaultValue = CommonDefine.Scope.SCOPE_CURRENT
                    + "") Integer scope,
            HttpServletResponse response) throws ScmServerException {
        logger.info("internal get file delta: workspace={},filter={},scope={}", workspaceName,
                condition, scope);
        long count = fileService.countFiles(workspaceName, scope, condition);
        long sumSize = fileService.sumFileSizes(workspaceName, scope, condition);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        response.setHeader("X-SCM-Sum", String.valueOf(sumSize));
        return ResponseEntity.ok("");
    }

    @RequestMapping(value = "/files", method = RequestMethod.GET, params = "action=count")
    public long countFile(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, required = false, defaultValue = CommonDefine.Scope.SCOPE_CURRENT
                    + "") Integer scope,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition)
            throws ScmServerException {
        return fileService.countFiles(workspaceName, scope, condition);
    }

    @RequestMapping(value = "/files", method = RequestMethod.GET, params = "action=list")
    public void fileList(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            @RequestParam(CommonDefine.RestArg.FILE_LIST_SCOPE) int scope,
            @RequestParam(value = CommonDefine.RestArg.FILE_ORDERBY, required = false) BSONObject orderby,
            @RequestParam(value = CommonDefine.RestArg.FILE_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIMIT, required = false, defaultValue = "-1") long limit,
            @RequestParam(value = CommonDefine.RestArg.FILE_SELECTOR, required = false) BSONObject selector,
            HttpServletResponse response) throws ScmServerException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = fileService.getFileList(workspace_name, condition, scope, orderby,
                skip, limit, selector);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }

    @PostMapping(value = "/files/{file_id}", params = "action=updateExternalData")
    public boolean updateFileExternalData(@PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.FILE_MAJOR_VERSION) int majorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_EXTERNAL_DATA) BSONObject externalData,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName)
            throws ScmServerException {
        return fileService.updateFileExternalData(workspaceName, fileId, majorVersion, minorVersion,
                externalData, null);
    }

    @PostMapping(value = "/files", params = "action=updateExternalData")
    public void updateFileExternalData(
            @RequestParam(CommonDefine.RestArg.FILE_EXTERNAL_DATA) BSONObject externalData,
            @RequestParam(CommonDefine.RestArg.FILE_FILTER) BSONObject matcher,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName)
            throws ScmServerException {
        fileService.updateFileExternalData(workspaceName, matcher, externalData);
    }

    @RequestMapping(value = "/files/{file_id}", params = "action=get_info", method = RequestMethod.GET)
    public BSONObject getFileInfo(@PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(name = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false, defaultValue = "-1") int majorVersion,
            @RequestParam(name = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false, defaultValue = "-1") int minorVersion)
            throws ScmServerException {
        BSONObject file = fileService.getFileInfoById(workspaceName, fileId, majorVersion,
                minorVersion);
        return file;
    }

    @GetMapping(value = "/files/{file_id}", params = "action=download")
    public void downloadFile(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION, required = false) Integer majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION, required = false) Integer minorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_FLAG, required = false, defaultValue = "0") Integer readFlag,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_OFFSET, required = false, defaultValue = "0") long offset,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_LENGTH, required = false, defaultValue = CommonDefine.File.UNTIL_END_OF_FILE
                    + "") int length,
            HttpServletResponse response) throws ScmServerException {

        if (offset < 0) {
            throw new ScmInvalidArgumentException("invlid arg:offset=" + offset);
        }

        if (length < -1) {
            throw new ScmInvalidArgumentException("invlid arg:length=" + length);
        }

        ScmVersion version = new ScmVersion(majorVersion, minorVersion);

        BSONObject fileInfo = fileService.getFileInfoById(workspace_name, fileId,
                version.getMajorVersion(), version.getMinorVersion());
        response.setHeader("Content-Type",
                String.valueOf(fileInfo.get(FieldName.FIELD_CLFILE_FILE_MIME_TYPE)));
        response.setHeader("Content-Disposition",
                "attachment; filename=" + fileInfo.get(FieldName.FIELD_CLFILE_NAME));

        ServletOutputStream os = RestUtils.getOutputStream(response);

        if (offset > 0) {
            readFlag |= CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK;
        }

        // TODO:有可能服务端抛异常后再发了一次消息，考虑其它办法
        FileReaderDao dao = fileService.downloadFile(null, null, workspace_name, fileInfo,
                readFlag);
        try {
            dao.seek(offset);
            if (length == CommonDefine.File.UNTIL_END_OF_FILE) {
                // read all file data
                response.setHeader(CommonDefine.RestArg.DATA_LENGTH, dao.getSize() - offset + "");
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
                byte[] buffer = new byte[expectedLength];
                int actualReadLength = dao.read(buffer, 0, expectedLength);
                response.setHeader(CommonDefine.RestArg.DATA_LENGTH, actualReadLength + "");
                os.write(buffer, 0, actualReadLength);
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
}
