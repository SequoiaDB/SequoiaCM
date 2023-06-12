package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.service.IBatchService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.common.SecurityRestField;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

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
        ScmUser user = (ScmUser) auth.getPrincipal();

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

        String batchId = batchService.create(user, workspace_name, batchInfo);

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
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (skip < 0) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT, "skip can not be less than 0");
        }
        if (limit < -1) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                    "limit can not be less than -1");
        }
        RestUtils.checkWorkspaceName(workspace_name);
        // e.g.
        // filter={"name":"batch-test","properties":{"k1":"v1"},"tags":{"k1":"v1"}}
        logger.info("batch {} : {}", CommonDefine.RestArg.BATCH_FILTER, filter);

        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = batchService.getList(user, workspace_name, filter, orderBy, skip,
                limit);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    @GetMapping(value = "/batches/{batch_id}")
    public ResponseEntity batch(String workspace_name, @PathVariable("batch_id") String batchId,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        ScmUser user = (ScmUser) auth.getPrincipal();
        BSONObject batchInfo = batchService.getBatchInfo(user, workspace_name, batchId, true);
        batchInfo.removeField("_id");
        Map<String, Object> result = new HashMap<>(1);
        result.put(CommonDefine.RestArg.BATCH_OBJECT, batchInfo);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/batches/{batch_id}")
    public ResponseEntity delete(String workspace_name, @PathVariable("batch_id") String batchId,
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        ScmUser user = (ScmUser) auth.getPrincipal();
        batchService.delete(sessionId, userDetail, user, workspace_name, batchId);
        return ResponseEntity.ok("");
    }

    @PutMapping("/batches/{batch_id}")
    public ResponseEntity update(String workspace_name, @PathVariable("batch_id") String batchId,
            @RequestParam(CommonDefine.RestArg.BATCH_DESCRIPTION) BSONObject newBatchBson,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        ScmUser user = (ScmUser) auth.getPrincipal();
        logger.info("batch update {} : {}", CommonDefine.RestArg.BATCH_DESCRIPTION,
                newBatchBson.toString());
        batchService.update(user, workspace_name, batchId, newBatchBson);
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
        ScmUser user = (ScmUser) auth.getPrincipal();
        String fileId = getAndCheckParameter(request, CommonDefine.RestArg.BATCH_FILE_ID);
        batchService.attachFile(user, workspace_name, batchId, fileId);

        return ResponseEntity.ok("");
    }

    @PostMapping("/batches/{batch_id}/detachfile")
    public ResponseEntity detachFile(String workspace_name,
            @PathVariable("batch_id") String batchId, HttpServletRequest request,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspace_name);
        ScmUser user = (ScmUser) auth.getPrincipal();
        String fileId = getAndCheckParameter(request, CommonDefine.RestArg.BATCH_FILE_ID);
        batchService.detachFile(user, workspace_name, batchId, fileId);
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
        ScmUser user = (ScmUser) auth.getPrincipal();

        long count = batchService.countBatch(user, workspaceName, condition);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
    }
}
