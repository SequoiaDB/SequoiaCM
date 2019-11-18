package com.sequoiacm.contentserver.controller;

import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.service.IFileService;

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
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE, required = false, 
                    defaultValue = CommonDefine.Scope.SCOPE_CURRENT + "") Integer scope,
            HttpServletResponse response) throws ScmServerException {
        logger.info("internal get file delta: workspace={},filter={},scope={}", workspaceName,
                condition, scope);
        long count = fileService.countFiles(workspaceName, scope, condition);
        long sumSize = fileService.sumFileSizes(workspaceName, scope, condition);
        response.setHeader("X-SCM-Count", String.valueOf(count));
        response.setHeader("X-SCM-Sum", String.valueOf(sumSize));
        return ResponseEntity.ok("");
    }
}
