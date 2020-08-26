package com.sequoiacm.fulltext.server.operator;

import java.util.UUID;

import org.bson.BSONObject;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobType;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;

@Component
public class FulltextIdxNoneStateOp extends FulltextIdxOperator {

    @Override
    public void createIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject fileMatcher, ScmFulltextMode mode) throws FullTextException {
        String uuid = UUID.randomUUID().toString();
        String wsName = currentWsFulltextExtData.getWsName();
        String indexLocation = FulltextCommonDefine.FULLTEXT_INDEX_PREFIX
                + currentWsFulltextExtData.getWsId() + "-" + uuid;

        esClient.createIndexWithOverwrite(indexLocation);

        try {
            createTopicIfNotExist(wsName);
        }
        catch (Exception e) {
            dropIndexSilence(indexLocation);
            throw e;
        }

        String schName = FulltextCommonDefine.FULLTEXT_SCHEDULE_PREFIX + wsName + "-"
                + FulltextIdxSchJobType.FULLTEXT_INDEX_CREATE + "-" + uuid;

        try {
            changeToCreateing(wsName, fileMatcher, mode, indexLocation, schName);
        }
        catch (Exception e) {
            dropTopicSilence(wsName);
            dropIndexSilence(indexLocation);
            throw e;
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
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_DISABLE,
                "full text index not created, no need drop index:"
                        + currentWsFulltextExtData.getWsName());
    }

    @Override
    public void updateIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject newFileMatcher, ScmFulltextMode newMode) throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_DISABLE,
                "full text index not created, can not update index:"
                        + currentWsFulltextExtData.getWsName());
    }

    @Override
    public ScmFulltextStatus operatorForStatus() {
        return ScmFulltextStatus.NONE;
    }

    @Override
    public void inspectIndex(ScmWorkspaceFulltextExtData fulltextData) throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_DISABLE,
                "full text index not created, can not inspect index:" + fulltextData.getWsName());
    }

    @Override
    public void rebuildIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData, String fileId)
            throws FullTextException {
        throw new FullTextException(ScmError.FULL_TEXT_INDEX_DISABLE,
                "full text index not created, can not rebuild index:"
                        + currentWsFulltextExtData.getWsName());
    }

}
