package com.sequoiacm.config.framework.workspace.dao;

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
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.workspace.metasource.WorkspaceMetaSerivce;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.metadata.MetaDataNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;

@Component
public class DeleteWorkspaceDao {
    private static final Logger logger = LoggerFactory.getLogger(DeleteWorkspaceDao.class);

    @Autowired
    private WorkspaceMetaSerivce workspaceMetaservice;

    @Autowired
    private Metasource metasource;

    @Autowired
    private DefaultVersionDao versionDao;

    public ScmConfOperateResult delete(WorkspaceFilter filter) throws ScmConfigException {
        ScmConfOperateResult opRes = new ScmConfOperateResult();

        BasicBSONObject sysWsRecMatcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                filter.getWsName());
        Transaction transaction = metasource.createTransaction();
        BSONObject oldWsRec;
        try {
            transaction.begin();
            TableDao sysWsDao = workspaceMetaservice.getSysWorkspaceTable(transaction);
            oldWsRec = sysWsDao.deleteAndCheck(sysWsRecMatcher);
            if (oldWsRec != null) {
                // delete ws version
                versionDao.deleteVersion(ScmConfigNameDefine.WORKSPACE, filter.getWsName(),
                        transaction);
                // delete metadata version
                versionDao.deleteVersion(ScmConfigNameDefine.META_DATA, filter.getWsName(),
                        transaction);
                transaction.commit();
            }
            else {
                transaction.rollback();
                // do noting
                return opRes;
            }
        }
        catch (Exception e) {
            transaction.rollback();
            throw e;
        }
        finally {
            transaction.close();
        }

        modifyDataTableNameHistory(filter);

        try {
            workspaceMetaservice.deleteWorkspaceMetaTable(oldWsRec);
        }
        catch (Exception e) {
            logger.warn("failed to delete workspace meta table:ws={}", filter.getWsName(), e);
        }

        ScmConfEvent wsEvent = createWorkspaceDeleteEvent(filter);
        ScmConfEvent metaDataEvent = createMetaDataDeleteEvent(filter);

        opRes.addEvent(metaDataEvent);
        opRes.addEvent(wsEvent);

        return opRes;
    }

    private ScmConfEvent createMetaDataDeleteEvent(WorkspaceFilter filter) {
        MetaDataNotifyOption notifycation = new MetaDataNotifyOption(filter.getWsName(),
                EventType.DELTE, null);
        return new ScmConfEventBase(ScmConfigNameDefine.META_DATA, notifycation);
    }

    private void modifyDataTableNameHistory(WorkspaceFilter filter) {
        BSONObject matcher = new BasicBSONObject(
                FieldName.FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_NAME, filter.getWsName());
        matcher.put(FieldName.FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_IS_DELTED, false);

        BSONObject modifier = new BasicBSONObject(
                FieldName.FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_IS_DELTED, true);
        try {
            workspaceMetaservice.getDataTableNameHistoryDao().update(matcher, modifier);
        }
        catch (MetasourceException e) {
            logger.warn("failed to mark workspace deleted in DATA_TABLE_NAME_HISTORY:wsName={}",
                    filter.getWsName(), e);
        }
    }

    private ScmConfEvent createWorkspaceDeleteEvent(WorkspaceFilter filter) {
        WorkspaceNotifyOption notifycation = new WorkspaceNotifyOption(filter.getWsName(), null,
                EventType.DELTE);
        return new ScmConfEventBase(ScmConfigNameDefine.WORKSPACE, notifycation);
    }
}
