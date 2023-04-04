package com.sequoiacm.diagnose.task;

import com.sequoiacm.diagnose.common.ResidueCheckInfo;
import com.sequoiacm.diagnose.datasource.ScmDataSourceMgr;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ResidueCheckTaskFactory implements TaskFactory {
    private ResidueCheckInfo residueCheckInfo;
    private boolean fromDataTable = false;
    private DBCursor dataTableDBCursor;
    private ScmFileResource fileResource;
    private SequoiadbDatasource sdbDatasource;
    private Sequoiadb db;
    private static final int MAX_ID_COUNT_PER_TASK = 200;

    public ResidueCheckTaskFactory(ResidueCheckInfo residueCheckInfo)
            throws ScmToolsException, InterruptedException {
        this.residueCheckInfo = residueCheckInfo;
        try {
            if (residueCheckInfo.getDataTable() != null) {
                String dataTable = residueCheckInfo.getDataTable();
                String[] split = dataTable.split("\\.");
                fromDataTable = true;
                sdbDatasource = ScmDataSourceMgr.getInstance()
                        .getSdbDatasource(residueCheckInfo.getSite());
                db = sdbDatasource.getConnection();
                DBCollection cl = db.getCollectionSpace(split[0]).getCollection(split[1]);
                dataTableDBCursor = cl.listLobs();
            }
            else {
                String dataIdFilePath = residueCheckInfo.getDataIdFilePath();
                File dataIdListFile = new File(dataIdFilePath);
                if (dataIdListFile.exists() && dataIdListFile.isFile()) {
                    fileResource = ScmResourceFactory.getInstance()
                            .createFileResource(dataIdListFile);
                }
                else {
                    throw new ScmToolsException(
                            "Can not read content from file,filePath:" + dataIdFilePath,
                            ScmExitCode.SYSTEM_ERROR);
                }
            }
        }
        catch (Exception e) {
            releaseSource();
            throw e;
        }
    }

    @Override
    public Runnable createTask(ExecutionContext context) throws ScmToolsException {
        List<String> ids;
        if (fromDataTable) {
            ids = getIdsFromDBCursor();
        }
        else {
            ids = getIdsFromFile();
        }
        if (ids.isEmpty()) {
            releaseSource();
            return null;
        }
        return new ResidueCheckTask(residueCheckInfo.getWorkspace(), residueCheckInfo.getProgress(),
                ids, context);
    }

    private List<String> getIdsFromDBCursor() {
        List<String> ids = new ArrayList<>(MAX_ID_COUNT_PER_TASK);
        while (dataTableDBCursor.hasNext() && ids.size() < MAX_ID_COUNT_PER_TASK) {
            BSONObject bsonObject = dataTableDBCursor.getNext();
            String oid = bsonObject.get("Oid").toString();
            ids.add(oid);
        }
        return ids;
    }

    private List<String> getIdsFromFile() throws ScmToolsException {
        List<String> ids = new ArrayList<>(MAX_ID_COUNT_PER_TASK);
        String key;
        while (ids.size() < MAX_ID_COUNT_PER_TASK && (key = fileResource.readLine()) != null) {
            if (key.trim().isEmpty()) {
                continue;
            }
            ids.add(key);
        }
        return ids;
    }

    private void releaseSource() {
        ScmCommon.closeResource(dataTableDBCursor);
        if (null != fileResource) {
            fileResource.release();
        }
        if (null != db) {
            sdbDatasource.releaseConnection(db);
        }
    }
}
