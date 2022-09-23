package com.sequoiacm.om.omserver.controller;

import java.util.Collections;
import java.util.List;

import com.sequoiacm.om.omserver.module.OmWorkspaceInfo;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1")
public class ScmWorkspaceController {
    @Autowired
    private ScmWorkspaceService service;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("/workspaces/{workspace_name:.+}")
    public OmWorkspaceInfoWithStatistics getWorkspaceDetailWithStatistics(
            @PathVariable("workspace_name") String workspaceName, ScmOmSession session)
            throws ScmInternalException, ScmOmServerException {
        return service.getWorksapceDetailWithStatistics(session, workspaceName);
    }

    @GetMapping("/workspaces")
    public List<OmWorkspaceBasicInfo> getWorkspaceList(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            @RequestParam(value = RestParamDefine.FILTER, required = false, defaultValue = "{}") BSONObject filter,
            @RequestParam(value = RestParamDefine.ORDERBY, required = false) BSONObject orderBy,
            @RequestParam(value = RestParamDefine.STRICT_MODE, required = false, defaultValue = "false") Boolean isStrictMode,
            HttpServletResponse response) throws ScmInternalException, ScmOmServerException {
        long workspaceCount = service.getWorkspaceCount(session, filter, isStrictMode);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(workspaceCount));
        if (workspaceCount <= 0) {
            return Collections.emptyList();
        }
        return service.getUserRelatedWsList(session, filter, orderBy, skip, limit, isStrictMode);
    }

    @RequestMapping(value = "/workspaces/{workspace_name:.+}", method = RequestMethod.HEAD)
    public ResponseEntity<Object> getWorkspaceDetail(
            @PathVariable("workspace_name") String workspaceName, ScmOmSession session)
            throws ScmInternalException, ScmOmServerException, JsonProcessingException {
        OmWorkspaceDetail ws = service.getWorkspaceDetail(session, workspaceName);
        return ResponseEntity.ok().header(RestParamDefine.WORKSPACE, mapper.writeValueAsString(ws))
                .build();
    }

    @PutMapping(value = "/workspaces/{workspace_name}")
    public void updateWorkspace(ScmOmSession session,
            @PathVariable("workspace_name") String workSpaceName, OmWorkspaceInfo workspaceInfo)
            throws ScmInternalException, ScmOmServerException {
        service.updateWorkspace(session, workSpaceName, workspaceInfo);
    }
}
