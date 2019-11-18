package com.sequoiacm.config.framework.workspace.dao;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.workspace.entity.RootDirEntity;
import com.sequoiacm.config.framework.workspace.metasource.WorkspaceMetaSerivce;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Component
public class CreateWorkspaceDao {
    private static final Logger logger = LoggerFactory.getLogger(CreateWorkspaceDao.class);

    @Autowired
    private WorkspaceMetaSerivce workspaceMetaService;

    @Autowired
    private Metasource metaSource;

    @Autowired
    private DefaultVersionDao versionDao;

    public ScmConfOperateResult create(WorkspaceConfig wsConfig) throws ScmConfigException {
        logger.info("start to create workspace:{}", wsConfig.getWsName());
        WorkspaceConfig wsRespConfig = createWorkspace(wsConfig);
        logger.info("create workspace success:{}", wsRespConfig.getWsName());
        ScmConfEvent event = createWorkspaceEvent(wsRespConfig.getWsName());
        return new ScmConfOperateResult(wsRespConfig, event);
    }

    private WorkspaceConfig createWorkspace(WorkspaceConfig wsConfig) throws ScmConfigException {
        Date createTime = new Date();
        wsConfig.setCreateTime(createTime.getTime());
        wsConfig.setUpdateTime(createTime.getTime());
        wsConfig.setUpdateUser(wsConfig.getCreateUser());
        Transaction transaction = null;
        BSONObject wsRecord = null;
        boolean isNeedRollbackMetaTable = false;
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresLock(ScmLockPathFactory.createWorkspaceConfOpLockPath());
        try {
            transaction = metaSource.createTransaction();
            TableDao sysWsTabledao = workspaceMetaService.getSysWorkspaceTable(transaction);
            // generate ws id
            int wsId = sysWsTabledao.generateId();
            wsConfig.setWsId(wsId);
            // insert ws record
            transaction.begin();

            wsRecord = formateToWorkspaceRecord(wsConfig);

            try {
                sysWsTabledao.insert(wsRecord);
            }
            catch (MetasourceException e) {
                if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.WORKSPACE_EXIST,
                            "workspace exist:wsName=" + wsConfig.getWsName(), e);
                }
                throw e;
            }

            lock.unlock();
            lock = null;

            // create ws table
            workspaceMetaService.createWorkspaceMetaTable(wsConfig.getWsName(),
                    wsConfig.getMetalocation());
            isNeedRollbackMetaTable = true;
            TableDao dirDao = workspaceMetaService.getWorkspaceDirTableDao(wsConfig.getWsName(),
                    transaction);
            RootDirEntity rootDir = new RootDirEntity(wsConfig.getCreateUser(),
                    createTime.getTime());
            // insert root dir record
            dirDao.insert(rootDir.toReocord());

            // insert ws version record
            versionDao.createVersion(ScmConfigNameDefine.WORKSPACE, wsConfig.getWsName(),
                    transaction);

            // insert metadata version record
            versionDao.createVersion(ScmConfigNameDefine.META_DATA, wsConfig.getWsName(),
                    transaction);

            transaction.commit();
        }
        catch (ScmConfigException e) {
            if (isNeedRollbackMetaTable) {
                rollbackMetaTable(wsConfig.getWsName());
            }
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
        catch (Exception e) {
            if (isNeedRollbackMetaTable) {
                rollbackMetaTable(wsConfig.getWsName());
            }
            if (transaction != null) {
                transaction.rollback();
            }
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to create workspace:wsName=" + wsConfig.getWsName(), e);
        }
        finally {
            if (transaction != null) {
                transaction.close();
            }
            if (lock != null) {
                lock.unlock();
            }
        }
        return wsConfig;
    }

    private void rollbackMetaTable(String wsName) {
        try {
            workspaceMetaService.deleteWorkspaceMetaTable(wsName);
        }
        catch (Exception e) {
            logger.warn("failed to rollback workspace meta table:wsName={}", wsName, e);
        }
    }

    private ScmConfEvent createWorkspaceEvent(String wsName) throws ScmConfigException {
        WorkspaceNotifyOption notifyOption = new WorkspaceNotifyOption(wsName, 1, EventType.CREATE);
        return new ScmConfEventBase(ScmConfigNameDefine.WORKSPACE, notifyOption);
    }

    private BSONObject formateToWorkspaceRecord(WorkspaceConfig wsConfig) {
        BSONObject wsRecord = new BasicBSONObject();
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_CREATETIME, wsConfig.getCreateTime());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_CREATEUSER, wsConfig.getCreateUser());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, wsConfig.getDataLocations());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_ID, wsConfig.getWsId());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_META_LOCATION, wsConfig.getMetalocation());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_NAME, wsConfig.getWsName());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_UPDATETIME, wsConfig.getUpdateTime());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_UPDATEUSER, wsConfig.getCreateUser());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_DESCRIPTION, wsConfig.getDesc());
        return wsRecord;
    }
}
