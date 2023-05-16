package com.sequoiacm.om.omserver.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.om.omserver.common.CommonUtil;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.common.ScmOmInputStream;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileContent;
import com.sequoiacm.om.omserver.module.OmFileDetail;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import com.sequoiacm.om.omserver.service.ScmFileService;
import com.sequoiacm.om.omserver.service.ScmTagService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmFileController {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ScmFileService fileService;

    @Autowired
    private ScmTagService tagService;

    @RequestMapping(value = "/files/id/{file_id}", method = RequestMethod.HEAD)
    public ResponseEntity<Object> getFileDetail(@PathVariable("file_id") String fileId,
            @RequestParam(value = RestParamDefine.MAJOR_VERSION, defaultValue = "1") int majorVersion,
            @RequestParam(value = RestParamDefine.MINOR_VERSION, defaultValue = "0") int minorVersion,
            @RequestParam(RestParamDefine.WORKSPACE) String wsName, ScmOmSession session)
            throws JsonProcessingException, ScmInternalException, ScmOmServerException {
        OmFileDetail fileDetail = fileService.getFileDetail(session, wsName, fileId, majorVersion,
                minorVersion);
        return ResponseEntity.ok().header(RestParamDefine.FILE,
                CommonUtil.urlEncode(mapper.writeValueAsString(fileDetail))).build();
    }

    @GetMapping(value = "/files/id/{file_id}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("file_id") String fileId,
            @RequestParam(RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(RestParamDefine.SITE_NAME) String siteName,
            @RequestParam(value = RestParamDefine.MAJOR_VERSION, defaultValue = "1") int majorVersion,
            @RequestParam(value = RestParamDefine.MINOR_VERSION, defaultValue = "0") int minorVersion,
            ScmOmSession session) throws ScmInternalException, ScmOmServerException, IOException {
        OmFileContent fileContent = fileService.downloadFile(session, wsName, siteName, fileId,
                majorVersion, minorVersion);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment;fileName=" + CommonUtil.urlEncode(fileContent.getFileName()))
                .contentLength(fileContent.getFileLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(fileContent.getFileContent()));
    }

    @GetMapping("/files")
    public List<OmFileBasic> getFileList(ScmOmSession session,
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(value = RestParamDefine.FILE_LIST_SCOPE, required = false, defaultValue = "1") int scope,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            @RequestParam(value = RestParamDefine.FILTER, required = false, defaultValue = "{}") BSONObject filter,
            @RequestParam(value = RestParamDefine.TAG_CONDITION, required = false) BSONObject tagCondition,
            @RequestParam(value = RestParamDefine.ORDERBY, required = false) BSONObject orderBy,
            HttpServletResponse response) throws ScmInternalException, ScmOmServerException {
        if (tagCondition == null) {
            long fileCount = fileService.getFileCount(session, wsName, scope, filter);
            response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(fileCount));
            if (fileCount <= 0) {
                return Collections.emptyList();
            }
            return fileService.getFileList(session, wsName, scope, filter, orderBy, skip, limit);
        }

        long fileCount = tagService.countFileWithTag(session, wsName, scope, tagCondition, filter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(fileCount));
        if (fileCount <= 0) {
            return Collections.emptyList();
        }
        return tagService.searchFileWithTag(session, wsName, scope, tagCondition, filter, orderBy,
                skip, limit);
    }

    @PostMapping("/files")
    public void uploadFile(ScmOmSession session,
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(value = RestParamDefine.SITE_NAME) String siteName,
            @RequestParam(value = RestParamDefine.FILE_UPLOAD_CONFIG, required = false, defaultValue = "{}") BSONObject uploadConf,
            @RequestHeader(value = RestParamDefine.FILE_DESCRIPTION, required = false) String desc,
            HttpServletRequest request)
            throws ScmInternalException, ScmOmServerException, IOException {
        ScmOmInputStream is = new ScmOmInputStream(request.getInputStream());
        try {
            OmFileInfo fileInfo = mapper.readValue(CommonUtil.urlDecode(desc), OmFileInfo.class);
            fileService.uploadFile(session, wsName, siteName, fileInfo, uploadConf, is);
        }
        finally {
            CommonUtil.consumeAndCloseResource(is);
        }
    }

    @PutMapping("/files/id/{file_id}/content")
    public void updateFileContent(ScmOmSession session, @PathVariable("file_id") String fileId,
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(value = RestParamDefine.SITE_NAME) String siteName,
            @RequestParam(value = RestParamDefine.FILE_UPDATE_CONTENT_OPTION, required = false, defaultValue = "{}") BSONObject updateContentOption,
            HttpServletRequest request)
            throws IOException, ScmOmServerException, ScmInternalException {
        ScmOmInputStream newFileContent = new ScmOmInputStream(request.getInputStream());
        try {
            fileService.updateFileContent(session, wsName, fileId, siteName, updateContentOption,
                    newFileContent);
        }
        finally {
            CommonUtil.consumeAndCloseResource(newFileContent);
        }
    }

    @DeleteMapping("/files")
    public void deleteFiles(ScmOmSession session,
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestBody List<String> fileIdList)
            throws ScmOmServerException, ScmInternalException {
        fileService.deleteFiles(session, wsName, fileIdList);
    }
}
