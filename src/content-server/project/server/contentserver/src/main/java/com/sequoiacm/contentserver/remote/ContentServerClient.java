package com.sequoiacm.contentserver.remote;

import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.infrastructure.security.auth.RestField;

import feign.Response;

public interface ContentServerClient {

    // ***************system***************//
    @PostMapping(value = "/api/v1/reload-bizconf")
    public BasicBSONList reloadBizConf(
            @RequestParam(CommonDefine.RestArg.BIZ_RELOAD_SCOPE) int scope,
            @RequestParam(CommonDefine.RestArg.BIZ_RELOAD_ID) int id,
            @RequestParam(value = CommonDefine.RestArg.BIZ_RELOAD_METADATA_ONLY, required = false, defaultValue = "false") boolean isMetadataOnly)
            throws ScmServerException;

    // ***************task****************//
    @PostMapping(value = "/api/v1/tasks")
    public BSONObject startTask(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TYPE) int taskType,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_SERVER_ID) int serverId,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TARGET_SITE) String targetSite,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_OPTIONS) String options)
            throws ScmServerException;

    @PostMapping(value = "/api/v1/tasks/{taskId}/stop")
    public void stopTask(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @PathVariable("taskId") String taskId) throws ScmServerException;

    @PostMapping(value = "/internal/v1/tasks/{taskId}/notify")
    public void notifyTask(@PathVariable("taskId") String taskId,
            @RequestParam(CommonDefine.RestArg.TASK_NOTIFY_TYPE) int notifyType);

    // **************file******************//
    @GetMapping(value = "/api/v1/files/{fileId}")
    public Response downloadFile(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("fileId") String fileId,
            @RequestParam(CommonDefine.RestArg.FILE_MAJOR_VERSION) int mojorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag)
            throws ScmServerException;

    @DeleteMapping(value = "/api/v1/files/{fileId}")
    public void deleteFile(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("fileId") String fileId,
            @RequestParam(CommonDefine.RestArg.FILE_MAJOR_VERSION) int mojorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_IS_PHYSICAL) boolean isPhysical)
            throws ScmServerException;

    // **************datasource************//
    @DeleteMapping(value = "/internal/v1/datasource/{dataId}")
    public void deleteData(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime)
            throws ScmServerException;

    @GetMapping(value = "/internal/v1/datasource/{dataId}")
    public Response readData(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag)
            throws ScmServerException;

    @RequestMapping(value = "/internal/v1/datasource/{dataId}", method = RequestMethod.HEAD)
    public DataInfo headDataInfo(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime)
            throws ScmServerException;

    @DeleteMapping(value = "/internal/v1/datasource/tables")
    public void deleteDataTables(
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TABLE_NAMES) List<String> tableNames)
            throws ScmServerException;

    @DeleteMapping(value = "/api/v1/workspaces/{workspace_name}")
    public void deleteWorkspace(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @PathVariable("workspace_name") String wsName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_ENFORCED_DELETE) boolean isEnforced)
            throws ScmServerException;

    @PostMapping(value = "/api/v1/files/{file_id}?action=" + CommonDefine.RestArg.ACTION_CALC_MD5)
    public BSONObject calcMd5(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION) int majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion)
            throws ScmServerException;
}
