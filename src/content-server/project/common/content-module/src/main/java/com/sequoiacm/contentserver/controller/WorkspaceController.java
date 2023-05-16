package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.model.ClientWorkspaceUpdator;
import com.sequoiacm.contentserver.service.IWorkspaceService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.sdbversion.RequireSdbVersion;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceController.class);

    private final IWorkspaceService workspaceService;

    @Autowired
    private ScmAudit audit;

    @Autowired
    public WorkspaceController(IWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping(value = "/workspaces/{workspace_name}")
    public ResponseEntity<Map<String, Object>> workspace(
            @PathVariable("workspace_name") String workspaceName, Authentication auth)
            throws ScmServerException {
        logger.debug(String.format("getting the workspace:%s", workspaceName));

        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject workspace = workspaceService.getWorkspace(user, workspaceName);

        Map<String, Object> result = new HashMap<>(1);
        result.put(CommonDefine.RestArg.GET_WORKSPACE_REPS, toClientWsBSON(workspace));
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/workspaces")
    public void workspaceList(
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_FILTER, required = false) BSONObject filter,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_ORDERBY, required = false) BSONObject orderBy,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        if (skip < 0) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT, "skip can not be less than 0");
        }
        if (limit < -1) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                    "limit can not be less than -1");
        }

        ScmUser user = (ScmUser) auth.getPrincipal();
        MetaCursor cursor = workspaceService.getWorkspaceList(user, filter, orderBy, skip, limit);
        ServiceUtils.putCursorToResponse(cursor, response);

    }

    @PostMapping(value = "/workspaces/{workspace_name}")
    public BSONObject createWorkspace(@PathVariable("workspace_name") String newWsName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_CONF) BSONObject newWsConf,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser scmUser = (ScmUser) auth.getPrincipal();
        if (!ScmArgChecker.Workspace.checkWorkspaceName(newWsName)) {
            throw new ScmInvalidArgumentException("invalid workspace name:name=" + newWsName);
        }

        BSONObject newWs = workspaceService.createWorkspace(scmUser, newWsName, newWsConf,
                auth.getName());
        return new BasicBSONObject(CommonDefine.RestArg.GET_WORKSPACE_REPS, toClientWsBSON(newWs));

    }

    @DeleteMapping(value = "/workspaces/{workspace_name}")
    public void deleteWorkspace(@PathVariable("workspace_name") String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_ENFORCED_DELETE, defaultValue = "false") boolean isEnforced,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String token,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser scmUser = (ScmUser) auth.getPrincipal();
        workspaceService.deleteWorkspace(sessionId, token, scmUser, workspaceName, isEnforced);

    }

    @PutMapping(value = "/workspaces/{workspace_name}")
    public BSONObject updateWorkspace(@PathVariable("workspace_name") String workspaceName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_UPDATOR) BSONObject updator,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser scmUser = (ScmUser) auth.getPrincipal();

        ClientWorkspaceUpdator clientWsUpdator = ClientWorkspaceUpdator.fromBSONObject(updator);
        BSONObject config = workspaceService.updateWorkspace(scmUser, workspaceName,
                clientWsUpdator);

        return new BasicBSONObject(CommonDefine.RestArg.GET_WORKSPACE_REPS, toClientWsBSON(config));
    }

    @RequestMapping(value = "/workspaces", method = RequestMethod.HEAD)
    public ResponseEntity<String> countWorkspace(
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_FILTER, required = false) BSONObject condition,
            HttpServletResponse response, Authentication auth) throws ScmServerException {

        ScmUser user = (ScmUser) auth.getPrincipal();
        long count = workspaceService.countWorkspace(user, condition);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok("");
    }

    @RequireSdbVersion(versionProperty = "scm.tag.sdbRequiredVersion", defaultVersion = "3.6.1")
    @PutMapping(value = "/workspaces/{workspace_name}", params = "action=enable_tag_retrieval")
    public BSONObject workspaceEnableTagRetrieval(@PathVariable("workspace_name") String wsName,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject config = workspaceService.enableTagRetrieval(user, wsName);
        return new BasicBSONObject(CommonDefine.RestArg.GET_WORKSPACE_REPS, toClientWsBSON(config));
    }

    @PutMapping(value = "/workspaces/{workspace_name}", params = "action=disable_tag_retrieval")
    public BSONObject workspaceDisableTagRetrieval(@PathVariable("workspace_name") String wsName,
            Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject config = workspaceService.disabledTagRetrieval(user, wsName);
        return new BasicBSONObject(CommonDefine.RestArg.GET_WORKSPACE_REPS, toClientWsBSON(config));
    }

    private BSONObject toClientWsBSON(BSONObject wsRec) {
        BSONObject clientWsBSON = new BasicBSONObject();
        clientWsBSON.putAll(wsRec);

        BSONObject metaLocation = (BSONObject) wsRec.get(FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        BSONObject clientMetaLocation = new BasicBSONObject();
        clientMetaLocation.putAll(metaLocation);
        int metaSiteId = (int) clientMetaLocation.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
        ScmSite metaSite = ScmContentModule.getInstance().getSiteInfo(metaSiteId);
        clientMetaLocation.put(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME,
                metaSite.getName());
        clientMetaLocation.put(CommonDefine.RestArg.WORKSPACE_LOCATION_TYPE,
                metaSite.getMetaUrl().getType());
        clientWsBSON.put(FieldName.FIELD_CLWORKSPACE_META_LOCATION, clientMetaLocation);

        BasicBSONList datalocations = (BasicBSONList) wsRec
                .get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        BasicBSONList clientDataLocations = new BasicBSONList();
        for (Object datalocation : datalocations) {
            BSONObject clientDataLocation = new BasicBSONObject();
            clientDataLocation.putAll((BSONObject) datalocation);
            int dataSiteId = (int) clientDataLocation
                    .get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
            ScmSite dataSite = ScmContentModule.getInstance().getSiteInfo(dataSiteId);
            clientDataLocation.put(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME,
                    dataSite.getName());
            clientDataLocation.put(CommonDefine.RestArg.WORKSPACE_LOCATION_TYPE,
                    dataSite.getDataUrl().getType());
            clientDataLocations.add(clientDataLocation);
        }
        clientWsBSON.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, clientDataLocations);
        return clientWsBSON;
    }
}
