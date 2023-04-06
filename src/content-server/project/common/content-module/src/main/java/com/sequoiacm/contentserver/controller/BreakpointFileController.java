package com.sequoiacm.contentserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.BreakpointFileJsonSerializer;
import com.sequoiacm.contentserver.service.IBreakpointFileService;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsBreakpointFileMeta;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

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
        ScmUser user = (ScmUser) auth.getPrincipal();

        BreakpointFile file = breakpointFileService.getBreakpointFile(user, workspaceName, fileName);
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
        response.setHeader(BREAKPOINT_FILE_ATTRIBUTE, RestUtils.urlEncode(jsonFile));
    }

    @GetMapping("/breakpointfiles")
    public List<BreakpointFile> listBreakpointFiles(
            @RequestParam("workspace_name") String workspaceName,
            @RequestParam(value = "filter", required = false) BSONObject filter,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();

        return breakpointFileService.getBreakpointFiles(user, workspaceName, filter);
    }

    @PostMapping("/breakpointfiles/{file_name}")
    public BreakpointFile createBreakpointFile(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName,
            @RequestParam(value = "create_time", required = false) Long createTime,
            @RequestParam(value = "checksum_type", required = false) ChecksumType checksumType,
            @RequestParam(value = "is_last_content", required = false) Boolean isLastContent,
            @RequestParam(value = "is_need_md5", required = false, defaultValue = "false") boolean isNeedMd5,
            @RequestParam(value = "has_set_time", required = false) Boolean hasSetTime,
            @RequestHeader(value = ScmStatisticsDefine.STATISTICS_HEADER, required = false) String statisticsType,
            HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws ScmServerException, IOException {
        ScmUser user = (ScmUser) auth.getPrincipal();

        InputStream is = request.getInputStream();
        try {
            if (checksumType == null) {
                checksumType = ChecksumType.NONE;
            }

            if (isLastContent == null) {
                throw new ScmMissingArgumentException("Missing is_last_content");
            }

            if (null == hasSetTime || !hasSetTime) {
                if (null == createTime || 0 == createTime) {
                    createTime = System.currentTimeMillis();
                }
            }
            else {
                if (null == createTime) {
                    throw new ScmMissingArgumentException("Missing create_time");
                }
            }

            BreakpointFile breakpointFile = breakpointFileService.createBreakpointFile(
                   user, workspaceName, fileName, createTime, checksumType, is,
                    isLastContent, isNeedMd5);
            if (ScmStatisticsType.BREAKPOINT_FILE_UPLOAD.equals(statisticsType)) {
                addExtraHeader(response, breakpointFile, isLastContent);
            }
            return breakpointFile;
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }
    }

    @PutMapping("/breakpointfiles/{file_name}")
    public BreakpointFile uploadBreakpointFile(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName,
            @RequestParam(value = "is_last_content") boolean isLastContent,
            @RequestParam("offset") long offset,
            @RequestHeader(value = ScmStatisticsDefine.STATISTICS_HEADER, required = false) String statisticsType,
            HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws ScmServerException, IOException {
        ScmUser user = (ScmUser) auth.getPrincipal();

        InputStream fileStream = request.getInputStream();
        try {
            BreakpointFile breakpointFile = breakpointFileService.uploadBreakpointFile(
                  user, workspaceName, fileName, fileStream, offset, isLastContent);
            if (ScmStatisticsType.BREAKPOINT_FILE_UPLOAD.equals(statisticsType)) {
                addExtraHeader(response, breakpointFile, isLastContent);
            }
            return breakpointFile;
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(fileStream);
        }
    }

    @DeleteMapping("/breakpointfiles/{file_name}")
    public void deleteBreakpointFile(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName, Authentication auth)
            throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        breakpointFileService.deleteBreakpointFile(user, workspaceName, fileName);
    }

    @PostMapping(value = "/breakpointfiles/{file_name}", params = "action="
            + CommonDefine.RestArg.ACTION_CALC_MD5)
    public BSONObject calcMd5(@RequestParam("workspace_name") String workspaceName,
            @PathVariable("file_name") String fileName, Authentication auth,
            HttpServletResponse response) throws ScmServerException, UnsupportedEncodingException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        String md5 = breakpointFileService.calcBreakpointFileMd5(user, workspaceName, fileName);

        return new BasicBSONObject(FieldName.BreakpointFile.FIELD_MD5, md5);
    }

    private void addExtraHeader(HttpServletResponse response, BreakpointFile breakpointFile,
            boolean isLastContent) {
        ScmStatisticsBreakpointFileMeta fileMeta = new ScmStatisticsBreakpointFileMeta(
                breakpointFile.getFileName(), breakpointFile.getWorkspaceName(),
                breakpointFile.getCreateTime(), isLastContent);
        response.setHeader(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER, fileMeta.toJSON());
    }
}
