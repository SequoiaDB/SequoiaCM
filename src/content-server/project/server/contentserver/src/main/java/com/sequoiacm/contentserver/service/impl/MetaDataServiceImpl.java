package com.sequoiacm.contentserver.service.impl;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataClass;
import com.sequoiacm.contentserver.service.IMetaDataService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfigUpdator;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Service
public class MetaDataServiceImpl implements IMetaDataService {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataServiceImpl.class);

    /*
     * @Override public void listClass(PrintWriter writer, String wsName,
     * BSONObject filter) throws ScmServerException { ScmContentServer
     * contentServer = ScmContentServer.getInstance();
     * contentServer.getWorkspaceInfoChecked(wsName);
     *
     * MetaCursor cursor = null; try { BSONObject selector = new
     * BasicBSONObject(); selector.put(FieldName.Class.FIELD_ID, 1);
     * selector.put(FieldName.Class.FIELD_NAME, 1);
     * selector.put(FieldName.Class.FIELD_DESCRIPTION, 1);
     * selector.put(FieldName.Class.FIELD_INNER_CREATE_USER, 1);
     * selector.put(FieldName.Class.FIELD_INNER_CREATE_TIME, 1);
     *
     * contentServer.getMetaService().getClassInfoList(wsName, filter,
     * selector); ServiceUtils.putCursorToWriter(cursor, writer); } finally {
     * ScmSystemUtils.closeResource(cursor); } }
     */

    @Override
    public List<MetadataClass> listClass(String wsName, BSONObject filter)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(wsName);
        return contentServer.getMetaService().listClassInfo(wsName, filter);
    }

    @Override
    public MetadataClass getClassInfoWithAttr(String wsName, String classId)
            throws ScmServerException {
        BSONObject idMatcher = new BasicBSONObject(FieldName.Class.FIELD_ID, classId);
        return getClassInfoWithAttr(wsName, idMatcher);
    }

    @Override
    public MetadataClass getClassInfoWithAttrByName(String wsName, String className)
            throws ScmServerException {
        BSONObject nameMatcher = new BasicBSONObject(FieldName.Class.FIELD_NAME, className);
        return getClassInfoWithAttr(wsName, nameMatcher);
    }

    private MetadataClass getClassInfoWithAttr(String wsName, BSONObject matcher)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(wsName);
        MetadataClass classObj = getAndCheckClass(wsName, matcher);
        List<MetadataAttr> attrList = contentServer.getMetaService().getAttrListForClass(wsName,
                classObj.getId());
        classObj.setAttrList(attrList);
        return classObj;
    }

    @Override
    public MetadataClass createClass(String user, String workspaceName, BSONObject classInfo)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);

        classInfo.put(FieldName.Class.FIELD_INNER_CREATE_USER, user);
        MetaDataClassConfig classConfig = new MetaDataClassConfig(classInfo);
        classConfig.setWsName(workspaceName);
        MetaDataClassConfig resp = ContenserverConfClient.getInstance().createClass(classConfig);
        return convertConfClass(resp);
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
    public MetadataClass updateClass(String user, String workspaceName, String classId,
            BSONObject updator) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        MetaDataClassConfigUpdator classUpdator = new MetaDataClassConfigUpdator(updator);
        classUpdator.setClassId(classId);
        classUpdator.setWsName(workspaceName);
        classUpdator.setUpdateUser(user);
        MetaDataClassConfig resp = ContenserverConfClient.getInstance().updateClass(classUpdator);
        return convertConfClass(resp);
    }

    @Override
    public void deleteClass(String workspaceName, String classId) throws ScmServerException {
        MetaDataClassConfigFilter filter = new MetaDataClassConfigFilter(workspaceName)
                .appendId(classId);
        deleteClass(workspaceName, filter);
    }

    @Override
    public void deleteClassByName(String workspaceName, String className)
            throws ScmServerException {
        MetaDataClassConfigFilter filter = new MetaDataClassConfigFilter(workspaceName)
                .appendName(className);
        deleteClass(workspaceName, filter);
    }

    private void deleteClass(String workspaceName, MetaDataClassConfigFilter filter)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        ContenserverConfClient.getInstance().deleteClass(filter);
    }

    @Override
    public void attachAttr(String user, String workspaceName, String classId, String attrId)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        MetaDataClassConfigUpdator updator = new MetaDataClassConfigUpdator(workspaceName, classId,
                user);
        updator.setAttachAttributeId(attrId);
        ContenserverConfClient.getInstance().updateClass(updator);
    }

    @Override
    public void detachAttr(String user, String workspaceName, String classId, String attrId)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        MetaDataClassConfigUpdator classUpdator = new MetaDataClassConfigUpdator(workspaceName,
                classId, user);
        classUpdator.setDettachAttributeId(attrId);
        ContenserverConfClient.getInstance().updateClass(classUpdator);
    }

    @Override
    public MetadataAttr createAttr(String user, String workspaceName, BSONObject attrInfo)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        attrInfo.put(FieldName.Attribute.FIELD_INNER_CREATE_USER, user);
        MetaDataAttributeConfig attributeConfig = new MetaDataAttributeConfig(attrInfo);
        attributeConfig.setWsName(workspaceName);
        MetaDataAttributeConfig resp = ContenserverConfClient.getInstance()
                .createAttribute(attributeConfig);
        return convertConfAttribute(resp);
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
    public List<MetadataAttr> listAttr(String wsName, BSONObject filter) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(wsName);
        return contentServer.getMetaService().listAttrInfo(wsName, filter);
    }

    @Override
    public MetadataAttr getAttrInfo(String wsName, String attrId) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(wsName);

        return getAndCheckAttr(wsName, attrId);
    }

    @Override
    public MetadataAttr updateAttr(String user, String workspaceName, String attrId,
            BSONObject updator) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        MetaDataAttributeConfigUpdator attributeUpdator = new MetaDataAttributeConfigUpdator(
                updator);
        attributeUpdator.setAttributeId(attrId);
        attributeUpdator.setUpdateUser(user);
        attributeUpdator.setWsName(workspaceName);
        MetaDataAttributeConfig resp = ContenserverConfClient.getInstance()
                .updateAttribute(attributeUpdator);
        return convertConfAttribute(resp);
    }

    @Override
    public void deleteAttr(String workspaceName, String attrId) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        contentServer.getWorkspaceInfoChecked(workspaceName);
        MetaDataAttributeConfigFilter filter = new MetaDataAttributeConfigFilter(workspaceName,
                attrId);
        ContenserverConfClient.getInstance().deleteAttribute(filter);
    }

    private MetadataClass getAndCheckClass(String wsName, BSONObject matcher)
            throws ScmServerException {
        MetadataClass classObj = ScmContentServer.getInstance().getMetaService()
                .getClassInfo(wsName, matcher);
        if (null == classObj) {
            throw new ScmServerException(ScmError.METADATA_CLASS_NOT_EXIST,
                    "class is unexist: workspace=" + wsName + ", matcher=" + matcher);
        }
        return classObj;
    }

    private MetadataAttr getAndCheckAttr(String wsName, String attrId) throws ScmServerException {
        MetadataAttr attrInfo = ScmContentServer.getInstance().getMetaService().getAttrInfo(wsName,
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
