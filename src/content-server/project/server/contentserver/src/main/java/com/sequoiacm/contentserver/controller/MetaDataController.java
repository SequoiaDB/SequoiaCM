package com.sequoiacm.contentserver.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metadata.AttrManager;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataAttrBsonConverter;
import com.sequoiacm.contentserver.model.MetadataClass;
import com.sequoiacm.contentserver.model.MetadataClassBsonConverter;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IMetaDataService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;

@RestController
@RequestMapping({ "/api/v1" })
public class MetaDataController {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataController.class);
    private IMetaDataService metadataService;

    @Autowired
    private ScmAudit audit;

    @Autowired
    public MetaDataController(IMetaDataService metadataService) {
        this.metadataService = metadataService;
    }

    @PostMapping({ "/metadatas/classes" })
    public MetadataClass createClass(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.METADATA_DESCRIPTION) BSONObject desc,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "create class");

        BSONObject checkedClassObj = checkClassObj(desc);

        logger.info("class {} : {}", CommonDefine.RestArg.METADATA_DESCRIPTION,
                checkedClassObj.toString());

        MetadataClass classObj = this.metadataService.createClass(user, workspaceName,
                checkedClassObj);
        audit.info(ScmAuditType.CREATE_META_CLASS, auth, workspaceName, 0,
                "create meta data class=" + checkedClassObj.toString());
        return classObj;
    }

    @GetMapping({ "/metadatas/classes" })
    public List<MetadataClass> listClass(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.METADATA_FILTER, required = false) BSONObject filter,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.READ, "list class info");
        String message = "list meta data class info";
        if (filter != null) {
            message += " by filter=" + filter.toString();
        }
        audit.info(ScmAuditType.META_CLASS_DQL, auth, workspaceName, 0, message);
        return this.metadataService.listClass(workspaceName, filter);
    }

    @RequestMapping(value = { "/metadatas/classes/{class_id}" }, method = { RequestMethod.GET })
    public MetadataClass getClassInfo(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_id") String classId, HttpServletRequest request,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.READ, "get class info with attr");
        audit.info(ScmAuditType.META_CLASS_DQL, auth, workspaceName, 0,
                "get class info with attr by classId=" + classId);
        return this.metadataService.getClassInfoWithAttr(workspaceName, classId);
    }

    @RequestMapping(value = { "/metadatas/classes/{class_name}" }, params = "type=name", method = {
            RequestMethod.GET })
    public MetadataClass getClassInfoByName(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_name") String className, HttpServletRequest request,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.READ, "get class info with attr");
        audit.info(ScmAuditType.META_CLASS_DQL, auth, workspaceName, 0,
                "get class info with attr by className=" + className);
        return this.metadataService.getClassInfoWithAttrByName(workspaceName, className);
    }

    @PutMapping({ "/metadatas/classes/{class_id}" })
    public void updateClass(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_id") String classId,
            @RequestParam(CommonDefine.RestArg.METADATA_DESCRIPTION) BSONObject desc,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "update class");

        checkClassUpdateInfo(desc);
        logger.info("class update {} : {}", CommonDefine.RestArg.METADATA_DESCRIPTION,
                desc.toString());

        MetadataClass newClass = this.metadataService.updateClass(user, workspaceName, classId,
                desc);
        MetadataClassBsonConverter converter = new MetadataClassBsonConverter();
        response.setHeader(CommonDefine.RestArg.METADATA_CLASSINFO_RESP,
                RestUtils.urlEncode(converter.convert(newClass).toString()));
        converter = null;
        audit.info(ScmAuditType.UPDATE_META_CLASS, auth, workspaceName, 0,
                "update class, classid=" + classId + ", description=" + desc.toString());
    }

    @DeleteMapping({ "/metadatas/classes/{class_id}" })
    public void deleteClass(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_id") String classId, Authentication auth)
            throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "delete class");

        this.metadataService.deleteClass(workspaceName, classId);
        audit.info(ScmAuditType.DELETE_META_CLASS, auth, workspaceName, 0,
                "delete class by classid=" + classId);
    }

    @DeleteMapping(value = { "/metadatas/classes/{class_name}" }, params = "type=name")
    public void deleteClassByName(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_name") String className, Authentication auth)
            throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "delete class");
        this.metadataService.deleteClassByName(workspaceName, className);
        audit.info(ScmAuditType.DELETE_META_CLASS, auth, workspaceName, 0,
                "delete class by className=" + className);
    }

    @PutMapping({ "/metadatas/classes/{class_id}/attachattr/{attr_id}" })
    public void attachAttr(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_id") String classId, @PathVariable("attr_id") String attrId,
            HttpServletRequest request, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "class attach attr");

        this.metadataService.attachAttr(user, workspaceName, classId, attrId);
        audit.info(ScmAuditType.UPDATE_META_CLASS, auth, workspaceName, 0,
                "attach attr , classid=" + classId + ", and attrId=" + attrId);
    }

    @PutMapping({ "/metadatas/classes/{class_id}/detachattr/{attr_id}" })
    public void detachAttr(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("class_id") String classId, @PathVariable("attr_id") String attrId,
            HttpServletRequest request, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "class detach attr");

        this.metadataService.detachAttr(user, workspaceName, classId, attrId);

        audit.info(ScmAuditType.UPDATE_META_CLASS, auth, workspaceName, 0,
                "detach attr , classid=" + classId + ", and attrId=" + attrId);
    }

    @PostMapping("/metadatas/attrs")
    public MetadataAttr createAttr(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.METADATA_DESCRIPTION) BSONObject desc,
            Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "create attr");

        BSONObject checkedAttrObj = checkAttrObj(desc);

        logger.info("attr {} : {}", CommonDefine.RestArg.METADATA_DESCRIPTION,
                checkedAttrObj.toString());
        audit.info(ScmAuditType.CREATE_META_ATTR, auth, workspaceName, 0,
                "create attr , desc=" + checkedAttrObj.toString());
        return this.metadataService.createAttr(user, workspaceName, checkedAttrObj);
    }

    @GetMapping({ "/metadatas/attrs" })
    public List<MetadataAttr> listAttr(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.METADATA_FILTER, required = false) BSONObject filter,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.READ, "list attr info");
        String message = "get class attr info list";
        if (filter != null) {
            message += " by filter=" + filter.toString();
        }
        audit.info(ScmAuditType.META_ATTR_DQL, auth, workspaceName, 0, message);
        return this.metadataService.listAttr(workspaceName, filter);
    }

    @RequestMapping(value = { "/metadatas/attrs/{attr_id}" }, method = { RequestMethod.GET })
    public MetadataAttr getAttrInfo(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("attr_id") String attrId, HttpServletRequest request, Authentication auth)
            throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.READ, "get attr info");
        audit.info(ScmAuditType.META_ATTR_DQL, auth, workspaceName, 0,
                "get attr info by attrId=" + attrId);
        return this.metadataService.getAttrInfo(workspaceName, attrId);
    }

    @PutMapping({ "/metadatas/attrs/{attr_id}" })
    public void updateAttr(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("attr_id") String attrId,
            @RequestParam(CommonDefine.RestArg.METADATA_DESCRIPTION) BSONObject desc,
            HttpServletResponse response, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "update attr");

        checkAttrUpdateInfo(workspaceName, attrId, desc);
        logger.info("attr update {} : {}", CommonDefine.RestArg.METADATA_DESCRIPTION,
                desc.toString());

        MetadataAttr newAttr = this.metadataService.updateAttr(user, workspaceName, attrId, desc);
        MetadataAttrBsonConverter converter = new MetadataAttrBsonConverter();
        response.setHeader(CommonDefine.RestArg.METADATA_ATTRINFO_RESP,
                RestUtils.urlEncode(converter.convert(newAttr).toString()));
        audit.info(ScmAuditType.UPDATE_META_ATTR, auth, workspaceName, 0,
                "update attr, attrId=" + attrId + ", desc=" + desc.toString());
    }

    @DeleteMapping({ "/metadatas/attrs/{attr_id}" })
    public void deleteAttr(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("attr_id") String attrId, Authentication auth) throws ScmServerException {
        RestUtils.checkWorkspaceName(workspaceName);
        String user = auth.getName();
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "delete attr");

        this.metadataService.deleteAttr(workspaceName, attrId);
        audit.info(ScmAuditType.DELETE_META_ATTR, auth, workspaceName, 0,
                "delete attr by attrId=" + attrId);
    }

    private BSONObject checkClassObj(BSONObject classObj) throws ScmServerException {
        BSONObject result = new BasicBSONObject();

        String fieldName = FieldName.Class.FIELD_NAME;
        if (!classObj.containsField(fieldName)) {
            throw new ScmInvalidArgumentException(
                    "'" + fieldName + "' is not specified in the argument:"
                            + CommonDefine.RestArg.METADATA_DESCRIPTION);
        }
        result.put(fieldName, ScmMetaSourceHelper.checkExistString(classObj, fieldName));

        fieldName = FieldName.Class.FIELD_DESCRIPTION;
        result.put(fieldName, getAndCheckStringArg(classObj, fieldName));

        return result;
    }

    private BSONObject checkAttrObj(BSONObject attrObj) throws ScmServerException {
        BSONObject result = new BasicBSONObject();

        String fieldName = FieldName.Attribute.FIELD_NAME;
        if (!attrObj.containsField(fieldName)) {
            throw new ScmInvalidArgumentException(
                    "'" + fieldName + "' is not specified in the argument "
                            + CommonDefine.RestArg.METADATA_DESCRIPTION);
        }
        String attrName = (String) checkExistString(attrObj, fieldName);
        MetaDataManager.getInstence().validateKeyFormat(attrName, "attr");
        result.put(fieldName, attrName);

        fieldName = FieldName.Attribute.FIELD_DISPLAY_NAME;
        result.put(fieldName, getAndCheckStringArg(attrObj, fieldName));

        fieldName = FieldName.Attribute.FIELD_DESCRIPTION;
        result.put(fieldName, getAndCheckStringArg(attrObj, fieldName));

        fieldName = FieldName.Attribute.FIELD_TYPE;
        if (!attrObj.containsField(fieldName)) {
            throw new ScmInvalidArgumentException(
                    "'" + fieldName + "' is not specified in the argument " + "description");
        }
        String type = (String) getAndCheckStringArg(attrObj, fieldName);
        AttrManager.getInstance().validType(type);
        AttributeType attrType = AttributeType.valueOf(type);
        result.put(fieldName, attrType.getName());

        fieldName = FieldName.Attribute.FIELD_CHECK_RULE;
        result.put(fieldName, parseCheckRule(attrObj, attrType));

        fieldName = FieldName.Attribute.FIELD_REQUIRED;
        if (attrObj.containsField(fieldName)) {
            Object obj = attrObj.get(fieldName);
            String str = String.valueOf(obj);
            if (!"true".equalsIgnoreCase(str) && !"false".equalsIgnoreCase(str)) {
                throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                        "field[" + fieldName + "] is not boolean format: " + str);
            }
            result.put(fieldName, Boolean.valueOf(Boolean.parseBoolean(str)));
        }
        else {
            result.put(fieldName, Boolean.valueOf(false));
        }
        return result;
    }

    private void checkClassUpdateInfo(BSONObject updateInfoObj) throws ScmServerException {
        Set<String> objFields = updateInfoObj.keySet();
        if (objFields.size() != 1) {
            throw new ScmInvalidArgumentException(
                    "invlid argument,updates only one property at a time:updator=" + updateInfoObj);
        }
        Set<String> availableFields = new HashSet<>();
        availableFields.add(FieldName.Class.FIELD_NAME);
        availableFields.add(FieldName.Class.FIELD_DESCRIPTION);
        for (String field : objFields) {
            if (!availableFields.contains(field)) {
                throw new ScmOperationUnsupportedException(
                        "field can't be modified:fieldName=" + field);
            }
        }
        if (updateInfoObj.containsField(FieldName.Class.FIELD_NAME)) {
            ScmMetaSourceHelper.checkExistString(updateInfoObj, FieldName.Class.FIELD_NAME);
        }
        if (updateInfoObj.containsField(FieldName.Class.FIELD_DESCRIPTION)) {
            checkExistString(updateInfoObj, FieldName.Class.FIELD_DESCRIPTION);
        }
    }

    private void checkAttrUpdateInfo(String wsName, String attrId, BSONObject updateInfoObj)
            throws ScmServerException {
        Set<String> objFields = updateInfoObj.keySet();
        if (objFields.size() != 1) {
            throw new ScmInvalidArgumentException(
                    "invlid argument,updates only one property at a time:updator=" + updateInfoObj);
            /*
             * if ((!objFields.contains("type")) ||
             * (!objFields.contains("check_rule"))) { throw new
             * ScmInvalidArgumentException(
             * "invlid argument,updates both properties is limited to 'type' and 'check_rule':updator="
             * + updateInfoObj); }
             */
        }
        Set<String> availableFields = new HashSet<>();
        availableFields.add(FieldName.Attribute.FIELD_DISPLAY_NAME);
        availableFields.add(FieldName.Attribute.FIELD_DESCRIPTION);
        availableFields.add(FieldName.Attribute.FIELD_CHECK_RULE);
        availableFields.add(FieldName.Attribute.FIELD_REQUIRED);
        for (String field : objFields) {
            if (!availableFields.contains(field)) {
                throw new ScmOperationUnsupportedException(
                        "field can't be modified:fieldName=" + field);
            }
        }
        if (updateInfoObj.containsField(FieldName.Attribute.FIELD_DISPLAY_NAME)) {
            checkExistString(updateInfoObj, FieldName.Attribute.FIELD_DISPLAY_NAME);
        }
        if (updateInfoObj.containsField(FieldName.Attribute.FIELD_DESCRIPTION)) {
            checkExistString(updateInfoObj, FieldName.Attribute.FIELD_DESCRIPTION);
        }
        AttributeType attrType = null;
        /*
         * if (updateInfoObj.containsField(FieldName.Attribute.FIELD_TYPE)) {
         * String type = (String) checkExistString(updateInfoObj,
         * FieldName.Attribute.FIELD_TYPE); attrType =
         * AttributeType.getType(type); if (attrType ==
         * AttributeType.UNKOWN_TYPE) { throw new
         * ScmInvalidArgumentException("unsupported type: " + type); } }
         */
        if (updateInfoObj.containsField(FieldName.Attribute.FIELD_CHECK_RULE)) {
            if (attrType == null) {
                MetadataAttr attrInfo = ScmContentServer.getInstance().getMetaService()
                        .getAttrInfo(wsName, attrId);
                attrType = attrInfo.getType();
            }
            parseCheckRule(updateInfoObj, attrType);
        }
        if (updateInfoObj.containsField(FieldName.Attribute.FIELD_REQUIRED)) {
            Object obj = updateInfoObj.get(FieldName.Attribute.FIELD_REQUIRED);
            String str = String.valueOf(obj);
            if (!"true".equalsIgnoreCase(str) && !"false".equalsIgnoreCase(str)) {
                throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR, "field["
                        + FieldName.Attribute.FIELD_REQUIRED + "] is not boolean format: " + str);
            }
        }
    }

    private static Object checkExistString(BSONObject obj, String fieldName)
            throws ScmServerException {
        Object value = obj.get(fieldName);
        if (value == null) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + fieldName + "] is null!");
        }
        if (!(value instanceof String)) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + fieldName + "] is not String format!");
        }
        return value;
    }

    private static Object getAndCheckStringArg(BSONObject obj, String fieldName)
            throws ScmServerException {
        if (obj.containsField(fieldName)) {
            Object value = obj.get(fieldName);
            if (!(value instanceof String)) {
                throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                        "field[" + fieldName + "] is not String format!");
            }
            return value;
        }
        return "";
    }

    private Object parseCheckRule(BSONObject attrInfo, AttributeType attrType)
            throws ScmServerException {
        Object obj = null;
        if (attrInfo.containsField(FieldName.Attribute.FIELD_CHECK_RULE)) {
            obj = attrInfo.get(FieldName.Attribute.FIELD_CHECK_RULE);
            if (obj instanceof BSONObject) {
                BSONObject rule = (BSONObject) obj;
                AttrManager.getInstance().validCheckRule(attrType, rule);
            }
            else {
                throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                        "check_rule format specified incorrectly: " + obj);
            }
        }
        else {
            obj = new BasicBSONObject();
        }
        return obj;
    }
}
