package com.sequoiacm.om.omserver.controller;

import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileContent;
import com.sequoiacm.om.omserver.module.OmFileDetail;
import com.sequoiacm.om.omserver.service.ScmFileService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmFileController {

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ScmFileService fileService;

    @RequestMapping(value = "/files/id/{file_id}", method = RequestMethod.HEAD)
    public ResponseEntity<Object> getFileDetail(@PathVariable("file_id") String fileId,
            @RequestParam(value = RestParamDefine.MAJOR_VERSION, defaultValue = "1") int majorVersion,
            @RequestParam(value = RestParamDefine.MINOR_VERSION, defaultValue = "0") int minorVersion,
            @RequestParam(RestParamDefine.WORKSPACE) String workspace, ScmOmSession session)
            throws JsonProcessingException, ScmInternalException, ScmOmServerException {
        OmFileDetail fileDetail = fileService.getFileDetail(session, workspace, fileId,
                majorVersion, minorVersion);
        return ResponseEntity.ok()
                .header(RestParamDefine.FILE, mapper.writeValueAsString(fileDetail)).build();
    }

    @GetMapping(value = "/files/id/{file_id}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("file_id") String fileId,
            @RequestParam(value = RestParamDefine.MAJOR_VERSION, defaultValue = "1") int majorVersion,
            @RequestParam(value = RestParamDefine.MINOR_VERSION, defaultValue = "0") int minorVersion,
            @RequestParam(RestParamDefine.WORKSPACE) String workspace, ScmOmSession session)
            throws ScmInternalException, ScmOmServerException, IOException {
        OmFileContent fileContent = fileService.downloadFile(session, workspace, fileId,
                majorVersion, minorVersion);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment;fileName=" + fileContent.getFileName())
                .contentLength(fileContent.getFileLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(fileContent.getFileContent()));
    }

    @GetMapping("/files")
    public List<OmFileBasic> getFileList(ScmOmSession session,
            @RequestParam(value = RestParamDefine.WORKSPACE) String ws,
            @RequestParam(value = RestParamDefine.CONDITION, required = false, defaultValue = "{}") BSONObject condition,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit)
            throws ScmInternalException, ScmOmServerException {
        return fileService.getFileList(session, ws, condition, skip, limit);
    }
}
