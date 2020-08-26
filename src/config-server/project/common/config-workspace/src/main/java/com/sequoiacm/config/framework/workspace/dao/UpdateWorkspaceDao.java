package com.sequoiacm.config.framework.workspace.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.workspace.metasource.SysWorkspaceTableDao;
import com.sequoiacm.config.framework.workspace.metasource.WorkspaceMetaSerivce;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverterMgr;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;

@Component
public class UpdateWorkspaceDao {

    @Autowired
    private WorkspaceMetaSerivce workspaceMetaservice;

    @Autowired
    private Metasource metasource;

    @Autowired
    private BsonConverterMgr bsonConverterMgr;

    @Autowired
    private DefaultVersionDao versionDao;

    public ScmConfOperateResult update(WorkspaceUpdator updator) throws ScmConfigException {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        Transaction transaction = metasource.createTransaction();
        try {
            transaction.begin();
            SysWorkspaceTableDao table = workspaceMetaservice.getSysWorkspaceTable(transaction);

            BSONObject matcher = null;
            if (updator.getOldWsRecord() != null) {
                matcher = updator.getOldWsRecord();
            }
            else {
                matcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                        updator.getWsName());
            }

            String newDesc = updator.getNewDesc();
            BSONObject newWsRecord = null;
            if (newDesc != null) {
                BasicBSONObject descUpdator = new BasicBSONObject(
                        FieldName.FIELD_CLWORKSPACE_DESCRIPTION, newDesc);
                newWsRecord = table.updateAndCheck(matcher, descUpdator);
                if (newWsRecord == null) {
                    throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                            "client workspace cache is not latest");
                }
                matcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                        updator.getWsName());
            }

            Integer removeLocationId = updator.getRemoveDataLocationId();
            if (removeLocationId != null) {
                newWsRecord = table.removeDataLocation(matcher, removeLocationId);
                if (newWsRecord == null) {
                    throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                            "client workspace cache is not latest");
                }
                matcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                        updator.getWsName());
            }

            BSONObject addDataLocation = updator.getAddDataLocation();
            if (addDataLocation != null) {
                newWsRecord = table.addDataLocation(matcher, addDataLocation);
                if (newWsRecord == null) {
                    throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                            "client workspace cache is not latest");
                }
                matcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                        updator.getWsName());
            }

            BSONObject externalData = updator.getExternalData();
            if (externalData != null) {
                newWsRecord = table.updateExternalData(matcher, externalData);
                if (newWsRecord == null) {
                    throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                            "client workspace cache is not latest");
                }
            }

            if (newWsRecord == null) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG, "update nothing:" + updator);
            }

            WorkspaceConfig wsConfig = (WorkspaceConfig) bsonConverterMgr
                    .getMsgConverter(ScmConfigNameDefine.WORKSPACE).convertToConfig(newWsRecord);

            Integer oldVersion = versionDao.getVersion(ScmConfigNameDefine.WORKSPACE,
                    wsConfig.getWsName());
            Assert.notNull(oldVersion, "version record is null");
            Integer newVersion = versionDao.updateVersion(ScmConfigNameDefine.WORKSPACE,
                    wsConfig.getWsName(), ++oldVersion, transaction);
            Assert.notNull(oldVersion, "version record is null");

            ScmConfEvent event = createEvent(wsConfig, removeLocationId, newVersion);
            opRes.setConfig(wsConfig);
            opRes.addEvent(event);

            transaction.commit();

            return opRes;
        }
        catch (Exception e) {
            transaction.rollback();
            throw e;
        }
        finally {
            transaction.close();
        }

    }

    private ScmConfEvent createEvent(WorkspaceConfig wsConfig, Integer removeLocationId,
            Integer newVersion) {
        WorkspaceNotifyOption notifycation = new WorkspaceNotifyOption(wsConfig.getWsName(),
                newVersion, EventType.UPDATE);
        return new ScmConfEventBase(ScmConfigNameDefine.WORKSPACE, notifycation);
    }
}
