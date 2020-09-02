package com.sequoiacm.contentserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnauthorizedException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.model.ClientWorkspaceUpdator;
import com.sequoiacm.contentserver.service.IWorkspaceService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.metasource.MetaCursor;

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
        BSONObject workspace = workspaceService.getWorkspace(workspaceName);

        audit.info(ScmAuditType.WS_DQL, auth, workspaceName, 0,
                "get workspace by workspaceName=" + workspaceName);

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
        MetaCursor cursor = workspaceService.getWorkspaceList(filter, orderBy, skip, limit);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
        String auditMessage = "";
        if (filter != null) {
            auditMessage += " by filter=" + filter.toString();
        }
        audit.info(ScmAuditType.WS_DQL, auth, null, 0, "get workspace list" + auditMessage);
    }

    @PostMapping(value = "/workspaces/{workspace_name}")
    public BSONObject createWorkspace(@PathVariable("workspace_name") String newWsName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_CONF) BSONObject newWsConf,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser scmUser = (ScmUser) auth.getPrincipal();
        if (!scmUser.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            throw new ScmOperationUnauthorizedException("permission denied:user=" + auth.getName());
        }
        if (!ScmArgChecker.Workspace.checkWorkspaceName(newWsName)) {
            throw new ScmInvalidArgumentException("invalid workspace name:name=" + newWsName);
        }

        BSONObject newWs = workspaceService.createWorkspace(newWsName, newWsConf, auth.getName());
        audit.info(ScmAuditType.CREATE_WS, auth, newWsName, 0,
                "create workspace by wsconf : " + newWsConf.toString());
        return new BasicBSONObject(CommonDefine.RestArg.GET_WORKSPACE_REPS, toClientWsBSON(newWs));

    }

    @DeleteMapping(value = "/workspaces/{workspace_name}")
    public void deleteWorkspace(@PathVariable("workspace_name") String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_ENFORCED_DELETE, defaultValue = "false") boolean isEnforced,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String token,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser scmUser = (ScmUser) auth.getPrincipal();
        if (!scmUser.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            throw new ScmOperationUnauthorizedException("permission denied:user=" + auth.getName());
        }
        workspaceService.deleteWorkspace(sessionId, token, scmUser, workspaceName, isEnforced);
        audit.info(ScmAuditType.DELETE_WS, auth, workspaceName, 0,
                "delete workspace: " + workspaceName + ", isEnforced=" + isEnforced);

    }

    @PutMapping(value = "/workspaces/{workspace_name}")
    public BSONObject updateWorkspace(@PathVariable("workspace_name") String workspaceName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_UPDATOR) BSONObject updator,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        ScmUser scmUser = (ScmUser) auth.getPrincipal();
        if (!scmUser.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            audit.info(ScmAuditType.DELETE_WS, auth, workspaceName,
                    ScmError.OPERATION_UNAUTHORIZED.getErrorCode(),
                    "update workspace failed, permission denied:user=" + auth.getName());
            throw new ScmOperationUnauthorizedException("permission denied:user=" + auth.getName());
        }
        ClientWorkspaceUpdator clientWsUpdator = ClientWorkspaceUpdator.fromBSONObject(updator);
        BSONObject config = workspaceService.updateWorkspace(workspaceName, clientWsUpdator);
        audit.info(ScmAuditType.UPDATE_WS, auth, workspaceName, 0,
                "update workspace:" + workspaceName + ", updator=" + updator);
        return new BasicBSONObject(CommonDefine.RestArg.GET_WORKSPACE_REPS, toClientWsBSON(config));
    }

    private BSONObject toClientWsBSON(BSONObject wsRec) {
        BSONObject clientWsBSON = new BasicBSONObject();
        clientWsBSON.putAll(wsRec);

        BSONObject metaLocation = (BSONObject) wsRec.get(FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        BSONObject clientMetaLocation = new BasicBSONObject();
        clientMetaLocation.putAll(metaLocation);
        int metaSiteId = (int) clientMetaLocation.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
        ScmSite metaSite = ScmContentServer.getInstance().getSiteInfo(metaSiteId);
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
            ScmSite dataSite = ScmContentServer.getInstance().getSiteInfo(dataSiteId);
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
