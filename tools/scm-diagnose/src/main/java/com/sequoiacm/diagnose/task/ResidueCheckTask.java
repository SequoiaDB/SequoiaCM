package com.sequoiacm.diagnose.task;

import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.diagnose.datasource.ScmDataSourceMgr;
import com.sequoiacm.diagnose.progress.ResidueProgress;
import com.sequoiacm.diagnose.utils.FileOperateUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ResidueCheckTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ResidueCheckTask.class);
    private String wsName;
    private ResidueProgress progress;
    private List<String> dataIdList;
    private ExecutionContext context;

    public ResidueCheckTask(String wsName, ResidueProgress progress, List<String> dataIdList,
            ExecutionContext context) {
        this.wsName = wsName;
        this.progress = progress;
        this.dataIdList = dataIdList;
        this.context = context;
    }

    @Override
    public void run() {
        List<String> tmp = new ArrayList<>(this.dataIdList);
        List<String> residueList = null;
        int total = tmp.size();
        int residueCount = 0;
        SequoiadbDatasource metaSdbDs = ScmDataSourceMgr.getInstance().getMetaSdbDs();
        Sequoiadb db = null;
        DBCursor fileCursor = null;
        DBCursor historyCursor = null;
        boolean hasFailed = false;
        try {
            db = metaSdbDs.getConnection();
            DBCollection cl = db.getCollectionSpace(wsName + "_META").getCollection("FILE");
            fileCursor = cl.query(getMatcher(tmp), null, null, null);
            while (fileCursor.hasNext()) {
                BSONObject fileObj = fileCursor.getNext();
                String fileId = BsonUtils.getString(fileObj, "id");
                // 在file表中查询得到的，就剔除掉，剩余的就是在 file 表中找不到元数据的
                tmp.remove(fileId);
            }
            // 如果有剩余，将剩余的去历史表中查询
            if (tmp.size() > 0) {
                cl = db.getCollectionSpace(wsName + "_META").getCollection("FILE_HISTORY");
                historyCursor = cl.query(getMatcher(tmp), null, null, null);
                while (historyCursor.hasNext()) {
                    BSONObject historyFile = historyCursor.getNext();
                    String fileId = BsonUtils.getString(historyFile, "id");
                    // 在history_file表中查询得到的，就剔除掉，剩余的就是在 history_file 表中找不到元数据的,就是残留的
                    tmp.remove(fileId);
                }
                residueList = new ArrayList<>(tmp);
                residueCount = residueList.size();
            }
        }
        catch (Exception e) {
            progress.failed(total);
            hasFailed = true;
            logger.error("Failed to residue check,error data id list please see residue result", e);
            try {
                FileOperateUtils.appendErrorIdList(this.dataIdList);
            }
            catch (Exception ex) {
                logger.error("Failed to write residue error id list to file", ex);
                context.setHasException(ex);
            }
        }
        finally {
            ScmCommon.closeResource(historyCursor, fileCursor);
            if (null != db) {
                metaSdbDs.releaseConnection(db);
            }
        }
        if (!hasFailed) {
            // 成功
            progress.success(false, total - residueCount);
            progress.success(true, residueCount);
            if (null != residueList && residueCount > 0) {
                // 将残留的id写到文件里
                try {
                    FileOperateUtils.appendResidueIdList(residueList);
                }
                catch (Exception e) {
                    logger.error("Failed to write residue result to file", e);
                    context.setHasException(e);
                }
            }
        }
        context.taskCompleted();
    }

    private BSONObject getMatcher(List<String> idList) {
        // ["id","id"]
        BasicBSONList idBsonList = getIdBsonList(idList);
        // {"$in": ["id","id"]}
        BSONObject in = new BasicBSONObject("$in", idBsonList);
        // {"data_id": {"$in": ["id","id"]}}
        return new BasicBSONObject("data_id", in);
    }

    private BasicBSONList getIdBsonList(List<String> idList) {
        BasicBSONList list = new BasicBSONList();
        list.addAll(idList);
        return list;
    }
}