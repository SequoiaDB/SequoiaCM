package com.sequoiacm.om.omserver.controller;

import java.util.Collections;
import java.util.List;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.service.ScmSiteService;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1")
public class ScmSiteController {
    @Autowired
    private ScmSiteService service;

    @GetMapping("/sites")
    public List<OmSiteInfo> siteList(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "-1") long limit,
            @RequestParam(value = RestParamDefine.FILTER, required = false, defaultValue = "{}") BSONObject filter,
            HttpServletResponse response) throws ScmOmServerException, ScmInternalException {
        long siteCount = service.getSiteCount(session, filter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(siteCount));
        if (siteCount <= 0) {
            return Collections.emptyList();
        }
        return service.getSiteList(session, filter, skip, limit);
    }

    @GetMapping("/sites/strategy")
    public String siteStrategy(ScmOmSession session) throws ScmInternalException {
        return service.getSiteStrategy(session);
    }

}
