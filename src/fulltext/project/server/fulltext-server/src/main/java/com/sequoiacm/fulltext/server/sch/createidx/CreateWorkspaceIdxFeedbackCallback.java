package com.sequoiacm.fulltext.server.sch.createidx;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.sch.WorkspaceIdxWorkerBase;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOpFeedback;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.mq.client.core.FeedbackCallback;
import com.sequoiacm.mq.core.exception.MqException;

public class CreateWorkspaceIdxFeedbackCallback extends FeedbackCallback<FileFulltextOpFeedback> {
    private static final Logger logger = LoggerFactory.getLogger(CreateWorkspaceIdxFeedbackCallback.class);
    private final WorkspaceIdxWorkerBase idxWorker;
    private final int fileCount;

    public CreateWorkspaceIdxFeedbackCallback(WorkspaceIdxWorkerBase idxWorker, int fileCount) {
        super(FulltextCommonDefine.FULLTEXT_GROUP_NAME);
        this.idxWorker = idxWorker;
        this.fileCount = fileCount;
    }

    @Override
    public void onFeedback(String topic, String key, long msgId,
            FileFulltextOpFeedback feedbackContent) {
        idxWorker.getTaskContext().incSuccessCount(feedbackContent.getSuccessCount());
        idxWorker.getTaskContext().incErrorCount(feedbackContent.getFailedCount());
        try {
            idxWorker.reportStatus(false);
        }
        catch (Exception e) {
            logger.warn("failed to do feedback:feedback={}", feedbackContent, e);
        }
    }

    @Override
    public void onTimeout(String topic, String key, long msgId) {
        idxWorker.getTaskContext().incErrorCount(fileCount);
        try {
            idxWorker.reportStatus(false);
        }
        catch (Exception e) {
            logger.warn("failed to do feedback", e);
        }
    }

    @Override
    protected FileFulltextOpFeedback convert(BSONObject feedback) throws MqException {
        return new FileFulltextOpFeedback(feedback);
    }
}
