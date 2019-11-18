package org.sequoiacm.om.omserver.test;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBatchBasic;
import com.sequoiacm.om.omserver.module.OmBatchDetail;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmRoleInfo;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceBasicInfo;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfoWithStatistics;

import feign.Response;

public interface ScmOmClient {

    @PostMapping("/login")
    public Response login(@RequestParam(RestParamDefine.USERNAME) String username,
            @RequestParam(RestParamDefine.PASSWORD) String password) throws ScmInternalException;

    @PostMapping("/logout")
    public void logout(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId)
            throws ScmOmServerException;

    @PostMapping("/dock")
    public Response dock(@RequestParam(RestParamDefine.GATEWAY_ADDR) List<String> gatewayList,
            @RequestParam(RestParamDefine.REGION) String region,
            @RequestParam(RestParamDefine.ZONE) String zone,
            @RequestParam(RestParamDefine.USERNAME) String username,
            @RequestParam(RestParamDefine.PASSWORD) String encryptedPassword)
            throws ScmFeignException;

    @GetMapping("/api/v1/workspaces/{ws_name}")
    OmWorkspaceInfoWithStatistics getWorksapceDetailWithStatistics(
            @RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("ws_name") String workspaceName) throws ScmFeignException;

    @RequestMapping(value = "/api/v1/workspaces/{ws_name}", method = RequestMethod.HEAD)
    Response getScmWorkspaceDetail(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("ws_name") String workspaceName) throws ScmFeignException;

    @GetMapping("/api/v1/workspaces")
    public List<OmWorkspaceBasicInfo> getWorkspaceList(
            @RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(RestParamDefine.SKIP) long skip,
            @RequestParam(RestParamDefine.LIMIT) int limit) throws ScmFeignException;

    @RequestMapping(value = "/api/v1/files/id/{id}", method = RequestMethod.HEAD)
    Response getFileDetail(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(RestParamDefine.WORKSPACE) String ws, @PathVariable("id") String id,
            @RequestParam(RestParamDefine.MAJOR_VERSION) int majorVersion,
            @RequestParam(RestParamDefine.MINOR_VERSION) int minorVersion) throws ScmFeignException;

    @GetMapping(value = "/api/v1/files/id/{id}")
    Response downloadFile(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(RestParamDefine.WORKSPACE) String ws, @PathVariable("id") String id,
            @RequestParam(RestParamDefine.MAJOR_VERSION) int majorVersion,
            @RequestParam(RestParamDefine.MINOR_VERSION) int minorVersion) throws ScmFeignException;

    @GetMapping(value = "/api/v1/files/")
    List<OmFileBasic> getFileList(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(RestParamDefine.WORKSPACE) String ws,
            @RequestParam(RestParamDefine.CONDITION) BSONObject condition,
            @RequestParam(RestParamDefine.SKIP) long skip,
            @RequestParam(RestParamDefine.LIMIT) int limit) throws ScmFeignException;

    @GetMapping(value = "/api/v1/batches/{batch_id}")
    public OmBatchDetail getBatch(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(RestParamDefine.WORKSPACE) String ws,
            @PathVariable("batch_id") String batchId) throws ScmFeignException;

    @GetMapping(value = "/api/v1/batches")
    public List<OmBatchBasic> getBatchList(
            @RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(RestParamDefine.WORKSPACE) String ws,
            @RequestParam(RestParamDefine.CONDITION) BSONObject filter,
            @RequestParam(RestParamDefine.SKIP) long skip,
            @RequestParam(RestParamDefine.LIMIT) int limit) throws ScmFeignException;

    @GetMapping("/api/v1/users/{user_name}")
    public OmUserInfo getUserInfo(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username) throws ScmFeignException;

