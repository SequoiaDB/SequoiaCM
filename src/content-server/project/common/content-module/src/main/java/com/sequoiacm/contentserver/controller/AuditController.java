package com.sequoiacm.contentserver.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.service.IAuditService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.metasource.MetaCursor;


@RestController
@RequestMapping("/api/v1")
public class AuditController {
    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);
    private final IAuditService auditService;

    @Autowired
    public AuditController(IAuditService auditService) {
        this.auditService = auditService;
    }
    
    @GetMapping("/audits")
    public void list(HttpServletRequest request, HttpServletResponse response,
            Authentication auth) throws ScmServerException {
        String filter = request.getParameter(CommonDefine.RestArg.AUDIT_FILTER);
        BSONObject condition = null;
        if (null != filter) {
            condition = (BSONObject) JSON.parse(filter);
        }
        else {
            condition = new BasicBSONObject();
        }
        logger.info("get audit list by filter=" + filter);
        
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = auditService.getList(condition);
        ServiceUtils.putCursorToResponse(cursor, response);
    }
            
}
