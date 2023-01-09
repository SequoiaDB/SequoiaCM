package com.sequoiacm.fulltext.server.service;

import com.sequoiacm.fulltext.es.client.base.EsClient;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.operator.FulltextIdxOperator;
import com.sequoiacm.fulltext.server.operator.FulltextIdxOperatorMgr;
import com.sequoiacm.fulltext.server.sch.SchJobStatus;
import com.sequoiacm.fulltext.server.sch.ScheduleServiceClient;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.fulltext.server.workspace.ScmWorkspaceMgr;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextJobInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Service
public class FulltextServiceImpl implements FulltextService {

    @Autowired
    private LockManager lockMgr;
    @Autowired
    private LockPathFactory lockPathFactory;

    @Autowired
    private FulltextIdxOperatorMgr operatorMgr;

    @Autowired
    private ConfServiceClient confClient;
    @Autowired
    private ScmSiteInfoMgr siteMgr;
    @Autowired
    private ScmWorkspaceMgr wsInfoMgr;

    @Autowired
    private EsClient esClient;

    @Autowired
    private ScheduleServiceClient schClient;
    @Autowired
    private ContentserverClientMgr csMgr;

    @Override
    public void createIndex(String ws, BSONObject fileMatcher, ScmFulltextMode mode)
            throws FullTextException {
        ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(ws));
        try {
            ScmWorkspaceFulltextExtData fulltextData = confClient.getWsExternalData(ws);
            FulltextIdxOperator operator = operatorMgr.getOperator(fulltextData.getIndexStatus());
            operator.createIndex(fulltextData, fileMatcher, mode);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void dropIndex(String ws) throws FullTextException {
        ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(ws));
        try {
            ScmWorkspaceFulltextExtData fulltextData = confClient.getWsExternalData(ws);
            FulltextIdxOperator operator = operatorMgr.getOperator(fulltextData.getIndexStatus());
            operator.dropIndex(fulltextData);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void updateIndex(String ws, BSONObject newFileMatcher, ScmFulltextMode newMode)
            throws FullTextException {
        ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(ws));
        try {
            ScmWorkspaceFulltextExtData fulltextData = confClient.getWsExternalData(ws);
            FulltextIdxOperator operator = operatorMgr.getOperator(fulltextData.getIndexStatus());
            operator.updateIndex(fulltextData, newFileMatcher, newMode);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public FulltextSearchCursor search(String ws, int scope, BSONObject contentCondition,
            BSONObject fileCondition) throws FullTextException {
        ScmWorkspaceFulltextExtData wsExternalData = wsInfoMgr.getWorkspaceExtData(ws);
        if (wsExternalData == null) {
            throw new FullTextException(ScmError.WORKSPACE_NOT_EXIST, "workspace not exist:" + ws);
        }
        if (!wsExternalData.isEnabled()) {
            throw new FullTextException(ScmError.FULL_TEXT_INDEX_DISABLE,
                    "workspace fulltext index is :" + wsExternalData.getIndexStatus()
                            + ", workspace:" + ws);
        }

        ContentserverClient csClient = csMgr.getClient(siteMgr.getRootSiteName());
        return new FulltextSearchCursor(wsExternalData, scope, contentCondition, fileCondition,
                csClient, esClient);

    }

    @Override
    public ScmFulltexInfo getIndexInfo(String ws) throws FullTextException {
        ScmWorkspaceFulltextExtData wsExternalData = wsInfoMgr.getWorkspaceExtData(ws);
        if (wsExternalData == null) {
            throw new FullTextException(ScmError.WORKSPACE_NOT_EXIST, "workspace not exist:" + ws);
        }
        ScmFulltexInfo ret = new ScmFulltexInfo();
        ret.setStatus(wsExternalData.getIndexStatus());
        ret.setFileMatcher(wsExternalData.getFileMatcher());
        ret.setMode(wsExternalData.getMode());
        ret.setFulltextLocation(wsExternalData.getIndexDataLocation());

        if (wsExternalData.getFulltextJobName() != null) {
            SchJobStatus status = schClient
                    .getInternalSchLatestStatus(wsExternalData.getFulltextJobName());
            if (status != null) {
                ScmFulltextJobInfo jobStatus = new ScmFulltextJobInfo();
                jobStatus.setEstimateFileCount(status.getEstimateCount());
                double processedCount = status.getErrorCount() + status.getSuccessCount();
                int progress = (int) (status.getEstimateCount() == 0 ? 100
                        : (processedCount / status.getEstimateCount() * 100));
                progress = progress > 100 ? 99 : progress;
                jobStatus.setProgress(progress);
                jobStatus.setSpeed(status.getSpeed());
                jobStatus.setErrorCount(status.getErrorCount());
                jobStatus.setSuccessCount(status.getSuccessCount());
                ret.setJobInfo(jobStatus);
            }
        }

        return ret;
    }

    @Override
    public void inspect(String ws) throws FullTextException {
        ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(ws));
        try {
            ScmWorkspaceFulltextExtData fulltextData = confClient.getWsExternalData(ws);
            FulltextIdxOperator operator = operatorMgr.getOperator(fulltextData.getIndexStatus());
            operator.inspectIndex(fulltextData);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void rebuildIndex(String ws, String fileId) throws FullTextException {
        ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(ws));
        try {
            ScmWorkspaceFulltextExtData fulltextData = confClient.getWsExternalData(ws);
            FulltextIdxOperator operator = operatorMgr.getOperator(fulltextData.getIndexStatus());
            operator.rebuildIndex(fulltextData, fileId);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public ScmFileFulltextInfoCursor getFileIndexInfo(String ws, ScmFileFulltextStatus status)
            throws FullTextException {
        ContentserverClient client = csMgr.getClient(siteMgr.getRootSiteName());
        ScmWorkspaceFulltextExtData wsExtData = wsInfoMgr.getWorkspaceExtData(ws);
        if (wsExtData == null) {
            throw new FullTextException(ScmError.WORKSPACE_NOT_EXIST, "workspace not exist:" + ws);
        }
        return new ScmFileFulltextInfoCursor(client, wsExtData, status);
    }

    @Override
    public long countFileWithIdxStatus(String ws, ScmFileFulltextStatus status)
            throws FullTextException {
        ContentserverClient client = csMgr.getClient(siteMgr.getRootSiteName());
        ScmWorkspaceFulltextExtData wsExtData = wsInfoMgr.getWorkspaceExtData(ws);
        if (wsExtData == null) {
            throw new FullTextException(ScmError.WORKSPACE_NOT_EXIST, "workspace not exist:" + ws);
        }
        ScmFileFulltextInfoCounter counter = new ScmFileFulltextInfoCounter(client, wsExtData,
                status);
        try {
            return counter.count();
        }
        catch (ScmServerException e) {
            throw new FullTextException(e.getError(),
                    "failed to count file in contentserver:ws=" + ws + ", fileIdxStatus=" + status,
                    e);
        }
    }
}
