package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DirController {
    private final IDirService dirService;
    private final IFileService fileService;

    @Autowired
    private ScmAudit audit;

    @Autowired
    public DirController(IDirService dirService, IFileService fileService) {
        this.dirService = dirService;
        this.fileService = fileService;
    }

    @RequestMapping(value = "/directories/id/{dir_id}/path", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getDirPathById(String workspace_name,
            @PathVariable("dir_id") String dirId, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        if (dirId == null) {
            throw new ScmInvalidArgumentException("missing required parameter:dir_id=null");
        }
        ScmUser user = (ScmUser) auth.getPrincipal();

        String path = dirService.getDirPathById(user, workspace_name, dirId);

        Map<String, String> result = new HashMap<>(1);
        result.put(CommonDefine.Directory.SCM_REST_ARG_PATH, path);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/directories/{type}/**", method = RequestMethod.HEAD)
    public void getDirInfo(String workspace_name, @PathVariable("type") String type,
            HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject dir;
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            String dirPath = RestUtils.getDecodePath(request.getRequestURI(), ignoreStr.length());
            dirPath = ScmSystemUtils.formatDirPath(dirPath);
            dir = dirService.getDirInfoByPath(user, workspace_name, dirPath);
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            String dirId = request.getRequestURI().substring(ignoreStr.length());
            dir = dirService.getDirInfoById(user, workspace_name, dirId);
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }
        dir.removeField("_id");
        String dirInfo = RestUtils.urlEncode(dir.toString());
        response.setHeader(CommonDefine.Directory.SCM_REST_ARG_DIRECTORY, dirInfo);
    }

    @RequestMapping(value = "/directories", method = RequestMethod.POST)
    public ResponseEntity<BSONObject> createDir(HttpServletRequest request, Authentication auth)
            throws ScmServerException {
        String workspaceName;
        String name;
        String path;
        String parentId;
        ScmUser user = (ScmUser) auth.getPrincipal();

        workspaceName = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_WORKSPACE_NAME);
        if (workspaceName == null) {
            throw new ScmMissingArgumentException(
                    "missing argument:" + CommonDefine.Directory.SCM_REST_ARG_WORKSPACE_NAME);
        }
        name = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_NAME);
        parentId = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_ID);
        path = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_PATH);
        BSONObject newDir;
        if (parentId != null && name != null) {
            newDir = dirService.createDirByPidAndName(user, workspaceName, name, parentId);
        }
        else if (path != null) {
            path = ScmSystemUtils.formatDirPath(path);
            newDir = dirService.createDirByPath(user, workspaceName, path);
        }
        else {
            throw new ScmInvalidArgumentException("missing required args");
        }

        return ResponseEntity.ok(newDir);
    }

    @RequestMapping(value = "/directories/{type}/**", method = RequestMethod.DELETE)
    public void deleteDir(String workspace_name, @PathVariable("type") String type,
            HttpServletRequest request, Authentication auth) throws ScmServerException {
        String dirId = null;
        String dirPath = null;
        ScmUser user = (ScmUser) auth.getPrincipal();
        RestUtils.checkWorkspaceName(workspace_name);
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            dirPath = RestUtils.getDecodePath(request.getRequestURI(), ignoreStr.length());
            dirPath = ScmSystemUtils.formatDirPath(dirPath);
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            dirId = request.getRequestURI().substring(ignoreStr.length());
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }
        dirService.deleteDir(user, workspace_name, dirId, dirPath);
    }

    @RequestMapping(value = "/directories/{type}/**/rename", method = RequestMethod.PUT)
    public ResponseEntity<Map<String, Long>> renameDir(@PathVariable("type") String type,
            String workspace_name, String name, HttpServletRequest request, Authentication auth)
            throws ScmServerException {
        String dirId;
        String dirPath;
        ScmUser user = (ScmUser) auth.getPrincipal();

        RestUtils.checkWorkspaceName(workspace_name);
        long timestamp;
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            dirPath = RestUtils.getDecodePath(request.getRequestURI(), ignoreStr.length());
            ignoreStr = "rename";
            dirPath = dirPath.substring(0, dirPath.lastIndexOf(ignoreStr));
            dirPath = ScmSystemUtils.formatDirPath(dirPath);
            timestamp = dirService.renameDirByPath(user, workspace_name, dirPath, name);
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            dirId = request.getRequestURI().substring(ignoreStr.length());
            ignoreStr = "/rename";
            dirId = dirId.substring(0, dirId.lastIndexOf(ignoreStr));
            timestamp = dirService.reanmeDirById(user, workspace_name, dirId, name);
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }
        Map<String, Long> result = new HashMap<>(1);
        result.put(FieldName.FIELD_CLDIR_UPDATE_TIME, timestamp);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/directories/{type}/**/move", method = RequestMethod.PUT)
    public ResponseEntity<Map<String, Long>> moveDir(@PathVariable("type") String type,
            String workspace_name, String parent_directory_path, String parent_directory_id,
            HttpServletRequest request, Authentication auth) throws ScmServerException {
        String dirId;
        String dirPath;
        ScmUser user = (ScmUser) auth.getPrincipal();
        String auditMessage = "";

        RestUtils.checkWorkspaceName(workspace_name);

        long timestamp;
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            dirPath = RestUtils.getDecodePath(request.getRequestURI(), ignoreStr.length());
            ignoreStr = "move";
            dirPath = dirPath.substring(0, dirPath.lastIndexOf(ignoreStr));
            dirPath = ScmSystemUtils.formatDirPath(dirPath);

            if (parent_directory_path != null) {
                parent_directory_path = ScmSystemUtils.formatDirPath(parent_directory_path);
            }

            timestamp = dirService.moveDirByPath(user, workspace_name, dirPath, parent_directory_id,
                    parent_directory_path);
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            dirId = request.getRequestURI().substring(ignoreStr.length());
            ignoreStr = "/move";
            dirId = dirId.substring(0, dirId.lastIndexOf(ignoreStr));

            if (null == parent_directory_id) {
                parent_directory_path = ScmSystemUtils.formatDirPath(parent_directory_path);
            }

            timestamp = dirService.moveDirById(user, workspace_name, dirId, parent_directory_id,
                    parent_directory_path);
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }

        Map<String, Long> result = new HashMap<>(1);
        result.put(FieldName.FIELD_CLDIR_UPDATE_TIME, timestamp);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/directories", method = RequestMethod.GET)
    public void listDir(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIMIT, required = false, defaultValue = "-1") int limit,
            @RequestParam(value = CommonDefine.RestArg.FILE_SKIP, required = false, defaultValue = "0") int skip,
            @RequestParam(value = CommonDefine.RestArg.FILE_ORDERBY, required = false) BSONObject orderby,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        if (condition == null) {
            condition = new BasicBSONObject();
        }
        ScmUser user = (ScmUser) auth.getPrincipal();
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = dirService.getDirList(user, workspaceName, condition, orderby, skip,
                limit);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    @RequestMapping(value = "/directories/id/{directory_id}/listfiles", method = RequestMethod.GET)
    public void listFiles(@PathVariable("directory_id") String directoryId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIMIT, required = false, defaultValue = "-1") int limit,
            @RequestParam(value = CommonDefine.RestArg.FILE_SKIP, required = false, defaultValue = "0") int skip,
            @RequestParam(value = CommonDefine.RestArg.FILE_ORDERBY, required = false) BSONObject orderby,
            @RequestParam(value = CommonDefine.RestArg.FILE_SELECTOR, required = false) BSONObject selector,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        ScmUser user = (ScmUser) auth.getPrincipal();
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = fileService.getDirSubFileList(user, workspaceName, directoryId, condition,
                CommonDefine.Scope.SCOPE_CURRENT, orderby, skip, limit, selector);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    @RequestMapping(value = "/directories", method = RequestMethod.HEAD)
    public void countDirectory(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmUser user = (ScmUser) auth.getPrincipal();
        long count = dirService.countDir(user, workspaceName, condition);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
    }
}
