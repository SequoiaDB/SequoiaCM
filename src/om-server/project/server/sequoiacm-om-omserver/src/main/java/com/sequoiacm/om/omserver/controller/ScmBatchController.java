package com.sequoiacm.om.omserver.controller;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBatchBasic;
import com.sequoiacm.om.omserver.module.OmBatchDetail;
import com.sequoiacm.om.omserver.service.ScmBatchService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmBatchController {

    @Autowired
    private ScmBatchService batchService;

    @GetMapping("/batches/{batch_id}")
    public OmBatchDetail getBatchDetail(@PathVariable("batch_id") String batchId,
            @RequestParam(RestParamDefine.WORKSPACE) String workspace, ScmOmSession session)
            throws ScmInternalException, ScmOmServerException {
        return batchService.getBatch(session, workspace, batchId);
    }

    @GetMapping("/batches")
    public List<OmBatchBasic> getBatchesList(ScmOmSession session,
            @RequestParam(value = RestParamDefine.WORKSPACE) String ws,
            @RequestParam(value = RestParamDefine.CONDITION, required = false, defaultValue = "{}") BSONObject condition,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit)
            throws ScmInternalException, ScmOmServerException {
        return batchService.getBatchList(session, ws, condition, skip, limit);
    }
}
