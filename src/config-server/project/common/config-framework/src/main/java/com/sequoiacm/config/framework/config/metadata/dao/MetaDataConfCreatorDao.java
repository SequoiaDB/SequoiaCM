package com.sequoiacm.config.framework.config.metadata.dao;

import java.util.Date;

import com.sequoiacm.config.framework.config.metadata.metasource.MetaDataConfMetaService;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataAttributeConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataClassConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataConfig;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataNotifyOption;

@Component
public class MetaDataConfCreatorDao {
    @Autowired
    private MetaDataConfMetaService metaDataService;

    @Autowired
    private Metasource metasource;

    @Autowired
    private DefaultVersionDao versionDao;

    public ScmConfOperateResult createClass(MetaDataClassConfig classConfig)
            throws ScmConfigException {
        Date date = new Date();
        String classId = ScmIdGenerator.ClassId.get(date);
        classConfig.setCreateTime(date.getTime());
        classConfig.setUpdateTime(date.getTime());
        classConfig.setUpdateUser(classConfig.getCreateUser());
        classConfig.setId(classId);

        BSONObject record = classConfig.toRecord();
        Transaction t = metasource.createTransaction();
        try {
            t.begin();
            TableDao classTable = metaDataService.getClassTableDao(classConfig.getWsName(), t);
            try {
                classTable.insert(record);
            }
            catch (MetasourceException e) {
                if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.CLASS_EXIST,
                            "class already exist:className=" + classConfig.getName(), e);
                }
                throw e;
            }
            Integer version = versionDao.increaseVersion(ScmBusinessTypeDefine.META_DATA,
                    classConfig.getWsName(), t);
            ScmConfEvent event = new ScmConfEvent(ScmBusinessTypeDefine.META_DATA, EventType.CREATE,
                    new MetaDataNotifyOption(classConfig.getWsName(), version));
            t.commit();
            return new ScmConfOperateResult(new MetaDataConfig(classConfig), event);
        }

        catch (Exception e) {
            t.rollback();
            throw e;
        }
        finally {
            t.close();
        }
    }

    public ScmConfOperateResult createAttribute(MetaDataAttributeConfig attributeConfig)
            throws ScmConfigException {
        Date date = new Date();
        String attributeId = ScmIdGenerator.AttrId.get(date);
        attributeConfig.setId(attributeId);
        attributeConfig.setCreateTime(date.getTime());
        attributeConfig.setUpdateTime(date.getTime());
        attributeConfig.setUpdateUser(attributeConfig.getCreateUser());

        BSONObject record = attributeConfig.toRecord();
        Transaction t = metasource.createTransaction();
        try {
            t.begin();
            TableDao table = metaDataService.getAttributeTableDao(attributeConfig.getWsName(), t);
            try {
                table.insert(record);
            }
            catch (MetasourceException e) {
                if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.ATTRIBUTE_EXIST,
                            "attr already exist:attrName=" + attributeConfig.getName(), e);
                }
                throw e;
            }
            t.commit();
            // create attribute no need change version
            return new ScmConfOperateResult(new MetaDataConfig(attributeConfig), null);
        }
        catch (Exception e) {
            t.rollback();
            throw e;
        }
        finally {
            t.close();
        }
    }
}
