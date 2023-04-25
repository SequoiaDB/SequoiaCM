package com.sequoiacm.contentserver.job;

import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentModule;

public class ScmTaskTransferFile extends ScmTaskFile {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskTransferFile.class);

    public ScmTaskTransferFile(ScmTaskManager mgr, BSONObject info, boolean isAsyncCountFile)
            throws ScmServerException {
        super(mgr, info, isAsyncCountFile);
    }

    @Override
    public int getTaskType() {
        return CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE;
    }

    @Override
    public String getName() {
        return "SCM_TASK_TRANSFER_FILE";
    }

    @Override
    protected void doFile(BSONObject fileInfoNotInLock) throws ScmServerException {
        ScmFileSubTask scmFileSubTask = new ScmFileTransferSubTask(fileInfoNotInLock,
                getWorkspaceInfo(), taskInfoContext, this);
        submitSubTask(scmFileSubTask);
    }

    @Override
    protected void taskComplete() {
        // do nothing
    }

    @Override
    protected BSONObject buildActualMatcher() throws ScmServerException {
        int remoteSiteId = (int) taskInfo.get(FieldName.Task.FIELD_TARGET_SITE);
        try {
            BasicBSONList matcherList = new BasicBSONList();
            BSONObject taskMatcher = getTaskContent();
            BSONObject mySiteFileMatcher = ScmMetaSourceHelper
                    .dollarSiteInList(ScmContentModule.getInstance().getLocalSite());
            BSONObject targetSiteFileMatcher = ScmMetaSourceHelper
                    .dollarSiteNotInList(remoteSiteId);
            matcherList.add(taskMatcher);
            matcherList.add(mySiteFileMatcher);
            matcherList.add(targetSiteFileMatcher);

            BSONObject needProcessMatcher = new BasicBSONObject();
            needProcessMatcher.put(ScmMetaSourceHelper.SEQUOIADB_MATCHER_AND, matcherList);

            return needProcessMatcher;
        }
        catch (Exception e) {
            logger.error("build actual matcher failed", e);
            throw new ScmSystemException("build actual matcher failed", e);
        }
    }
}