    @PostMapping("/api/v1/users/{user_name}")
    public void createUser(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.USER_TYPE, required = false, defaultValue = "LOCAL") String userType,
            @RequestParam(value = RestParamDefine.PASSWORD, required = false) String password)
            throws ScmFeignException;

    @DeleteMapping("/api/v1/users/{user_name}")
    public void deleteUser(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username) throws ScmFeignException;

    @PutMapping(value = "/api/v1/users/{user_name}", params = "action=change_password")
    public void changePassword(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.OLD_PASSWORD, required = false) String oldPassword,
            @RequestParam(value = RestParamDefine.NEW_PASSWORD, required = true) String newPassword,
            @RequestParam(value = RestParamDefine.CLEAB_SESSIONS, required = false, defaultValue = "false") boolean cleanSessions,
            @RequestParam(value = "action", required = true) String change_password)
            throws ScmFeignException;

    @PutMapping(value = "/api/v1/users/{user_name}", params = "action=grant_role")
    public void grantRoles(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.ROLES, required = true) List<String> roles,
            @RequestParam(value = "action", required = true) String grant_role)
            throws ScmFeignException;

    @PutMapping(value = "/api/v1/users/{user_name}", params = "action=revoke_role")
    public void revokeRoles(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.ROLES, required = true) List<String> roles,
            @RequestParam(value = "action", required = true) String revoke_role)
            throws ScmFeignException;

    @PutMapping(value = "/api/v1/users/{user_name}", params = "action=change_user_type")
    public void changeUserType(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username,
            @RequestParam(value = RestParamDefine.USER_TYPE, required = true) String userType,
            @RequestParam(value = RestParamDefine.NEW_PASSWORD, required = false) String newPassword,
            @RequestParam(value = RestParamDefine.OLD_PASSWORD, required = false) String oldPassword,
            @RequestParam(value = "action", required = true) String change_user_type)
            throws ScmFeignException;

    @PutMapping(value = "/api/v1/users/{user_name}", params = "action=disable")
    public void disableUser(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username,
            @RequestParam(value = "action", required = true) String disable)
            throws ScmFeignException;

    @PutMapping(value = "/api/v1/users/{user_name}", params = "action=enable")
    public void enableUser(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("user_name") String username,
            @RequestParam(value = "action", required = true) String enable)
            throws ScmFeignException;

    @GetMapping("/api/v1/users")
    public List<OmUserInfo> listUsers(
            @RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(value = RestParamDefine.CONDITION, required = false) BSONObject condition,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit)
            throws ScmFeignException;

    @GetMapping("/api/v1/roles/{role_name}")
    public OmRoleInfo getRole(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("role_name") String rolename) throws ScmFeignException;

    @PostMapping("/api/v1/roles/{role_name}")
    public void createRole(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("role_name") String rolename,
            @RequestParam(value = RestParamDefine.DESCRIPTION, required = false) String description)
            throws ScmFeignException;

    @DeleteMapping("/api/v1/roles/{role_name}")
    public void deleteRole(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("role_name") String rolename) throws ScmFeignException;

    @PutMapping(value = "/api/v1/roles/{role_name}", params = "action=grant")
    public void grantPrivilege(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("role_name") String rolename,
            @RequestParam(value = RestParamDefine.RESOURCE_TYPE, required = true) String resourceType,
            @RequestParam(value = RestParamDefine.RESOURCE, required = true) String resource,
            @RequestParam(value = RestParamDefine.PRIVILEGE, required = true) String privilege,
            @RequestParam(value = "action", required = true) String grant) throws ScmFeignException;

    @PutMapping(value = "/api/v1/roles/{role_name}", params = "action=revoke")
    public void revokePrivilege(@RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @PathVariable("role_name") String rolename,
            @RequestParam(value = RestParamDefine.RESOURCE_TYPE, required = true) String resourceType,
            @RequestParam(value = RestParamDefine.RESOURCE, required = true) String resource,
            @RequestParam(value = RestParamDefine.PRIVILEGE, required = true) String privilege,
            @RequestParam(value = "action", required = true) String revoke)
            throws ScmFeignException;

    @GetMapping("/api/v1/roles")
    public List<OmRoleBasicInfo> listRoles(
            @RequestHeader(RestParamDefine.X_AUTH_TOKEN) String sessionId,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit)
            throws ScmFeignException;
}
