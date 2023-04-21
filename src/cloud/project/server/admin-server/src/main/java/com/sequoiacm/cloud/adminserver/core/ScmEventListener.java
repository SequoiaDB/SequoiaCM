package com.sequoiacm.cloud.adminserver.core;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.dao.QuotaSyncDao;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketDeletedEvent;
import com.sequoiacm.infrastructure.config.client.core.workspace.WorkspaceDeletedEvent;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ScmEventListener {

    private static Logger logger = LoggerFactory.getLogger(ScmEventListener.class);

    @Autowired
    private QuotaSyncDao quotaSyncDao;

    @EventListener
    public void handleBucketDeletedEvent(BucketDeletedEvent bucketDeletedEvent)
            throws ScmMetasourceException {
        logger.info("handle BucketDeletedEvent,bucketName={}",
                bucketDeletedEvent.getDeletedBucketName());
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, StatisticsDefine.QuotaType.BUCKET);
        matcher.put(FieldName.QuotaSync.NAME, bucketDeletedEvent.getDeletedBucketName());
        quotaSyncDao.delete(matcher);
    }

    @EventListener
    public void handleWorkspaceDeletedEvent(WorkspaceDeletedEvent workspaceDeletedEvent)
            throws ScmMetasourceException {
        logger.info("handle WorkspaceDeletedEvent,workspaceName={}",
                workspaceDeletedEvent.getDeletedWorkspace());
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, StatisticsDefine.QuotaType.BUCKET);
        matcher.put(
                FieldName.QuotaSync.EXTRA_INFO + "."
                        + FieldName.QuotaSync.EXTRA_INFO_WORKSPACE,
                workspaceDeletedEvent.getDeletedWorkspace());
        quotaSyncDao.delete(matcher);
    }
}
