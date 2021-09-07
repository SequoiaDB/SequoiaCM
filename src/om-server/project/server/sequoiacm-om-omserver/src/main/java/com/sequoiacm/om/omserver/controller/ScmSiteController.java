package com.sequoiacm.om.omserver.controller;

import java.util.List;

import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.service.ScmSiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmSiteController {
    @Autowired
    private ScmSiteService service;

    @GetMapping("/sites")
    public List<OmSiteInfo> siteList(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException {
        return service.getSiteList(session);
    }

    @GetMapping("/sites/strategy")
    public String siteStrategy(ScmOmSession session) throws ScmInternalException {
        return service.getSiteStrategy(session);
    }

}
