package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmTaskMoveFile extends ScmTaskFileSpaceRecyclable {

    private static final Logger logger = LoggerFactory.getLogger(ScmTaskMoveFile.class);

    public ScmTaskMoveFile(ScmTaskManager mgr, BSONObject info, boolean isAsyncCountFile)
            throws ScmServerException {
        super(mgr, info, isAsyncCountFile);
    }

    @Override
    public String getName() {
        return "SCM_TASK_MOVE_FILE";
    }

    @Override
    public int getTaskType() {
        return CommonDefine.TaskType.SCM_TASK_MOVE_FILE;
    }

    @Override
    protected void doFile(BSONObject fileInfo) throws ScmServerException {
        super.doFile(fileInfo);
        ScmFileSubTask scmFileSubTask = new ScmFileMoveSubTask(fileInfo, getWorkspaceInfo(),
                taskInfoContext);
        submitSubTask(scmFileSubTask);
    }

    @Override
    protected BSONObject buildActualMatcher() throws ScmServerException {
        try {
            BasicBSONList matcherList = new BasicBSONList();
            BSONObject taskMatcher = getTaskContent();
            BSONObject mySiteFileMatcher = ScmMetaSourceHelper
                    .dollarSiteInList(ScmContentModule.getInstance().getLocalSite());
            matcherList.add(taskMatcher);
            matcherList.add(mySiteFileMatcher);

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
