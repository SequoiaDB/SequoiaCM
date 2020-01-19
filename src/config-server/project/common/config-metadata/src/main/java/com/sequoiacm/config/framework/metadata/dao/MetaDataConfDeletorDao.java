package com.sequoiacm.config.framework.metadata.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.framework.metadata.metasource.MetaDataConfMetaService;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataNotifyOption;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Component
public class MetaDataConfDeletorDao {
    @Autowired
    private MetaDataConfMetaService metaDataService;

    @Autowired
    private Metasource metasource;

    @Autowired
    private DefaultVersionDao versionDao;

    private ScmLockManager lockManger = ScmLockManager.getInstance();

    public ScmConfOperateResult deleteClass(MetaDataClassConfigFilter classFilter)
            throws ScmConfigException {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        Transaction t = null;
        ScmLock lock = lockManger.acquiresLock(
                ScmLockPathFactory.createGlobalMetadataLockPath(classFilter.getWsName()));
        try {
            t = metasource.createTransaction();
            t.begin();
            TableDao classTable = metaDataService.getClassTableDao(classFilter.getWsName(), t);
            TableDao relTable = metaDataService
                    .getAttributeClassRelTableDao(classFilter.getWsName(), t);

            BasicBSONObject classMatcher = new BasicBSONObject(FieldName.ClassTable.FIELD_ID,
                    classFilter.getId());
            BSONObject oldRecord = classTable.deleteAndCheck(classMatcher);
            if (oldRecord == null) {
                throw new ScmConfigException(ScmConfError.CLASS_NOT_EXIST,
                        "class not exist:classId=" + classFilter.getId());
            }

            BasicBSONObject relMatcher = new BasicBSONObject(FieldName.ClassAttrRel.FIELD_CLASS_ID,
                    classFilter.getId());
            relTable.delete(relMatcher);

            Integer version = versionDao.increaseVersion(ScmConfigNameDefine.META_DATA,
                    classFilter.getWsName(), t);
            ScmConfEventBase event = new ScmConfEventBase(ScmConfigNameDefine.META_DATA,
                    new MetaDataNotifyOption(classFilter.getWsName(), EventType.UPDATE, version));
            opRes.addEvent(event);
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

    public ScmConfOperateResult deleteAttribute(MetaDataAttributeConfigFilter attributeFilter)
            throws ScmConfigException {
        Transaction t = null;
        ScmLock lock = lockManger.acquiresLock(
                ScmLockPathFactory.createGlobalMetadataLockPath(attributeFilter.getWsName()));
        try {
            t = metasource.createTransaction();
            t.begin();
            TableDao attributeTable = metaDataService
                    .getAttributeTableDao(attributeFilter.getWsName(), t);
            TableDao relTable = metaDataService
                    .getAttributeClassRelTableDao(attributeFilter.getWsName(), t);

            BasicBSONObject relMatcher = new BasicBSONObject(FieldName.ClassAttrRel.FIELD_ATTR_ID,
                    attributeFilter.getId());
            long relCount = relTable.count(relMatcher);
            if (relCount > 0) {
                throw new ScmConfigException(ScmConfError.ATTRIBUTE_IN_CLASS,
                        "delete attribute failed, attribute are attached with certain classes:attributeId="
                                + attributeFilter.getId());
            }

            BasicBSONObject matcher = new BasicBSONObject(FieldName.ClassTable.FIELD_ID,
                    attributeFilter.getId());
            BSONObject oldRecord = attributeTable.deleteAndCheck(matcher);
            if (oldRecord == null) {
                throw new ScmConfigException(ScmConfError.ATTRIBUTE_NOT_EXIST,
                        "attribute not exist:classId=" + attributeFilter.getId());
            }
            t.commit();
            // delete attribute no need change version
            return new ScmConfOperateResult();
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
}
