package com.sequoiacm.om.omserver.controller;

import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmConfPropsParam;
import com.sequoiacm.om.omserver.service.ScmConfigPropsService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1/config-props")
public class ScmConfigPropsController {

    @Autowired
    private ScmConfigPropsService propertiesService;

    @PutMapping
    public ScmUpdateConfResultSet updateProperties(ScmOmSession session, OmConfPropsParam config)
            throws ScmOmServerException, ScmInternalException {
        return propertiesService.updateProperties(session, config);
    }
}
