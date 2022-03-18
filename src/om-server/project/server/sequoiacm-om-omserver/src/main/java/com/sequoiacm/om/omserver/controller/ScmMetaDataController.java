package com.sequoiacm.om.omserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmClassBasic;
import com.sequoiacm.om.omserver.module.OmClassDetail;
import com.sequoiacm.om.omserver.service.ScmMetaDataService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScmMetaDataController {

    @Autowired
    private ScmMetaDataService metaDataService;

    @GetMapping(value = { "/metadatas/classes/{class_id}" })
    public OmClassDetail getClassDetail(ScmOmSession session,
            @RequestParam(RestParamDefine.WORKSPACE) String wsName,
            @PathVariable("class_id") String classId) throws ScmServerException,
            JsonProcessingException, ScmOmServerException, ScmInternalException {
        return metaDataService.getClassDetail(session, wsName, classId);
    }

    @GetMapping("/metadatas/classes")
    public List<OmClassBasic> getClassList(ScmOmSession session,
            @RequestParam(value = RestParamDefine.WORKSPACE) String wsName,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") int skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            @RequestParam(value = RestParamDefine.FILTER, required = false, defaultValue = "{}") BSONObject filter,
            @RequestParam(value = RestParamDefine.ORDERBY, required = false) BSONObject orderBy,
            HttpServletResponse response) throws ScmInternalException, ScmOmServerException {
        long classCount = metaDataService.getClassCount(session, wsName, filter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(classCount));
        if (classCount <= 0) {
            return Collections.emptyList();
        }
        return metaDataService.listClass(session, wsName, filter, orderBy, skip, limit);
    }

}
