package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataClass;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IMetaDataService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.config.core.msg.metadata.*;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetaDataServiceImpl implements IMetaDataService {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataServiceImpl.class);
    @Autowired
    private ScmAudit audit;
    /*
     * @Override public void listClass(PrintWriter writer, String wsName, BSONObject
     * filter) throws ScmServerException { ScmContentServer contentModule =
     * ScmContentServer.getInstance();
     * contentModule.getWorkspaceInfoChecked(wsName);
     *
     * MetaCursor cursor = null; try { BSONObject selector = new BasicBSONObject();
     * selector.put(FieldName.Class.FIELD_ID, 1);
     * selector.put(FieldName.Class.FIELD_NAME, 1);
     * selector.put(FieldName.Class.FIELD_DESCRIPTION, 1);
     * selector.put(FieldName.Class.FIELD_INNER_CREATE_USER, 1);
     * selector.put(FieldName.Class.FIELD_INNER_CREATE_TIME, 1);
     *
     * contentModule.getMetaService().getClassInfoList(wsName, filter, selector);
     * ServiceUtils.putCursorToWriter(cursor, writer); } finally {
     * ScmSystemUtils.closeResource(cursor); } }
     */

    @Override
    public List<MetadataClass> listClass(ScmUser user, String wsName, BSONObject filter,
            BSONObject orderBy, int skip, int limit) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, wsName, ScmPrivilegeDefine.READ,
                "list class info");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        List<MetadataClass> ret = contentModule.getMetaService().listClassInfo(wsName, filter,
                orderBy, skip, limit);
        String message = "list meta data class info";
        if (filter != null) {
            message += " by filter=" + filter.toString();
        }
        audit.info(ScmAuditType.META_CLASS_DQL, user, wsName, 0, message);
        return ret;
    }

    @Override
    public long countClass(ScmUser user, String wsName, BSONObject condition)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, wsName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "count class");
        String message = "count class";
        if (null != condition) {
            message += " by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.META_CLASS_DQL, user, wsName, 0, message);
        ScmContentModule contentModule = ScmContentModule.getInstance();
        return contentModule.getMetaService().getClassCount(wsName, condition);
    }

    @Override
    public MetadataClass getClassInfoWithAttr(ScmUser user, String wsName, String classId)
            throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, wsName, ScmPrivilegeDefine.READ,
                "get class info with attr");
        BSONObject idMatcher = new BasicBSONObject(FieldName.Class.FIELD_ID, classId);

        MetadataClass ret = getClassInfoWithAttr(wsName, idMatcher);
        audit.info(ScmAuditType.META_CLASS_DQL, user, wsName, 0,
                "get class info with attr by classId=" + classId);
        return ret;
    }

    @Override
    public MetadataClass getClassInfoWithAttrByName(ScmUser user, String wsName, String className)
            throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, wsName, ScmPrivilegeDefine.READ,
                "get class info with attr");
        BSONObject nameMatcher = new BasicBSONObject(FieldName.Class.FIELD_NAME, className);
        MetadataClass ret = getClassInfoWithAttr(wsName, nameMatcher);

        audit.info(ScmAuditType.META_CLASS_DQL, user, wsName, 0,
                "get class info with attr by className=" + className);
        return ret;
    }

    private MetadataClass getClassInfoWithAttr(String wsName, BSONObject matcher)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        MetadataClass classObj = getAndCheckClass(wsName, matcher);
        List<MetadataAttr> attrList = contentModule.getMetaService().getAttrListForClass(wsName,
                classObj.getId());
        classObj.setAttrList(attrList);
        return classObj;
    }

    @Override
    public MetadataClass createClass(ScmUser user, String workspaceName, BSONObject classInfo)
            throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "create class");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        classInfo.put(FieldName.Class.FIELD_INNER_CREATE_USER, user.getUsername());
        MetaDataClassConfig classConfig = new MetaDataClassConfig(classInfo);
        classConfig.setWsName(workspaceName);
        MetaDataClassConfig resp = ContenserverConfClient.getInstance().createClass(classConfig);
        MetadataClass ret = convertConfClass(resp);
        audit.info(ScmAuditType.CREATE_META_CLASS, user, workspaceName, 0,
                "create meta data class=" + classInfo.toString());
        return ret;
    }

    private MetadataClass convertConfClass(MetaDataClassConfig confClass) {
        MetadataClass metaClass = new MetadataClass();
        metaClass.setCreateTime(confClass.getCreateTime());
        metaClass.setCreateUser(confClass.getCreateUser());
        metaClass.setDescription(confClass.getDescription());
        metaClass.setId(confClass.getId());
        metaClass.setName(confClass.getName());
        metaClass.setUpdateTime(confClass.getUpdateTime());
        metaClass.setUpdateUser(confClass.getUpdateUser());
        return metaClass;
    }

    @Override
    public MetadataClass updateClass(ScmUser user, String workspaceName, String classId,
            BSONObject updator) throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "update class");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        MetaDataClassConfigUpdator classUpdator = new MetaDataClassConfigUpdator(updator);
        classUpdator.setClassId(classId);
        classUpdator.setWsName(workspaceName);
        classUpdator.setUpdateUser(user.getUsername());
        MetaDataClassConfig resp = ContenserverConfClient.getInstance().updateClass(classUpdator);
        MetadataClass ret = convertConfClass(resp);
        audit.info(ScmAuditType.UPDATE_META_CLASS, user, workspaceName, 0,
                "update class, classId=" + classId + ", description=" + updator);
        return ret;
    }

    @Override
    public void deleteClass(ScmUser user, String workspaceName, String classId)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "delete class");
        MetaDataClassConfigFilter filter = new MetaDataClassConfigFilter(workspaceName)
                .appendId(classId);
        deleteClass(workspaceName, filter);
        audit.info(ScmAuditType.DELETE_META_CLASS, user, workspaceName, 0,
                "delete class by classId=" + classId);
    }

    @Override
    public void deleteClassByName(ScmUser user, String workspaceName, String className)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "delete class");
        MetaDataClassConfigFilter filter = new MetaDataClassConfigFilter(workspaceName)
                .appendName(className);
        deleteClass(workspaceName, filter);
        audit.info(ScmAuditType.DELETE_META_CLASS, user, workspaceName, 0,
                "delete class by className=" + className);
    }

    private void deleteClass(String workspaceName, MetaDataClassConfigFilter filter)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        ContenserverConfClient.getInstance().deleteClass(filter);
    }

    @Override
    public void attachAttr(ScmUser user, String workspaceName, String classId, String attrId)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "class attach attr");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        MetaDataClassConfigUpdator updator = new MetaDataClassConfigUpdator(workspaceName, classId,
                user.getUsername());
        updator.setAttachAttributeId(attrId);
        ContenserverConfClient.getInstance().updateClass(updator);

        audit.info(ScmAuditType.UPDATE_META_CLASS, user, workspaceName, 0,
                "attach attr, classId=" + classId + ", and attrId=" + attrId);
    }

    @Override
    public void detachAttr(ScmUser user, String workspaceName, String classId, String attrId)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "class detach attr");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        MetaDataClassConfigUpdator classUpdator = new MetaDataClassConfigUpdator(workspaceName,
                classId, user.getUsername());
        classUpdator.setDettachAttributeId(attrId);
        ContenserverConfClient.getInstance().updateClass(classUpdator);
        audit.info(ScmAuditType.UPDATE_META_CLASS, user, workspaceName, 0,
                "detach attr, classId=" + classId + ", and attrId=" + attrId);
    }

    @Override
    public MetadataAttr createAttr(ScmUser user, String workspaceName, BSONObject attrInfo)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "create attr");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        attrInfo.put(FieldName.Attribute.FIELD_INNER_CREATE_USER, user.getUsername());
        MetaDataAttributeConfig attributeConfig = new MetaDataAttributeConfig(attrInfo);
        attributeConfig.setWsName(workspaceName);
        MetaDataAttributeConfig resp = ContenserverConfClient.getInstance()
                .createAttribute(attributeConfig);
        MetadataAttr ret = convertConfAttribute(resp);

        audit.info(ScmAuditType.CREATE_META_ATTR, user, workspaceName, 0,
                "create attr, desc=" + attrInfo);
        return ret;
    }

    private MetadataAttr convertConfAttribute(MetaDataAttributeConfig confAttr) {
        MetadataAttr attr = new MetadataAttr();
        attr.setCheckRule(confAttr.getCheckRule());
        attr.setCreateTime(confAttr.getCreateTime());
        attr.setCreateUser(confAttr.getCreateUser());
        attr.setDescription(confAttr.getDescription());
        attr.setDisplayName(confAttr.getDisplayName());
        attr.setId(confAttr.getId());
        attr.setName(confAttr.getName());
        attr.setRequired(confAttr.isRequired());
        attr.setType(AttributeType.getType(confAttr.getType()));
        attr.setUpdateTime(confAttr.getUpdateTime());
        attr.setUpdateUser(confAttr.getUpdateUser());
        return attr;
    }

    @Override
    public List<MetadataAttr> listAttr(ScmUser user, String wsName, BSONObject filter)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, wsName, ScmPrivilegeDefine.READ,
                "list attr info");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        List<MetadataAttr> ret = contentModule.getMetaService().listAttrInfo(wsName, filter);

        String message = "get class attr info list";
        if (filter != null) {
            message += " by filter=" + filter.toString();
        }
        audit.info(ScmAuditType.META_CLASS_DQL, user, wsName, 0, message);
        return ret;
    }

    @Override
    public MetadataAttr getAttrInfo(ScmUser user, String wsName, String attrId)
            throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, wsName, ScmPrivilegeDefine.READ,
                "get attr info");

        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(wsName);

        MetadataAttr ret = getAndCheckAttr(wsName, attrId);
        audit.info(ScmAuditType.META_CLASS_DQL, user, wsName, 0,
                "get attr info by attrId=" + attrId);
        return ret;

    }

    @Override
    public MetadataAttr updateAttr(ScmUser user, String workspaceName, String attrId,
            BSONObject updator) throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "update attr");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        MetaDataAttributeConfigUpdator attributeUpdator = new MetaDataAttributeConfigUpdator(
                updator);
        attributeUpdator.setAttributeId(attrId);
        attributeUpdator.setUpdateUser(user.getUsername());
        attributeUpdator.setWsName(workspaceName);
        MetaDataAttributeConfig resp = ContenserverConfClient.getInstance()
                .updateAttribute(attributeUpdator);
        MetadataAttr ret = convertConfAttribute(resp);

        audit.info(ScmAuditType.UPDATE_META_ATTR, user, workspaceName, 0,
                "update attr, attrId=" + attrId + ", desc=" + updator);
        return ret;
    }

    @Override
    public void deleteAttr(ScmUser user, String workspaceName, String attrId)
            throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.ALL, "delete attr");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        MetaDataAttributeConfigFilter filter = new MetaDataAttributeConfigFilter(workspaceName,
                attrId);
        ContenserverConfClient.getInstance().deleteAttribute(filter);

        audit.info(ScmAuditType.DELETE_META_ATTR, user, workspaceName, 0,
                "delete attr by attrId=" + attrId);
    }

    private MetadataClass getAndCheckClass(String wsName, BSONObject matcher)
            throws ScmServerException {
        MetadataClass classObj = ScmContentModule.getInstance().getMetaService()
                .getClassInfo(wsName, matcher);
        if (null == classObj) {
            throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST,
                    "class is unexist: workspace=" + wsName + ", matcher=" + matcher);
        }
        return classObj;
    }

    private MetadataAttr getAndCheckAttr(String wsName, String attrId) throws ScmServerException {
        MetadataAttr attrInfo = ScmContentModule.getInstance().getMetaService().getAttrInfo(wsName,
                attrId);
        if (null == attrInfo) {
            throw new ScmServerException(ScmError.METADATA_ATTR_NOT_EXIST,
                    "attribute is unexist: workspace=" + wsName + ", attrId=" + attrId);
        }
        return attrInfo;
    }

    private ScmLock lock(ScmLockPath lockPath) throws Exception {
        return ScmLockManager.getInstance().acquiresLock(lockPath);
    }

    private void unlock(ScmLock lock, ScmLockPath lockPath) {
        try {
            if (lock != null) {
                lock.unlock();
            }
        }
        catch (Exception e) {
            logger.error("failed to unlock:path=" + lockPath);
        }
    }
}
