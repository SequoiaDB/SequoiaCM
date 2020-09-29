package com.sequoiacm.fulltext.server.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine.Scope;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.service.FulltextCursor;
import com.sequoiacm.fulltext.server.service.FulltextSearchCursor;
import com.sequoiacm.fulltext.server.service.FulltextService;
import com.sequoiacm.fulltext.server.service.ScmFileFulltextInfoCursor;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.fulltext.common.FultextRestCommonDefine;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.security.privilege.impl.EnableScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmWorkspaceResource;

@EnableScmPrivClient
@RestController
@RequestMapping("/api/v1")
public class FulltextIdxController {

    @Autowired
    private ScmPrivClient privClient;

    @Autowired
    private FulltextService service;

    @PostMapping(value = "fulltext", params = "action=create")
    public void createIndex(
            @RequestParam(value = FultextRestCommonDefine.REST_FILEMATCHER) BSONObject fileMatcher,
            @RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            @RequestParam(value = FultextRestCommonDefine.REST_INDEX_MONDE) ScmFulltextMode mode,
            Authentication auth) throws FullTextException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.ALL, "create fulltext index");
        service.createIndex(ws, fileMatcher, mode);
    }

    private void checkPriv(String ws, String username, ScmPrivilegeDefine priv, String opDesc)
            throws FullTextException {
        IResourceBuilder wsResBuilder = privClient
                .getResourceBuilder(ScmWorkspaceResource.RESOURCE_TYPE);
        IResource wsResource = wsResBuilder.fromStringFormat(ws);
        boolean isPermited = privClient.check(username, wsResource, priv.getFlag());
        if (!isPermited) {
            throw new FullTextException(ScmError.OPERATION_UNAUTHORIZED,
                    opDesc + " failed, do not have priority:user=" + username + ",ws=" + ws
                            + ", needPriv=" + priv.getName());
        }
    }

    @PostMapping(value = "fulltext", params = "action=update")
    public void updateIndex(
            @RequestParam(value = FultextRestCommonDefine.REST_FILEMATCHER, required = false) BSONObject newFileMatcher,
            @RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            @RequestParam(value = FultextRestCommonDefine.REST_INDEX_MONDE, required = false) ScmFulltextMode newMode,
            Authentication auth) throws FullTextException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.ALL, "create fulltext index");
        service.updateIndex(ws, newFileMatcher, newMode);
    }

    @PostMapping(value = "fulltext", params = "action=drop")
    public void dropIndex(@RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            Authentication auth) throws FullTextException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.ALL, "drop fulltext index");
        service.dropIndex(ws);
    }

    @PostMapping(value = "fulltext", params = "action=inspect")
    public void inspectIndex(@RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            Authentication auth) throws FullTextException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.ALL, "inspect fulltext index");
        service.inspect(ws);
    }

    @PostMapping(value = "fulltext", params = "action=rebuild")
    public void rebuildIndex(@RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            @RequestParam(value = FultextRestCommonDefine.REST_FILE_ID) String fileId,
            Authentication auth) throws FullTextException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.ALL, "inspect fulltext index");
        service.rebuildIndex(ws, fileId);
    }

    @GetMapping(value = "fulltext", params = "action=get_file_idx_info")
    public void getFileIndexInfo(@RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            @RequestParam(FultextRestCommonDefine.REST_FILE_IDX_STATUS) ScmFileFulltextStatus status,
            Authentication auth, HttpServletResponse resp) throws FullTextException, IOException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.READ, "get fulltext index info");
        resp.setHeader("Content-Type", "application/json;charset=utf-8");
        ScmFileFulltextInfoCursor cursor = service.getFileIndexInfo(ws, status);
        putCursorToResponse(cursor, resp);
    }

    @GetMapping(value = "fulltext", params = "action=count_file")
    public void countFileWithIdxStatus(
            @RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            @RequestParam(FultextRestCommonDefine.REST_FILE_IDX_STATUS) ScmFileFulltextStatus status,
            Authentication auth, HttpServletResponse resp) throws FullTextException, IOException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.READ, "get fulltext index info");
        long count = service.countFileWithIdxStatus(ws, status);
        resp.setHeader("X-SCM-Count", String.valueOf(count));
    }

    @GetMapping(value = "fulltext", params = "action=get_idx_info")
    public ScmFulltexInfo getWorkspaceIndexInfo(
            @RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws, Authentication auth)
            throws FullTextException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.READ, "get fulltext index info");
        return service.getIndexInfo(ws);
    }

    @GetMapping(value = "fulltext", params = "action=search")
    public void search(@RequestParam(FultextRestCommonDefine.REST_WORKSPACE) String ws,
            @RequestParam(value = FultextRestCommonDefine.REST_SCOPE, required = false, defaultValue = Scope.SCOPE_CURRENT
                    + "") int scope,
            @RequestParam(FultextRestCommonDefine.REST_CONTENT_CONDITION) BSONObject contentCondition,
            @RequestParam(value = FultextRestCommonDefine.REST_FILE_CONDITION, required = false, defaultValue = "{}") BSONObject fileCondition,
            Authentication auth, HttpServletResponse resp) throws FullTextException {
        String username = auth.getName();
        checkPriv(ws, username, ScmPrivilegeDefine.READ, "fulltext search");
        resp.setHeader("Content-Type", "application/json;charset=utf-8");
        FulltextSearchCursor cursor = service.search(ws, scope, contentCondition, fileCondition);
        putCursorToResponse(cursor, resp);
    }

    public static void putCursorToResponse(FulltextCursor cursor, ServletResponse response)
            throws FullTextException {
        PrintWriter writer = null;
        try {
            int count = 0;
            if (cursor.hasNext()) {
                writer = getWriter(response);
                writer.write("[");
                while (true) {
                    cursor.writeNextToWriter(writer);
                    if (cursor.hasNext()) {
                        writer.write(",");
                    }
                    else {
                        break;
                    }
                    if (count++ == 50) {
                        if (writer.checkError()) {
                            throw new ScmServerException(ScmError.NETWORK_IO,
                                    "failed to write response to client because of ioexception");
                        }
                        count = 0;
                    }
                }
                writer.write("]");
            }
            else {
                writer = getWriter(response);
                writer.write("[]");
            }
            if (writer.checkError()) {
                throw new ScmServerException(ScmError.NETWORK_IO,
                        "failed to write response to client because of ioexception");
            }
        }
        catch (FullTextException e) {
            throw e;
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "traverse cursor failed", e);
        }
        finally {
            cursor.close();
            if (writer != null) {
                writer.flush();
            }
        }
    }

    public static PrintWriter getWriter(ServletResponse response) throws FullTextException {
        try {
            return response.getWriter();
        }
        catch (IOException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "Failed to get writer", e);
        }
    }

}
