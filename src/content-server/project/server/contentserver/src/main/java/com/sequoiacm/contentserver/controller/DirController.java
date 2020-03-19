package com.sequoiacm.contentserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.metasource.MetaCursor;

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

        String path = dirService.getDirPathById(workspace_name, dirId);
        ScmFileServicePriv.getInstance().checkDirPriority(auth.getName(), workspace_name, path,
                ScmPrivilegeDefine.READ, "get dir path by id");
        audit.info(ScmAuditType.DIR_DQL, auth, workspace_name, 0,
                "get dir path by dir id=" + dirId + ", path=" + path);
        Map<String, String> result = new HashMap<>(1);
        result.put(CommonDefine.Directory.SCM_REST_ARG_PATH, path);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/directories/{type}/**", method = RequestMethod.HEAD)
    public void getDirInfo(String workspace_name, @PathVariable("type") String type,
            HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        BSONObject dir;
        String authMessage = "get dir path info";
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            String dirPath = request.getRequestURI().substring(ignoreStr.length());
            dirPath = RestUtils.urlDecode(dirPath);
            dirPath = ScmSystemUtils.formatDirPath(dirPath);
            // check priority before search
            ScmFileServicePriv.getInstance().checkDirPriority(auth.getName(), workspace_name,
                    dirPath, ScmPrivilegeDefine.READ, "get dir path info by path");
            dir = dirService.getDirInfoByPath(workspace_name, dirPath);
            authMessage += " by path= " + dirPath;
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            String dirId = request.getRequestURI().substring(ignoreStr.length());
            // check priority before search
            ScmFileServicePriv.getInstance().checkDirPriorityById(auth.getName(), workspace_name,
                    dirService, dirId, ScmPrivilegeDefine.READ, "get dir path info by id");
            dir = dirService.getDirInfoById(workspace_name, dirId);
            authMessage += " by id=" + dirId;
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }
        dir.removeField("_id");
        audit.info(ScmAuditType.DIR_DQL, auth, workspace_name, 0,
                authMessage + ", dir info=" + dir.toString());
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
        String user = auth.getName();

        workspaceName = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_WORKSPACE_NAME);
        if (workspaceName == null) {
            throw new ScmMissingArgumentException(
                    "missing argument:" + CommonDefine.Directory.SCM_REST_ARG_WORKSPACE_NAME);
        }
        name = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_NAME);
        parentId = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_ID);
        path = request.getParameter(CommonDefine.Directory.SCM_REST_ARG_PATH);
        BSONObject newDir;
        String authMessage = "create dir";
        if (parentId != null && name != null) {
            ScmFileServicePriv.getInstance().checkDirPriorityById(auth.getName(), workspaceName,
                    dirService, parentId, ScmPrivilegeDefine.CREATE,
                    "create dir by parentIdAndName");
            newDir = dirService.createDirByPidAndName(user, workspaceName, name, parentId);
            authMessage += " by parentId=" + parentId + " and Name=" + name;
        }
        else if (path != null) {
            path = ScmSystemUtils.formatDirPath(path);
            ScmFileServicePriv.getInstance().checkDirPriority(auth.getName(), workspaceName, path,
                    ScmPrivilegeDefine.CREATE, "create dir by path");
            newDir = dirService.createDirByPath(user, workspaceName, path);
            authMessage += " by path=" + path;
        }
        else {
            throw new ScmInvalidArgumentException("missing required args");
        }
        audit.info(ScmAuditType.CREATE_DIR, auth, workspaceName, 0,
                authMessage + ", newDir=" + newDir.toString());
        return ResponseEntity.ok(newDir);
    }

    @RequestMapping(value = "/directories/{type}/**", method = RequestMethod.DELETE)
    public void deleteDir(String workspace_name, @PathVariable("type") String type,
            HttpServletRequest request, Authentication auth) throws ScmServerException {
        String dirId = null;
        String dirPath = null;
        String auditMessage = "delete dir";

        RestUtils.checkWorkspaceName(workspace_name);
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            dirPath = request.getRequestURI().substring(ignoreStr.length());
            dirPath = RestUtils.urlDecode(dirPath);
            dirPath = ScmSystemUtils.formatDirPath(dirPath);
            ScmFileServicePriv.getInstance().checkDirPriority(auth.getName(), workspace_name,
                    dirPath, ScmPrivilegeDefine.DELETE, "delete dir by path");
            auditMessage += " by path=" + dirPath;
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            dirId = request.getRequestURI().substring(ignoreStr.length());
            ScmFileServicePriv.getInstance().checkDirPriorityById(auth.getName(), workspace_name,
                    dirService, dirId, ScmPrivilegeDefine.DELETE, "delete dir by id");
            auditMessage += " by id=" + dirId;
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }
        dirService.deleteDir(workspace_name, dirId, dirPath);
        audit.info(ScmAuditType.DELETE_DIR, auth, workspace_name, 0, auditMessage);
    }

    @RequestMapping(value = "/directories/{type}/**/rename", method = RequestMethod.PUT)
    public ResponseEntity<Map<String, Long>> renameDir(@PathVariable("type") String type,
            String workspace_name, String name, HttpServletRequest request, Authentication auth)
            throws ScmServerException {
        String dirId;
        String dirPath;
        String user = auth.getName();

        RestUtils.checkWorkspaceName(workspace_name);
        long timestamp;
        String auditMessage = "rename dir";
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            dirPath = request.getRequestURI().substring(ignoreStr.length());
            ignoreStr = "rename";
            dirPath = dirPath.substring(0, dirPath.lastIndexOf(ignoreStr));
            dirPath = RestUtils.urlDecode(dirPath);
            dirPath = ScmSystemUtils.formatDirPath(dirPath);
            ScmFileServicePriv.getInstance().checkDirPriority(auth.getName(), workspace_name,
                    dirPath, ScmPrivilegeDefine.DELETE, "delete source when rename dir by path");

            ScmFileServicePriv.getInstance().checkDirPriorityByOldDirAndNewName(auth.getName(),
                    workspace_name, dirPath, name, ScmPrivilegeDefine.CREATE,
                    "create target when rename dir by path");
            timestamp = dirService.renameDirByPath(user, workspace_name, dirPath, name);
            auditMessage += " by dirPath=" + dirPath + ", update_time=" + timestamp;
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            dirId = request.getRequestURI().substring(ignoreStr.length());
            ignoreStr = "/rename";
            dirId = dirId.substring(0, dirId.lastIndexOf(ignoreStr));
            ScmFileServicePriv.getInstance().checkDirPriorityById(auth.getName(), workspace_name,
                    dirService, dirId, ScmPrivilegeDefine.DELETE,
                    "delete source when rename dir by id");
            ScmFileServicePriv.getInstance().checkDirPriorityByOldIdAndNewName(auth.getName(),
                    workspace_name, dirService, dirId, name, ScmPrivilegeDefine.CREATE,
                    "create target when rename dir by id");
            timestamp = dirService.reanmeDirById(user, workspace_name, dirId, name);
            auditMessage += " by dirId=" + dirId + ", update_time=" + timestamp;
        }
        else {
            throw new ScmInvalidArgumentException("unknown type:type=" + type);
        }
        audit.info(ScmAuditType.UPDATE_DIR, auth, workspace_name, 0, auditMessage);

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
        String user = auth.getName();
        String auditMessage = "";

        RestUtils.checkWorkspaceName(workspace_name);

        long timestamp;
        if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH)) {
            String ignoreStr = "/api/v1/directories/"
                    + CommonDefine.Directory.SCM_REST_ARG_TYPE_PATH;
            dirPath = request.getRequestURI().substring(ignoreStr.length());
            ignoreStr = "move";
            dirPath = dirPath.substring(0, dirPath.lastIndexOf(ignoreStr));
            dirPath = RestUtils.urlDecode(dirPath);
            dirPath = ScmSystemUtils.formatDirPath(dirPath);

            if (null != parent_directory_id) {
                ScmFileServicePriv.getInstance().checkDirPriorityByOldDirAndNewParentId(
                        auth.getName(), workspace_name, dirService, dirPath, parent_directory_id,
                        ScmPrivilegeDefine.CREATE, "create target when move dir by id");
                auditMessage += ", parent_directory_id" + parent_directory_id;
            }
            else {
                parent_directory_path = ScmSystemUtils.formatDirPath(parent_directory_path);
                ScmFileServicePriv.getInstance().checkDirPriorityByOldDirAndNewParentDir(
                        auth.getName(), workspace_name, dirPath, parent_directory_path,
                        ScmPrivilegeDefine.CREATE, "create target when move dir by path");
                auditMessage += ", parent_directory_path" + parent_directory_path;
            }

            ScmFileServicePriv.getInstance().checkDirPriority(auth.getName(), workspace_name,
                    dirPath, ScmPrivilegeDefine.DELETE, "delete source when move dir by path");

            timestamp = dirService.moveDirByPath(user, workspace_name, dirPath, parent_directory_id,
                    parent_directory_path);
            audit.info(ScmAuditType.UPDATE_DIR, auth, workspace_name, 0,
                    "move dir by dirPath=" + dirPath + ", update_time=" + timestamp + auditMessage);
        }
        else if (type.equals(CommonDefine.Directory.SCM_REST_ARG_TYPE_ID)) {
            String ignoreStr = "/api/v1/directories/" + CommonDefine.Directory.SCM_REST_ARG_TYPE_ID
                    + "/";
            dirId = request.getRequestURI().substring(ignoreStr.length());
            ignoreStr = "/move";
            dirId = dirId.substring(0, dirId.lastIndexOf(ignoreStr));

            if (null != parent_directory_id) {
                ScmFileServicePriv.getInstance().checkDirPriorityByOldIdAndNewParentId(
                        auth.getName(), workspace_name, dirService, dirId, parent_directory_id,
                        ScmPrivilegeDefine.CREATE, "create target when move dir by id");
                auditMessage += ", parent_directory_id" + parent_directory_id;
            }
            else {
                parent_directory_path = ScmSystemUtils.formatDirPath(parent_directory_path);
                ScmFileServicePriv.getInstance().checkDirPriorityByOldIdAndNewParentDir(
                        auth.getName(), workspace_name, dirService, dirId, parent_directory_path,
                        ScmPrivilegeDefine.CREATE, "create target when move dir by path");
                auditMessage += ", parent_directory_path" + parent_directory_path;
            }

            ScmFileServicePriv.getInstance().checkDirPriorityById(auth.getName(), workspace_name,
                    dirService, dirId, ScmPrivilegeDefine.DELETE,
                    "delete source when move dir by id");

            timestamp = dirService.moveDirById(user, workspace_name, dirId, parent_directory_id,
                    parent_directory_path);
            audit.info(ScmAuditType.UPDATE_DIR, auth, workspace_name, 0,
                    "move dir by dirId=" + dirId + ", update_time=" + timestamp + auditMessage);
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
        audit.info(ScmAuditType.DIR_DQL, auth, workspaceName, 0,
                "list directory, condition=" + condition.toString());
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = dirService.getDirList(workspaceName, condition, orderby, skip, limit);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
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

        ScmFileServicePriv.getInstance().checkDirPriorityById(auth.getName(), workspaceName,
                dirService, directoryId, ScmPrivilegeDefine.READ, "list directory's files");
        response.setHeader("Content-Type", "application/json;charset=utf-8");

        BSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLFILE_DIRECTORY_ID, directoryId);
        if (condition != null) {
            BasicBSONList arrayCond = new BasicBSONList();
            arrayCond.add(condition);
            arrayCond.add(matcher);

            matcher = new BasicBSONObject();
            matcher.put("$and", arrayCond);
        }

        audit.info(ScmAuditType.DIR_DQL, auth, workspaceName, 0,
                "list directory's files, directoryId=" + directoryId + ", matcher"
                        + matcher.toString());

        MetaCursor cursor = fileService.getFileList(workspaceName, matcher,
                CommonDefine.Scope.SCOPE_CURRENT, orderby, skip, limit, selector);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }

    @RequestMapping(value = "/directories", method = RequestMethod.HEAD)
    public void countDirectory(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "count directory");
        String message = "count direcotry";
        if (null != condition) {
            message += " by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.DIR_DQL, auth, workspaceName, 0, message);
        long count = dirService.countDir(workspaceName, condition);
        response.setHeader("X-SCM-Count", String.valueOf(count));
    }
}
