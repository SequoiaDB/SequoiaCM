package com.sequoiacm.contentserver.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.BreakpointFileJsonSerializer;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IBreakpointFileService;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;

@RestController
@RequestMapping("/api/v1")
public class BreakpointFileController {

    private static final Logger logger = LoggerFactory.getLogger(BreakpointFileController.class);

    private static final String BREAKPOINT_FILE_ATTRIBUTE = "X-SCM-BREAKPOINTFILE";

    @Autowired
    private ScmAudit audit;

    @Autowired
    private IBreakpointFileService breakpointFileService;

    private ObjectMapper mapper = new ObjectMapper();

    public BreakpointFileController() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BreakpointFile.class, new BreakpointFileJsonSerializer());
        mapper.registerModule(module);
    }

    @RequestMapping(value = "/breakpointfiles/{file_name}", method = RequestMethod.HEAD)
    public void findBreakpointFile(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName, HttpServletResponse response,
            Authentication auth) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.READ, "find breakpoint file");

        BreakpointFile file = breakpointFileService.getBreakpointFile(workspaceName, fileName);
        if (file == null) {
            throw new ScmFileNotFoundException(
                    String.format("BreakpointFile is not found: /%s/%s", workspaceName, fileName));
        }

        String jsonFile;
        try {
            jsonFile = mapper.writeValueAsString(file);
        }
        catch (JsonProcessingException e) {
            throw new ScmSystemException("Failed to serialize BreakpointFile to json", e);
        }
        audit.info(ScmAuditType.FILE_DQL, auth, workspaceName, 0,
                "find breakpoint file by file name=" + fileName);
        response.setHeader(BREAKPOINT_FILE_ATTRIBUTE, RestUtils.urlEncode(jsonFile));
    }

    @GetMapping("/breakpointfiles")
    public List<BreakpointFile> listBreakpointFiles(
            @RequestParam("workspace_name") String workspaceName,
            @RequestParam(value = "filter", required = false) BSONObject filter,
            Authentication auth) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.READ, "list breakpoint files");

        String message = "list breakpoint files";
        if (null != filter) {
            message += " by filter=" + filter.toString();
        }
        audit.info(ScmAuditType.FILE_DQL, auth, workspaceName, 0, message);
        return breakpointFileService.getBreakpointFiles(workspaceName, filter);
    }

    @PostMapping("/breakpointfiles/{file_name}")
    public BreakpointFile createBreakpointFile(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName,
            @RequestParam(value = "create_time", required = false) Long createTime,
            @RequestParam(value = "checksum_type", required = false) ChecksumType checksumType,
            @RequestParam(value = "is_last_content", required = false) Boolean isLastContent,
            @RequestParam(value = "is_need_md5", required = false, defaultValue = "false") boolean isNeedMd5,
            HttpServletRequest request, Authentication auth)
            throws ScmServerException, IOException {
        InputStream is = request.getInputStream();
        try {
            if (checksumType == null) {
                checksumType = ChecksumType.NONE;
            }

            if (isLastContent == null) {
                throw new ScmMissingArgumentException("Missing is_last_content");
            }

            if (null == createTime || 0 == createTime) {
                createTime = System.currentTimeMillis();
            }

            ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                    ScmPrivilegeDefine.CREATE, "create breakpoint file");
            audit.info(ScmAuditType.CREATE_FILE, auth, workspaceName, 0,
                    "create breakpoint file, fileName=" + fileName);
            return breakpointFileService.createBreakpointFile(auth.getName(), workspaceName,
                    fileName, createTime, checksumType, is, isLastContent, isNeedMd5);
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }
    }

    @PutMapping("/breakpointfiles/{file_name}")
    public BreakpointFile uploadBreakpointFile(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName,
            @RequestParam(value = "is_last_content") boolean isLastContent,
            @RequestParam("offset") long offset, HttpServletRequest request, Authentication auth)
            throws ScmServerException, IOException {
        InputStream fileStream = request.getInputStream();
        try {
            ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                    ScmPrivilegeDefine.CREATE, "upload breakpoint file");
            audit.info(ScmAuditType.CREATE_FILE, auth, workspaceName, 0,
                    "upload breakpoint file, fileName=" + fileName);
            return breakpointFileService.uploadBreakpointFile(auth.getName(), workspaceName,
                    fileName, fileStream, offset, isLastContent);
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(fileStream);
        }
    }

    @DeleteMapping("/breakpointfiles/{file_name}")
    public void deleteBreakpointFile(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName, Authentication auth)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.DELETE, "delete breakpoint file");
        breakpointFileService.deleteBreakpointFile(workspaceName, fileName);
        audit.info(ScmAuditType.DELETE_FILE, auth, workspaceName, 0,
                "delete breakpoint file, fileName=" + fileName);
    }

    @PostMapping(value = "/breakpointfiles/{file_name}", params = "action="
            + CommonDefine.RestArg.ACTION_CALC_MD5)
    public BSONObject calcMd5(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName, Authentication auth,
            HttpServletResponse response) throws ScmServerException, UnsupportedEncodingException {
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.UPDATE, "calculate breakpoint file md5");
        String md5 = breakpointFileService.calcBreakpointFileMd5(workspaceName, fileName);
        audit.info(ScmAuditType.UPDATE_FILE, auth, workspaceName, 0,
                "calculate breakpoint file md5, fileName=" + fileName);
        return new BasicBSONObject(FieldName.BreakpointFile.FIELD_MD5, md5);
    }
}
