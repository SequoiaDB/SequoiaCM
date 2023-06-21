package com.sequoiacm.contentserver.remote;

import java.util.List;

import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.model.DataTableDeleteOption;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.web.bind.annotation.*;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.SecurityRestField;

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
    public BSONObject startTask(@RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TYPE) int taskType,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_SERVER_ID) int serverId,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TARGET_SITE) String targetSite,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_OPTIONS) String options,
            @RequestParam(value = CommonDefine.RestArg.IS_ASYNC_COUNT_FILE, required = false) Boolean isAsyncCountFile)
            throws ScmServerException;

    @PostMapping(value = "/api/v1/tasks/{taskId}/stop")
    public void stopTask(@RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String user,
            @PathVariable("taskId") String taskId) throws ScmServerException;

    @PostMapping(value = "/internal/v1/tasks/{taskId}/notify")
    public void notifyTask(@PathVariable("taskId") String taskId,
            @RequestParam(CommonDefine.RestArg.TASK_NOTIFY_TYPE) int notifyType,
            @RequestParam(CommonDefine.RestArg.IS_ASYNC_COUNT_FILE) Boolean isAsyncCountFile);

    // **************file******************//
    @GetMapping(value = "/api/v1/files/{fileId}")
    public Response downloadFile(@RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("fileId") String fileId,
            @RequestParam(CommonDefine.RestArg.FILE_MAJOR_VERSION) int mojorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag)
            throws ScmServerException;

    // **************file******************//
    @GetMapping(value = "/internal/v1/files/{fileId}")
    public Response downloadFileInternal(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("fileId") String fileId,
            @RequestParam(CommonDefine.RestArg.FILE_MAJOR_VERSION) int mojorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag)
            throws ScmServerException;

    @DeleteMapping(value = "/api/v1/files/{fileId}")
    public void deleteFile(@RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("fileId") String fileId,
            @RequestParam(CommonDefine.RestArg.FILE_MAJOR_VERSION) int mojorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_IS_PHYSICAL) boolean isPhysical)
            throws ScmServerException;

    @DeleteMapping(value = "/api/v1/buckets/{name}/files?action=delete_file")
    public BSONObject deleteFileInBucket(
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String userDetailJson,
            @PathVariable("name") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_NAME) String fileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_IS_PHYSICAL) boolean isPhysical)
            throws ScmServerException;

    // **************datasource************//
    @DeleteMapping(value = "/internal/v1/datasource/{dataId}")
    public void deleteData(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION) Integer wsVersion,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME) String tableName)
            throws ScmServerException;

    @DeleteMapping(value = "/internal/v1/datasource/{dataId}?action=delete_data_in_site_list")
    public void deleteDataInSiteList(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST) List<Integer> siteList,
            @RequestBody(required = false) List<ScmFileLocation> siteLocationList)
            throws ScmServerException;

    @GetMapping(value = "/internal/v1/datasource/{dataId}")
    public Response readData(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION) Integer wsVersion,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME) String tableName)
            throws ScmServerException;

    @GetMapping(value = "/internal/v1/datasource/{dataId}")
    public Response readData(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(value = CommonDefine.RestArg.DATASOURCE_SITE_NAME, required = false) String targetSiteName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION) Integer wsVersion,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME) String tableName)
            throws ScmServerException;

    @RequestMapping(value = "/internal/v1/datasource/{dataId}", method = RequestMethod.HEAD)
    public DataInfo headDataInfo(
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_NAME) String siteName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("dataId") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int dataType,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long dataCreateTime,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION) int wsVersion,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME) String tableName)
            throws ScmServerException;

    @DeleteMapping(value = "/internal/v1/datasource/tables?" + CommonDefine.RestArg.KEEP_ALIVE
            + "=true")
    public String deleteDataTablesKeepAlive(
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TABLE_NAMES) List<String> tableNames,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_NAME, required = false) String wsName,
            @RequestBody(required = false) DataTableDeleteOption location)
            throws ScmServerException;

    @DeleteMapping(value = "/api/v1/workspaces/{workspace_name}")
    public String deleteWorkspace(@RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String user,
            @PathVariable("workspace_name") String wsName,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_ENFORCED_DELETE) boolean isEnforced,
            @RequestParam(CommonDefine.RestArg.KEEP_ALIVE) boolean isKeepAlive)
            throws ScmServerException;

    @PostMapping(value = "/api/v1/files/{file_id}?action=" + CommonDefine.RestArg.ACTION_CALC_MD5
            + "&" + CommonDefine.RestArg.KEEP_ALIVE + "=true")
    public BSONObject calcFileMd5KeepAlive(
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION) int majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion)
            throws ScmServerException;

    @PutMapping(value = "/internal/v1/datasource/{data_id}" + "?action="
            + CommonDefine.RestArg.ACTION_CALC_MD5 + "&" + CommonDefine.RestArg.KEEP_ALIVE
            + "=true")
    public BSONObject calcDataMd5KeepAlive(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_ID) int siteId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_WS_VERSION) Integer wsVersion,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_SITE_LIST_TABLE_NAME) String tableName)
            throws ScmServerException;
}
