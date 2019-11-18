package com.sequoiacm.om.omserver.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmWorkspaceController {
    @Autowired
    private ScmWorkspaceService service;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("/workspaces/{workspace_name}")
    public OmWorkspaceInfoWithStatistics getWorkspaceDetailWithStatistics(
            @PathVariable("workspace_name") String workspaceName, ScmOmSession session)
            throws ScmInternalException, ScmOmServerException {
        return service.getWorksapceDetailWithStatistics(session, workspaceName);
    }

    @GetMapping("/workspaces")
    public List<OmWorkspaceBasicInfo> getWorkspaceList(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit)
            throws ScmInternalException, ScmOmServerException {
        return service.getWorkspaceList(session, skip, limit);
    }

    @RequestMapping(value = "/workspaces/{workspace_name}", method = RequestMethod.HEAD)
    public ResponseEntity<Object> getWorkspaceDetail(
            @PathVariable("workspace_name") String workspaceName, ScmOmSession session)
            throws ScmInternalException, ScmOmServerException, JsonProcessingException {
        OmWorkspaceDetail ws = service.getWorksapceDetail(session, workspaceName);
        return ResponseEntity.ok().header(RestParamDefine.WORKSPACE, mapper.writeValueAsString(ws))
                .build();
    }

}
