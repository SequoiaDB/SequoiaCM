package com.sequoiacm.config.framework.config.metadata.dao;

import java.util.Date;

import com.sequoiacm.config.framework.config.metadata.metasource.MetaDataConfMetaService;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataNotifyOption;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Component
public class MetaDataConfUpdatorDao {
    @Autowired
    private MetaDataConfMetaService metaDataService;

    @Autowired
    private Metasource metasource;

    @Autowired
    private DefaultVersionDao versionDao;

    private ScmLockManager lockManger = ScmLockManager.getInstance();

    public ScmConfOperateResult updateAttribute(MetaDataAttributeConfigUpdator updator)
            throws ScmConfigException {
        if (!updator.isNeedUpdate()) {
            return new ScmConfOperateResult();
        }
        Transaction t = null;
        ScmLock lock = lockManger
                .acquiresLock(ScmLockPathFactory.createGlobalMetadataLockPath(updator.getWsName()));
        try {
            t = metasource.createTransaction();
            t.begin();
            TableDao attributeTable = metaDataService.getAttributeTableDao(updator.getWsName(), t);
            BasicBSONObject matcher = new BasicBSONObject(FieldName.Attribute.FIELD_ID,
                    updator.getAttributeId());
            BasicBSONObject updatorObj = new BasicBSONObject();
            updatorObj.put(FieldName.Attribute.FIELD_INNER_UPDATE_TIME, new Date().getTime());
            updatorObj.put(FieldName.Attribute.FIELD_INNER_UPDATE_USER, updator.getUpdateUser());
            boolean isNeedNotify = false;
            if (updator.getIsRequire() != null) {
                updatorObj.put(FieldName.Attribute.FIELD_REQUIRED, updator.getIsRequire());
                isNeedNotify = true;
            }
            if (updator.getCheckRule() != null) {
                updatorObj.put(FieldName.Attribute.FIELD_CHECK_RULE, updator.getCheckRule());
                isNeedNotify = true;
            }
            if (updator.getDescription() != null) {
                updatorObj.put(FieldName.Attribute.FIELD_DESCRIPTION, updator.getDescription());
            }
            if (updator.getDisplayName() != null) {
                updatorObj.put(FieldName.Attribute.FIELD_DISPLAY_NAME, updator.getDisplayName());
            }
            BSONObject updatedAttributeRecord = null;
            try {
                updatedAttributeRecord = attributeTable.updateAndCheck(matcher, updatorObj);
            }
            catch (MetasourceException e) {
                if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.ATTRIBUTE_EXIST,
                            "failed to update, attribute exist:attributeId="
                                    + updator.getAttributeId(),
                            e);
                }
                throw e;
            }
            if (updatedAttributeRecord == null) {
                throw new ScmConfigException(ScmConfError.ATTRIBUTE_NOT_EXIST,
                        "attribute not exist:attributeId=" + updator.getAttributeId());
            }

            MetaDataAttributeConfig updatedAttribute = new MetaDataAttributeConfig(
                    updatedAttributeRecord);
            ScmConfOperateResult opRes = new ScmConfOperateResult();
            opRes.setConfig(new MetaDataConfig(updatedAttribute));

            if (isNeedNotify) {
                Integer version = versionDao.increaseVersion(ScmBusinessTypeDefine.META_DATA,
                        updator.getWsName(), t);
                ScmConfEvent event = new ScmConfEvent(ScmBusinessTypeDefine.META_DATA, EventType.UPDATE,
                        new MetaDataNotifyOption(updator.getWsName(), version));
                opRes.addEvent(event);
            }

            t.commit();

