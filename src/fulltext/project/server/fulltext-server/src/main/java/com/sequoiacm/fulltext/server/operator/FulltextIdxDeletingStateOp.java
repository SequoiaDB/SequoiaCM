package com.sequoiacm.fulltext.server.operator;

import org.bson.BSONObject;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobData;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobType;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;

@Component
public class FulltextIdxDeletingStateOp extends FulltextIdxOperator {

    @Override
    public void createIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject fileMatcher, ScmFulltextMode mode) throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_IS_DELETING,
                "index is deleting :ws=" + currentWsFulltextExtData.getWsName());
    }

    @Override
    public void dropIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData)
            throws FullTextException {
        // 当前是 Deleting 状态，正常情况下是有 ‘删除任务’ 在跑的，如果没有，我们给它补上
        if (schClient.isInternalSchExist(currentWsFulltextExtData.getFulltextJobName())) {
            return;
        }
        long latestMsgId = getLatestMsgId(currentWsFulltextExtData.getWsName());
        FulltextIdxSchJobData fulltextSch = new FulltextIdxSchJobData();
        fulltextSch.setFileMatcher(null);
        fulltextSch.setLatestMsgId(latestMsgId);
        fulltextSch.setIndexDataLocation(currentWsFulltextExtData.getIndexDataLocation());
        fulltextSch.setWs(currentWsFulltextExtData.getWsName());
        schClient.createFulltextSch(currentWsFulltextExtData.getFulltextJobName(),
                FulltextIdxSchJobType.FULLTEXT_INDEX_DELETE, fulltextSch);
    }

    @Override
    public void updateIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject newFileMatcher, ScmFulltextMode newMode) throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_IS_DELETING,
                "index is deleting :ws=" + currentWsFulltextExtData.getWsName());
    }

    @Override
    public ScmFulltextStatus operatorForStatus() {
        return ScmFulltextStatus.DELETING;
    }

    @Override
    public void inspectIndex(ScmWorkspaceFulltextExtData fulltextData) throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_IS_DELETING,
                "fulltext index is deleting, can not inspect now:workspace="
                        + fulltextData.getWsName());
    }

    @Override
    public void rebuildIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData, String fileId)
            throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_IS_DELETING,
                "fulltext index is deleting, can not rebuild index now:workspace="
                        + currentWsFulltextExtData.getWsName());
    }
}
