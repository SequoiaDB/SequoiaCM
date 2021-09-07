package com.sequoiacm.om.omserver.controller;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.ScmTaskService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TaskController {

    @Autowired
    private ScmTaskService service;

    @PostMapping(value = "/tasks/{task_id}", params = "action=" + RestParamDefine.ACTION_STOP_TASK)
    public void stopTask(ScmOmSession session, @PathVariable("task_id") String taskId)
            throws ScmOmServerException, ScmInternalException {
        service.stopTask(session, taskId);
    }

}
