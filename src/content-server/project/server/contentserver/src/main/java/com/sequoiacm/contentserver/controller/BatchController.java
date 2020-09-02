package com.sequoiacm.contentserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IBatchService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.metasource.MetaCursor;

@RestController
@RequestMapping("/api/v1")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    private final IBatchService batchService;

    @Autowired
    private ScmAudit audit;

    @Autowired
    public BatchController(IBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping(value = "/batches")
    public ResponseEntity create(String workspace_name,
            @RequestParam(value = CommonDefine.RestArg.BATCH_DESCRIPTION) BSONObject batchInfo,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace_name,
                ScmPrivilegeDefine.CREATE, "create batch");

        // e.g.
        // description={"name":"batch-test","properties":{"k1":"v1"},"tags":{"k1":"v1"}}
        if (!batchInfo.containsField(FieldName.Batch.FIELD_NAME)) {
            throw new ScmInvalidArgumentException(
                    "'" + FieldName.Batch.FIELD_NAME + "' is not specified in the argument:"
                            + CommonDefine.RestArg.BATCH_DESCRIPTION);
        }

        // check properties
        MetaDataManager.getInstence().checkPropeties(workspace_name,
                (String) batchInfo.get(FieldName.Batch.FIELD_CLASS_ID),
                (BSONObject) batchInfo.get(FieldName.Batch.FIELD_CLASS_PROPERTIES));

        logger.info("batch {} : {}", CommonDefine.RestArg.BATCH_DESCRIPTION, batchInfo.toString());

        String batchId = batchService.create(user, workspace_name, batchInfo);
        audit.info(ScmAuditType.CREATE_BATCH, auth, workspace_name, 0, "create batch, batchId="
                + batchId + ", batchName=" + (String) batchInfo.get(FieldName.Batch.FIELD_NAME));
        // return just created
        Map<String, Object> result = new HashMap<>(1);
        batchInfo = batchService.getBatchInfo(workspace_name, batchId, false);
        batchInfo.removeField("_id");
        result.put(CommonDefine.RestArg.BATCH_OBJECT, batchInfo);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/batches")
    public void list(String workspace_name,
            @RequestParam(value = CommonDefine.RestArg.BATCH_FILTER, required = false) BSONObject filter,
            @RequestParam(value = CommonDefine.RestArg.BATCH_ORDERBY, required = false) BSONObject orderBy,
            @RequestParam(value = CommonDefine.RestArg.BATCH_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = CommonDefine.RestArg.BATCH_LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        if (skip < 0) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT, "skip can not be less than 0");
        }
        if (limit < -1) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                    "limit can not be less than -1");
        }
        RestUtils.checkWorkspaceName(workspace_name);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspace_name,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "list batches");
        // e.g.
        // filter={"name":"batch-test","properties":{"k1":"v1"},"tags":{"k1":"v1"}}
        logger.info("batch {} : {}", CommonDefine.RestArg.BATCH_FILTER, filter);
        String message = "list batches";
        if (null != filter) {
            message += " by filter=" + filter.toString();
        }
        if (null != orderBy) {
            message += " order by=" + orderBy.toString();
        }
        message += " skip=" + skip + " limit=" + limit;
        audit.info(ScmAuditType.BATCH_DQL, auth, workspace_name, 0, message);

        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = batchService.getList(workspace_name, filter, orderBy, skip, limit);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }

    @GetMapping(value = "/batches/{batch_id}")
    public ResponseEntity batch(String workspace_name, @PathVariable("batch_id") String batchId,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspace_name,
                ScmPrivilegeDefine.READ, "get batch");
        BSONObject batchInfo = batchService.getBatchInfo(workspace_name, batchId, true);
        audit.info(ScmAuditType.BATCH_DQL, auth, workspace_name, 0, "get batch by batchId="
                + batchId + ", batchName=" + (String) batchInfo.get(FieldName.Batch.FIELD_NAME));

        batchInfo.removeField("_id");
        Map<String, Object> result = new HashMap<>(1);
        result.put(CommonDefine.RestArg.BATCH_OBJECT, batchInfo);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/batches/{batch_id}")
    public ResponseEntity delete(String workspace_name, @PathVariable("batch_id") String batchId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace_name,
                ScmPrivilegeDefine.DELETE, "delete batch");
        batchService.delete(sessionId, userDetail, user, workspace_name, batchId);
        audit.info(ScmAuditType.DELETE_BATCH, auth, workspace_name, 0,
                "delete batch by batchId=" + batchId);

        return ResponseEntity.ok("");
    }

    @PutMapping("/batches/{batch_id}")
    public ResponseEntity update(String workspace_name, @PathVariable("batch_id") String batchId,
            @RequestParam(CommonDefine.RestArg.BATCH_DESCRIPTION) BSONObject newBatchBson,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace_name,
                ScmPrivilegeDefine.UPDATE, "update batch");

        logger.info("batch update {} : {}", CommonDefine.RestArg.BATCH_DESCRIPTION,
                newBatchBson.toString());
        batchService.update(user, workspace_name, batchId, newBatchBson);
        audit.info(ScmAuditType.UPDATE_BATCH, auth, workspace_name, 0,
                "update batch by batchId=" + batchId + ", newBatchBson" + newBatchBson.toString());
        BSONObject batch = batchService.getBatchInfo(workspace_name, batchId, false);
        batch.removeField("_id");
        response.setHeader(CommonDefine.RestArg.BATCH_OBJECT, batch.toString());
        return ResponseEntity.ok(batch);
    }

    @PostMapping("/batches/{batch_id}/attachfile")
    public ResponseEntity attachFile(String workspace_name,
            @PathVariable("batch_id") String batchId, HttpServletRequest request,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace_name,
                ScmPrivilegeDefine.UPDATE, "attach batch's file");
        String fileId = getAndCheckParameter(request, CommonDefine.RestArg.BATCH_FILE_ID);
        batchService.attachFile(user, workspace_name, batchId, fileId);
        audit.info(ScmAuditType.UPDATE_BATCH, auth, workspace_name, 0,
                "attach batch's file batchId=" + batchId + ", fileId=" + fileId);
        return ResponseEntity.ok("");
    }

    @PostMapping("/batches/{batch_id}/detachfile")
    public ResponseEntity detachFile(String workspace_name,
            @PathVariable("batch_id") String batchId, HttpServletRequest request,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace_name,
                ScmPrivilegeDefine.UPDATE, "deattach batch's file");
        String fileId = getAndCheckParameter(request, CommonDefine.RestArg.BATCH_FILE_ID);
        batchService.detachFile(user, workspace_name, batchId, fileId);
        audit.info(ScmAuditType.UPDATE_BATCH, auth, workspace_name, 0,
                "detach batch's file batchId=" + batchId + ", fileId=" + fileId);
        return ResponseEntity.ok("");
    }

    private String getAndCheckParameter(HttpServletRequest request, String name)
            throws ScmMissingArgumentException {
        String val = request.getParameter(name);
        if (StringUtils.isEmpty(val)) {
            throw new ScmMissingArgumentException(name + " not present");
        }
        return val;
    }

    @RequestMapping(value = "/batches", method = RequestMethod.HEAD)
    public void getBatchCount(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "count directory");
        String message = "count batch";
        if (null != condition) {
            message += " by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.BATCH_DQL, auth, workspaceName, 0, message);
        long count = batchService.countBatch(workspaceName, condition);
        response.setHeader("X-SCM-Count", String.valueOf(count));
    }
}
