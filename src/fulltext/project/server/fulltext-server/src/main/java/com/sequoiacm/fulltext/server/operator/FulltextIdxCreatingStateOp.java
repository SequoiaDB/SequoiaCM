package com.sequoiacm.fulltext.server.operator;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;

@Component
public class FulltextIdxCreatingStateOp extends FulltextIdxCreatedStateOp {
    private static final Logger logger = LoggerFactory.getLogger(FulltextIdxCreatingStateOp.class);

    @Override
    public void createIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject fileMatcher, ScmFulltextMode mode) throws FullTextException {
        String schName = currentWsFulltextExtData.getFulltextJobName();
        
        // 当前状态是 CREATING，如果是正常情况，有调度任务在执行索引建立，
        // 这时对重复建立索引的操作进行报错
        if (schClient.isInternalSchExist(schName)) {
            throw new FullTextException(ScmError.FULL_TEXT_INDEX_IS_CREATING,
                    "index is creating:ws=" + currentWsFulltextExtData.getWsName());
        }

        // 状态是 CREATING，但是没有调度任务，这种情况是创建索引修改状态后没有建立调度任务直接崩溃退出导致的
        // 后面流程修复这种异常情况
        
        String wsName = currentWsFulltextExtData.getWsName();
        String indexLocation = currentWsFulltextExtData.getIndexDataLocation();

        esClient.createIndexIfNotExist(indexLocation);
        createTopicIfNotExist(wsName);

        // 如果这次createIdx请求带来的fileMatcher跟上一次的不一致，走更新索引的流程
        // 因为状态改成 CREATING 后可能有一些文件通过走消息队列建立了索引
        if (!fileMatcher.equals(currentWsFulltextExtData.getFileMatcher())) {
            updateIndex(currentWsFulltextExtData, fileMatcher, mode);
            return;
        }

        try {
            createSchForIndexCreate(wsName, fileMatcher, indexLocation, schName);
        }
        catch (Exception e) {
            rollbackToNoneSilence(wsName);
            dropTopicSilence(wsName);
            dropIndexSilence(indexLocation);
            throw e;
        }
    }

    @Override
    public void dropIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData)
            throws FullTextException {
        String wsName = currentWsFulltextExtData.getWsName();
        String idxLocation = currentWsFulltextExtData.getIndexDataLocation();

        schClient.removeInternalSch(currentWsFulltextExtData.getFulltextJobName(), true);

        changeToDeletingAndCreateSch(wsName, idxLocation);
    }

    @Override
    public void updateIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject newFileMatcher, ScmFulltextMode newMode) throws FullTextException {
        if (newFileMatcher == null && newMode == null) {
            return;
        }

        if (newFileMatcher == null) {
            updateWsFulltextMode(currentWsFulltextExtData, newMode);
            return;
        }

        String schName = currentWsFulltextExtData.getFulltextJobName();
        if (schClient.isInternalSchExist(schName)) {
            if (newFileMatcher.equals(currentWsFulltextExtData.getFileMatcher())) {
                updateWsFulltextMode(currentWsFulltextExtData, newMode);
                return;
            }
            schClient.removeInternalSch(schName, true);
        }

        updateAndBuildIndex(currentWsFulltextExtData, newFileMatcher, newMode);
    }

    @Override
    public ScmFulltextStatus operatorForStatus() {
        return ScmFulltextStatus.CREATING;
    }

    @Override
    public void inspectIndex(ScmWorkspaceFulltextExtData fulltextData) throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_IS_CREATING,
                "fulltext index is creating, can not do inspect now:workspace="
                        + fulltextData.getWsName());
    }

    @Override
    public void rebuildIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData, String fileId)
            throws FullTextException {
        super.rebuildIndex(currentWsFulltextExtData, fileId);
    }

}
