package com.sequoiacm.s3.remote;

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

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.security.auth.RestField;

import feign.Response;

public interface ContenServerService {
    @RequestMapping(value = "/api/v1/files", method = RequestMethod.GET)
    public List<BSONObject> getFileList(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @RequestParam(CommonDefine.RestArg.FILE_FILTER) BSONObject condition,
            @RequestParam(CommonDefine.RestArg.FILE_LIST_SCOPE) int scope,
            @RequestParam(CommonDefine.RestArg.FILE_ORDERBY) BSONObject orderby,
            @RequestParam(CommonDefine.RestArg.FILE_SKIP) long skip,
            @RequestParam(CommonDefine.RestArg.FILE_LIMIT) long limit,
            @RequestParam(CommonDefine.RestArg.FILE_SELECTOR) BSONObject selector)
            throws ScmFeignException;

    @RequestMapping(value = "/api/v1/files/path/{path}", method = RequestMethod.HEAD)
    public Response getFileByPath(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user, @PathVariable("path") String path,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName)
            throws ScmFeignException;

    @RequestMapping(value = "/api/v1/files/id/{id}", method = RequestMethod.HEAD)
    public Response getFileById(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user, @PathVariable("id") String id,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName)
            throws ScmFeignException;

    @GetMapping(value = "/api/v1/files/{fileId}")
    public Response downloadFile(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("fileId") String fileId) throws ScmFeignException;

    @DeleteMapping(value = "/api/v1/files/{fileId}")
    public void deleteFile(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @PathVariable("fileId") String fileId, @RequestParam("is_physical") boolean isPhysical)
            throws ScmFeignException;

    @PostMapping(value = "/api/v1/directories")
    public ScmDirInfo createDir(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam("workspace_name") String wsName, @RequestParam("path") String path)
            throws ScmFeignException;

    @RequestMapping(value = "/api/v1/directories/path/{path}", method = RequestMethod.DELETE)
    public void deleteDirByPath(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @PathVariable("path") String path) throws ScmFeignException;

    @RequestMapping(value = "/api/v1/directories/id/{id}", method = RequestMethod.DELETE)
    public void deleteDirById(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @PathVariable("id") String id) throws ScmFeignException;

    @RequestMapping(value = "/api/v1/directories/path/{path}", method = RequestMethod.HEAD)
    public Response getDirInfoByPath(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @PathVariable("path") String path) throws ScmFeignException;

    @RequestMapping(value = "/api/v1/directories/id/{id}", method = RequestMethod.HEAD)
    public Response getDirInfoById(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam("workspace_name") String workspace_name, @PathVariable("id") String id)
            throws ScmFeignException;

    @RequestMapping(value = "/api/v1/directories", method = RequestMethod.GET)
    public List<ScmDirInfo> listDir(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.FILE_FILTER) BSONObject condition,
            @RequestParam(CommonDefine.RestArg.FILE_LIMIT) int limit,
            @RequestParam(CommonDefine.RestArg.FILE_SKIP) int skip,
            @RequestParam(CommonDefine.RestArg.FILE_ORDERBY) BSONObject orderby)
            throws ScmFeignException;

    @RequestMapping(value = "/api/v1/directories/id/{directory_id}/listfiles", method = RequestMethod.GET)
    public List<ScmFileInfo> listFilesInDir(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @PathVariable("directory_id") String directoryId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.FILE_FILTER) BSONObject condition,
            @RequestParam(CommonDefine.RestArg.FILE_LIMIT) int limit,
            @RequestParam(CommonDefine.RestArg.FILE_SKIP) int skip,
            @RequestParam(CommonDefine.RestArg.FILE_ORDERBY) BSONObject orderby,
            @RequestParam(CommonDefine.RestArg.FILE_SELECTOR) BSONObject selector)
            throws ScmFeignException;

    @RequestMapping(value = "/api/v1/workspaces/{workspace_name}", method = RequestMethod.GET)
    public ScmWsInfo getWorkspace(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @PathVariable(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName)
            throws ScmFeignException;

    @RequestMapping(value = "/api/v1/workspaces", method = RequestMethod.GET)
    public List<ScmWsInfo> getWorkspace(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_FILTER) BSONObject filter,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_ORDERBY) BSONObject orderBy,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_SKIP) long skip,
            @RequestParam(value = CommonDefine.RestArg.WORKSPACE_LIMIT) long limit)
            throws ScmFeignException;

    @PostMapping("/api/v1/files")
    public ScmFileInfo uploadFile(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestHeader(value = CommonDefine.RestArg.FILE_DESCRIPTION) BSONObject desc,
            @RequestParam(value = CommonDefine.RestArg.FILE_BREAKPOINT_FILE) String breakpointFileName,
            @RequestParam(value = CommonDefine.RestArg.FILE_UPLOAD_CONFIG) BSONObject uploadConfig)
            throws ScmFeignException;

    @DeleteMapping("/api/v1/breakpointfiles/{file_name}")
    public void deleteBreakpointFile(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_name") String fileName) throws ScmFeignException;

    @PostMapping("/api/v1/metadatas/classes")
    public BSONObject createClass(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.METADATA_DESCRIPTION) BSONObject desc)
            throws ScmFeignException;

    @PostMapping("/api/v1/metadatas/attrs")
    public BSONObject createAttr(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.METADATA_DESCRIPTION) BSONObject desc)
            throws ScmFeignException;

    @GetMapping("/api/v1/metadatas/attrs")
    public List<BSONObject> listAttr(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.METADATA_FILTER) BSONObject filter)
            throws ScmFeignException;

    @PutMapping("/api/v1/metadatas/classes/{class_id}/attachattr/{attr_id}")
    public void attachAttr(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_id") String classId, @PathVariable("attr_id") String attrId)
            throws ScmFeignException;

    @GetMapping("/api/v1/metadatas/classes")
    public List<BSONObject> listClass(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.METADATA_FILTER) BSONObject filter)
            throws ScmFeignException;

    @GetMapping("/api/v1/files/{file_id}")
    public Response downloadFile(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_OFFSET) long offset,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_LENGTH) long length)
            throws ScmFeignException;
}
