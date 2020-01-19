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
        BSONObject oldWsRecord = updator.getOldWsRecord();
        Transaction transaction = metasource.createTransaction();
        try {
            transaction.begin();
            SysWorkspaceTableDao table = workspaceMetaservice.getSysWorkspaceTable(transaction);

            String newDesc = updator.getNewDesc();
            if (newDesc != null) {
                BasicBSONObject descUpdator = new BasicBSONObject(
                        FieldName.FIELD_CLWORKSPACE_DESCRIPTION, newDesc);
                oldWsRecord = table.updateAndCheck(oldWsRecord, descUpdator);
                if (oldWsRecord == null) {
                    throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                            "client workspace cache is not latest");
                }
            }

            Integer removeLocationId = updator.getRemoveDataLocationId();
            if (removeLocationId != null) {
                oldWsRecord = table.removeDataLocation(oldWsRecord, removeLocationId);
                if (oldWsRecord == null) {
                    throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                            "client workspace cache is not latest");
                }
            }

            BSONObject addDataLocation = updator.getAddDataLocation();
            if (addDataLocation != null) {
                oldWsRecord = table.addDataLocation(oldWsRecord, addDataLocation);
                if (oldWsRecord == null) {
                    throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                            "client workspace cache is not latest");
                }
            }

            WorkspaceConfig wsConfig = (WorkspaceConfig) bsonConverterMgr
                    .getMsgConverter(ScmConfigNameDefine.WORKSPACE).convertToConfig(oldWsRecord);

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
