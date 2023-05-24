package com.sequoiacm.config.framework.workspace.dao;

import java.util.Date;

import com.sequoiacm.config.framework.workspace.checker.DataLocationConfigChecker;
import com.sequoiacm.config.metasource.MetaSourceDefine;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
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

    @Autowired
    private DataLocationConfigChecker dataLocationConfigChecker;

    public ScmConfOperateResult create(WorkspaceConfig wsConfig) throws ScmConfigException {
        dataLocationConfigChecker.check(wsConfig.getDataLocations());
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
        wsConfig.setVersion(1);
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
            logger.info("create workspace:{}, generate ws id:{}", wsConfig.getWsName(), wsId);
            wsConfig.setWsId(wsId);
            // insert ws record
            transaction.begin();

            wsRecord = formateToWorkspaceRecord(wsConfig);

            try {
                logger.info("insert record into workspace table:" + wsRecord);
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

            // 检查各个站点上是否存在同名工作区的数据表，如果存在则报错，这里主要规避以下问题：
            // 1. 创建 ws1，ws1 在各个站点上创建了数据表 ws1_LOB
            // 2. 删除 ws1，config-server 删除工作区元数据后，content-server 会开始异步删除各个站点上的数据表 ws1_LOB
            // 3. 重建 ws1，用户立刻写入文件，数据落入 ws1_LOB，此时数据可能会被流程 2 中的 content-server 给删掉
            // 所以这里创建 ws1 要检查各个站点上是否存在 ws1_LOB，不存在才允许建立 ws1
            TableDao dataTableHistoryDao = workspaceMetaService.getDataTableNameHistoryDao();
            BSONObject matcher = new BasicBSONObject(
                    FieldName.FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_NAME, wsConfig.getWsName());
            long dataTableCount = dataTableHistoryDao.count(matcher);
            if (dataTableCount > 0) {
                throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                        "workspace " + wsConfig.getWsName()
                                + " is deleting, please check the workspace in "
                                + MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM + "."
                                + MetaSourceDefine.SequoiadbTableName.CL_DATA_TABLE_NAME_HISTORY);
            }

            // create ws table
            workspaceMetaService.createWorkspaceMetaTable(wsConfig);
            isNeedRollbackMetaTable = true;

            if (wsConfig.isEnableDirectory()) {
                TableDao dirDao = workspaceMetaService.getWorkspaceDirTableDao(wsConfig.getWsName(),
                        transaction);
                RootDirEntity rootDir = new RootDirEntity(wsConfig.getCreateUser(),
                        createTime.getTime());
                // insert root dir record
                dirDao.insert(rootDir.toReocord());
            }

            // insert metadata version record
            versionDao.createVersion(ScmConfigNameDefine.META_DATA, wsConfig.getWsName(),
                    transaction);

            transaction.commit();
        }
        catch (ScmConfigException e) {
            if (isNeedRollbackMetaTable) {
                rollbackMetaTable(wsRecord);
            }
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
        catch (Exception e) {
            if (isNeedRollbackMetaTable) {
                rollbackMetaTable(wsRecord);
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

    private void rollbackMetaTable(BSONObject wsRecord) {
        try {
            workspaceMetaService.deleteWorkspaceMetaTable(wsRecord, true);
        }
        catch (Exception e) {
            logger.warn("failed to rollback workspace meta table:wsName={}",
                    wsRecord.get(FieldName.FIELD_CLWORKSPACE_NAME), e);
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
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN,
                wsConfig.getBatchIdTimePattern());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX,
                wsConfig.getBatchIdTimeRegex());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE,
                wsConfig.isBatchFileNameUnique());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE,
                wsConfig.getBatchShardingType());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, wsConfig.isEnableDirectory());
        if (wsConfig.getPreferred() != null) {
            wsRecord.put(FieldName.FIELD_CLWORKSPACE_PREFERRED, wsConfig.getPreferred());
        }
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY,
                wsConfig.getSiteCacheStrategy());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_VERSION, wsConfig.getVersion());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_TAG_RETRIEVAL_STATUS,
                wsConfig.getTagRetrievalStatus());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_TAG_LIB_TABLE, wsConfig.getTagLibTableName());
        wsRecord.put(FieldName.FIELD_CLWORKSPACE_TAG_LIB_META_OPTION,
                wsConfig.getTagLibMetaOption());
        return wsRecord;
    }
}
