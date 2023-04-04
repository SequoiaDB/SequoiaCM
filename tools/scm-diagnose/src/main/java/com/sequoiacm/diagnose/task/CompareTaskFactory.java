package com.sequoiacm.diagnose.task;

import com.sequoiacm.diagnose.common.CompareInfo;
import com.sequoiacm.diagnose.common.ScmFileInfo;
import com.sequoiacm.diagnose.datasource.ScmDataSourceMgr;
import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;

import java.util.ArrayList;
import java.util.List;

public class CompareTaskFactory implements TaskFactory {
    private CompareInfo compareInfo;
    private DBCursor fileCursor;
    private DBCursor historyCursor;
    private Sequoiadb db;
    private SequoiadbDatasource metaSdbDs;
    private static final int MAX_FILE_COUNT_PER_TASK = 200;

    public CompareTaskFactory(CompareInfo compareInfo)
            throws InterruptedException, ScmToolsException {
        try {
            this.compareInfo = compareInfo;
            metaSdbDs = ScmDataSourceMgr.getInstance().getMetaSdbDs();
            db = metaSdbDs.getConnection();
            BSONObject matcher = CommonUtils.getMatcher(compareInfo.getBeginTime(),
                    compareInfo.getEndTime());
            DBCollection fileCl = db.getCollectionSpace(compareInfo.getWorkspace() + "_META")
                    .getCollection("FILE");
            this.fileCursor = fileCl.query(matcher, null, null, null);
            DBCollection fileHistoryCl = db.getCollectionSpace(compareInfo.getWorkspace() + "_META")
                    .getCollection("FILE_HISTORY");
            this.historyCursor = fileHistoryCl.query(matcher, null, null, null);
        }
        catch (Exception e) {
            releaseSource();
            throw e;
        }
    }

    @Override
    public Runnable createTask(ExecutionContext context) {
        List<ScmFileInfo> fileList = new ArrayList<>(MAX_FILE_COUNT_PER_TASK);

        while (fileCursor.hasNext() && fileList.size() < MAX_FILE_COUNT_PER_TASK) {
            BSONObject file = fileCursor.getNext();
            fileList.add(new ScmFileInfo(file));
        }

        while (fileList.size() < MAX_FILE_COUNT_PER_TASK && historyCursor.hasNext()) {
            BSONObject historyFile = historyCursor.getNext();
            fileList.add(new ScmFileInfo(historyFile));
        }
        if (fileList.isEmpty()) {
            releaseSource();
            return null;
        }
        return new CompareTask(compareInfo.getProgress(), compareInfo.getWorkspace(), fileList,
                compareInfo.getCheckLevel(), compareInfo.isFull(), context);
    }

    private void releaseSource() {
        ScmCommon.closeResource(fileCursor, historyCursor);
        if (null != db) {
            metaSdbDs.releaseConnection(db);
        }
    }
}