            return opRes;
        }
        catch (Exception e) {
            if (t != null) {
                t.rollback();
            }
            throw e;
        }
        finally {
            if (t != null) {
                t.close();
            }
            lock.unlock();
        }
    }

    public ScmConfOperateResult updateClass(MetaDataClassConfigUpdator updator)
            throws ScmConfigException {
        if (!updator.isNeedUpdate()) {
            return new ScmConfOperateResult();
        }
        Transaction t = null;
        ScmLock lock = lockManger
                .acquiresLock(ScmLockPathFactory.createGlobalMetadataLockPath(updator.getWsName()));
        try {
            t = metasource.createTransaction();
            t.begin();
            TableDao relTable = metaDataService.getAttributeClassRelTableDao(updator.getWsName(),
                    t);
            TableDao classTable = metaDataService.getClassTableDao(updator.getWsName(), t);
            TableDao attributeTable = metaDataService.getAttributeTableDao(updator.getWsName(), t);
            boolean isNeedNotify = false;
            if (updator.getAttachAttributeId() != null) {
                attachAttribute(updator, relTable, classTable, attributeTable);
                isNeedNotify = true;
            }

            if (updator.getDettachAttributeId() != null) {
                detachAttribute(updator, relTable);
                isNeedNotify = true;
            }

            BSONObject updatedClassRecord = updateClassRecord(updator, classTable);
            MetaDataClassConfig updatedClassConfig = new MetaDataClassConfig(updatedClassRecord);
            ScmConfOperateResult opRes = new ScmConfOperateResult();
            opRes.setConfig(new MetaDataConfig(updatedClassConfig));

            if (isNeedNotify) {
                Integer version = versionDao.increaseVersion(ScmBusinessTypeDefine.META_DATA,
                        updator.getWsName(), t);
                ScmConfEvent event = new ScmConfEvent(ScmBusinessTypeDefine.META_DATA, EventType.UPDATE,
                        new MetaDataNotifyOption(updator.getWsName(), version));
                opRes.addEvent(event);
            }
            t.commit();
            return opRes;
        }
        catch (Exception e) {
            if (t != null) {
                t.rollback();
            }
            throw e;
        }
        finally {
            if (t != null) {
                t.close();
            }
            lock.unlock();
        }
    }

    private BSONObject updateClassRecord(MetaDataClassConfigUpdator updator, TableDao classTable)
            throws MetasourceException, ScmConfigException {
        BasicBSONObject classUpdator = new BasicBSONObject();
        classUpdator.put(FieldName.Class.FIELD_INNER_UPDATE_USER, updator.getUpdateUser());
        classUpdator.put(FieldName.Class.FIELD_INNER_UPDATE_TIME, new Date().getTime());
        if (updator.getDescription() != null) {
            classUpdator.put(FieldName.Class.FIELD_DESCRIPTION, updator.getDescription());
        }

        if (updator.getName() != null) {
            classUpdator.put(FieldName.Class.FIELD_NAME, updator.getName());
        }

        BasicBSONObject classMather = new BasicBSONObject(FieldName.Class.FIELD_ID,
                updator.getClassId());
        BSONObject classRecord = null;
        try {
            classRecord = classTable.updateAndCheck(classMather, classUpdator);
        }
        catch (MetasourceException e) {
            if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                throw new ScmConfigException(ScmConfError.CLASS_EXIST,
                        "failed to update, class exist:classId=" + updator.getClassId()
                                + ", updatorName=" + updator.getName(),
                        e);
            }
            throw e;
        }

        if (classRecord == null) {
            throw new ScmConfigException(ScmConfError.CLASS_NOT_EXIST,
                    "class not exist:classId=" + updator.getClassId());
        }
        return classRecord;
    }

    private void detachAttribute(MetaDataClassConfigUpdator updator, TableDao relTable)
            throws ScmConfigException {
        BasicBSONObject relDeletor = new BasicBSONObject();
        relDeletor.put(FieldName.ClassAttrRel.FIELD_ATTR_ID, updator.getDettachAttributeId());
        relDeletor.put(FieldName.ClassAttrRel.FIELD_CLASS_ID, updator.getClassId());
        BSONObject oldRecord = relTable.deleteAndCheck(relDeletor);
        if (oldRecord == null) {
            throw new ScmConfigException(ScmConfError.ATTRIBUTE_NOT_IN_CLASS,
                    "class is not attached with this attribute:classId=" + updator.getClassId()
                            + ",attirbuteId=" + updator.getDettachAttributeId());
        }
    }

    private void attachAttribute(MetaDataClassConfigUpdator updator, TableDao relTable,
            TableDao classTable, TableDao attributeTable)
            throws MetasourceException, ScmConfigException {
        BasicBSONObject classMatcher = new BasicBSONObject(FieldName.Class.FIELD_ID,
                updator.getClassId());
        BSONObject classRecord = classTable.queryOne(classMatcher, null, null);
        if (classRecord == null) {
            throw new ScmConfigException(ScmConfError.CLASS_NOT_EXIST,
                    "class not exist:classId=" + updator.getClassId());
        }

        BasicBSONObject attributeMatcher = new BasicBSONObject(FieldName.Attribute.FIELD_ID,
                updator.getAttachAttributeId());
        BSONObject attributeRecord = attributeTable.queryOne(attributeMatcher, null, null);
        if (attributeRecord == null) {
            throw new ScmConfigException(ScmConfError.ATTRIBUTE_NOT_EXIST,
                    "attribute not exist:attributeId=" + updator.getAttachAttributeId());
        }
        BasicBSONObject relRecord = new BasicBSONObject();
        relRecord.put(FieldName.ClassAttrRel.FIELD_CLASS_ID, updator.getClassId());
        relRecord.put(FieldName.ClassAttrRel.FIELD_ATTR_ID, updator.getAttachAttributeId());

        try {
            relTable.insert(relRecord);
        }
        catch (MetasourceException e) {
            if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                throw new ScmConfigException(ScmConfError.ATTRIBUTE_ALREADY_IN_CLASS,
                        "class is already attached with this attr:classId=" + updator.getClassId()
                                + ", attributeId=" + updator.getAttachAttributeId(),
                        e);
            }
            throw e;
        }
    }
}
