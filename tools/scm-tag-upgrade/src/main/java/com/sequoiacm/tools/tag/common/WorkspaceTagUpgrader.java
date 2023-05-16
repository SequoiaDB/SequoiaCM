package com.sequoiacm.tools.tag.common;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkspaceTagUpgrader {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceTagUpgrader.class);
    private final UpgradeTagStatus upgradeStatus;

    private final FileUpgradeExecutor fileUpgradeExecutor;

    public WorkspaceTagUpgrader(List<String> wsNameList, String tagLibDomain, int thread)
            throws ScmToolsException {
        String statusFilePath = "./status/status-" + UUID.randomUUID();
        upgradeStatus = UpgradeTagStatus.newStatus(statusFilePath, wsNameList,
                SequoiadbDataSourceWrapper.getInstance().getSdbUrls(),
                SequoiadbDataSourceWrapper.getInstance().getSdbUser(),
                SequoiadbDataSourceWrapper.getInstance().getSdbPassword(), thread, tagLibDomain);
        fileUpgradeExecutor = new FileUpgradeExecutor(upgradeStatus.getThread());
    }

    public WorkspaceTagUpgrader(UpgradeTagStatus upgradeStatus) {
        this.upgradeStatus = upgradeStatus;
        fileUpgradeExecutor = new FileUpgradeExecutor(upgradeStatus.getThread());
    }

    public void destroy() {
        fileUpgradeExecutor.destroy();
    }

    private void createTagLibAndMarkUpgrading(String ws) throws ScmToolsException {
        TagLibMgr.getInstance().createTagLibAndMarkUpgrading(ws, upgradeStatus.getTagLibDomain());
    }

    public void doUpgrade() throws ScmToolsException {
        logger.info("Status File Path: " + upgradeStatus.getStatusFilePath());
        System.out.println("Status File Path: " + upgradeStatus.getStatusFilePath());

        Sequoiadb db = null;
        try {
            db = SequoiadbDataSourceWrapper.getInstance().getConnection();
            if (upgradeStatus.getAllFailedFileCount() > 0) {
                retryFailedFile(db);
            }

            int currentWsIndex = 0;
            if (upgradeStatus.getCurrentWorkspace() != null) {
                currentWsIndex = upgradeStatus.getWsList()
                        .indexOf(upgradeStatus.getCurrentWorkspace());
                if (currentWsIndex == -1) {
                    throw new ScmToolsException(
                            "status file is invalid, currentWorkspace not found in workspace list: currentWs: "
                                    + upgradeStatus.getCurrentWorkspace() + ", wsList="
                                    + upgradeStatus.getWsList(),
                            ScmBaseExitCode.SYSTEM_ERROR);
                }
                createTagLibAndMarkUpgrading(upgradeStatus.getCurrentWorkspace());
                continueProcessWorkspaceFile(db);
                currentWsIndex += 1;
            }

            for (int i = currentWsIndex; i < upgradeStatus.getWsList().size(); i++) {
                String wsName = upgradeStatus.getWsList().get(i);
                createTagLibAndMarkUpgrading(wsName);
                processWorkspaceFile(db, wsName);
            }

            logger.info("all workspace processed, failedFileCount: {}",
                    upgradeStatus.getAllFailedFileCount());
            if (upgradeStatus.getAllFailedFileCount() > 0) {
                System.out.println("All workspace processed, but there are "
                        + upgradeStatus.getAllFailedFileCount()
                        + " files failed to upgrade, see log for detail and specified status file to retry.");
                System.out.println("Status File Path: " + upgradeStatus.getStatusFilePath());
            }
            else {
                System.out.println("All workspace processed successfully.");
            }
        }
        catch (ScmToolsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to do upgrade", ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(db);
        }
    }

    private WsBasicInfo getWsInfo(String wsName, Sequoiadb sdb) throws ScmToolsException {
        DBCollection wsCl = sdb.getCollectionSpace("SCMSYSTEM").getCollection("WORKSPACE");
        BSONObject record = wsCl.queryOne(
                new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName), null, null, null, 0);
        if (record == null) {
            throw new ScmToolsException("workspace not found: " + wsName,
                    ScmBaseExitCode.SYSTEM_ERROR);
        }

        String tagLibTable = BsonUtils.getStringChecked(record,
                FieldName.FIELD_CLWORKSPACE_TAG_LIB_TABLE);
        return new WsBasicInfo(wsName, tagLibTable);
    }

    private void processWorkspaceFile(Sequoiadb db, String wsName)
            throws ScmToolsException, InterruptedException {
        WsBasicInfo wsInfo = getWsInfo(wsName, db);
        upgradeStatus.setCurrentWorkspaceProcessedFileCount(0);
        upgradeStatus.setCurrentWorkspace(wsName);
        upgradeStatus.setCurrentWorkspaceFileCount(CommUtil.getWorkspaceFileCount(db, wsName));
        upgradeStatus.setCurrentWorkspaceProcessingScope(FileScope.CURRENT);
        upgradeStatus.setFileIdMarker(CommonDefine.FILE_ID_MARKER_BEGIN);
        upgradeStatus.save();

        WorkspaceProgressPrinter workspaceProgressPrinter = new WorkspaceProgressPrinter(
                upgradeStatus);
        workspaceProgressPrinter.printBegin();

        fileUpgradeExecutor.resetFileFinishCallback(workspaceProgressPrinter);

        processFileCollection(db, wsInfo, FileScope.CURRENT, CommonDefine.FILE_ID_MARKER_BEGIN);
        upgradeStatus.setCurrentWorkspaceProcessingScope(FileScope.HISTORY);
        upgradeStatus.setFileIdMarker(CommonDefine.FILE_ID_MARKER_BEGIN);
        upgradeStatus.save();

        processFileCollection(db, wsInfo, FileScope.HISTORY, CommonDefine.FILE_ID_MARKER_BEGIN);
        upgradeStatus.setFileIdMarker(CommonDefine.FILE_ID_MARKER_END);
        upgradeStatus.save();

        workspaceProgressPrinter.printUserTagProgressEnd(
                upgradeStatus.getCurrentWorkspaceProcessedFileCount(),
                upgradeStatus.getWorkspaceFailedFileCount(upgradeStatus.getCurrentWorkspace()));

        workspaceProgressPrinter.printEmptyCustomTagProcessing();
        processEmptyCustomTag(db, wsName);
        upgradeStatus.setCurrentWorkspaceEmptyCustomTagProcessed(true);
        upgradeStatus.save();
        markWorkspaceUpgradeComplete(wsInfo.getName());
    }

    private void markWorkspaceUpgradeComplete(String ws) throws ScmToolsException {
        TagLibMgr.getInstance().markWorkspaceUpgradeComplete(ws);
    }

    private void processEmptyCustomTag(Sequoiadb db, String wsName) {
        DBCollection fileCl = db.getCollectionSpace(wsName + "_META").getCollection("FILE");
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLFILE_CUSTOM_TAG,
                new BasicBSONObject("$et", new BasicBSONObject()));

        BasicBSONObject setList = new BasicBSONObject(FieldName.FIELD_CLFILE_CUSTOM_TAG,
                new BasicBSONList());
        BasicBSONObject updater = new BasicBSONObject("$set", setList);

        fileCl.update(matcher, updater, null);

        DBCollection historyFile = db.getCollectionSpace(wsName + "_META")
                .getCollection("FILE_HISTORY");
        historyFile.update(matcher, updater, null);
    }

    private void continueProcessWorkspaceFile(Sequoiadb db)
            throws ScmToolsException, InterruptedException {
        WsBasicInfo wsInfo = getWsInfo(upgradeStatus.getCurrentWorkspace(), db);
        WorkspaceProgressPrinter workspaceProgressPrinter = new WorkspaceProgressPrinter(
                upgradeStatus);
        workspaceProgressPrinter.printBegin();

        fileUpgradeExecutor.resetFileFinishCallback(workspaceProgressPrinter);

        if (upgradeStatus.getCurrentWorkspaceProcessingScope() == FileScope.CURRENT) {
            processFileCollection(db, wsInfo, FileScope.CURRENT, upgradeStatus.getFileIdMarker());
            upgradeStatus.setCurrentWorkspaceProcessingScope(FileScope.HISTORY);
            upgradeStatus.setFileIdMarker(CommonDefine.FILE_ID_MARKER_BEGIN);
            upgradeStatus.save();
        }
        processFileCollection(db, wsInfo, FileScope.HISTORY, upgradeStatus.getFileIdMarker());
        upgradeStatus.setFileIdMarker(CommonDefine.FILE_ID_MARKER_END);
        upgradeStatus.save();

        workspaceProgressPrinter.printUserTagProgressEnd(
                upgradeStatus.getCurrentWorkspaceProcessedFileCount(),
                upgradeStatus.getWorkspaceFailedFileCount(upgradeStatus.getCurrentWorkspace()));

        workspaceProgressPrinter.printEmptyCustomTagProcessing();

        if (!upgradeStatus.isCurrentWorkspaceEmptyCustomTagProcessed()) {
            processEmptyCustomTag(db, wsInfo.getName());
            upgradeStatus.setCurrentWorkspaceEmptyCustomTagProcessed(true);
            upgradeStatus.save();
        }

        markWorkspaceUpgradeComplete(wsInfo.getName());
    }

    private void retryFailedFile(Sequoiadb sdb) throws ScmToolsException, InterruptedException {
        logger.info("Processing failed files, failedFileCount: {}",
                upgradeStatus.getAllFailedFileCount());
        System.out.println(
                "Processing Failed Files (" + upgradeStatus.getAllFailedFileCount() + ")...");
        for (Map.Entry<String, List<FileBasicInfo>> entry : upgradeStatus.getFailedFile()
                .entrySet()) {
            WsBasicInfo wsInfo = getWsInfo(entry.getKey(), sdb);
            for (FileBasicInfo fileInfo : entry.getValue()) {
                fileUpgradeExecutor.submit(wsInfo, fileInfo);
            }
        }
        fileUpgradeExecutor.waitFinish();
        upgradeStatus.setFailedFile(fileUpgradeExecutor.getFailedFile());
        logger.info("Failed files process finish, still failed file count: {}",
                upgradeStatus.getAllFailedFileCount());
        if (upgradeStatus.getAllFailedFileCount() > 0) {
            System.out.println("Failed files process finish,  some file still failed to upgrade ("
                    + upgradeStatus.getAllFailedFileCount() + ")");
        }
        else {
            System.out.println("Failed files Upgrade success.");
        }
    }

    private void processFileCollection(Sequoiadb db, WsBasicInfo wsInfo, FileScope scope,
            String fileIdMarker) throws ScmToolsException, InterruptedException {
        if (fileIdMarker.equals(CommonDefine.FILE_ID_MARKER_END)) {
            return;
        }

        BasicBSONList andArr = new BasicBSONList();
        andArr.add(CommUtil.containTagCondition());
        if (!fileIdMarker.equals(CommonDefine.FILE_ID_MARKER_BEGIN)) {
            andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_ID,
                    new BasicBSONObject("$gt", fileIdMarker)));

        }

        int maxUpgradeFailedFileCount = UpgradeConfig.getInstance()
                .getIntConf("maxUpgradeFailedFileCount", 100);

        BasicBSONObject cond = new BasicBSONObject("$and", andArr);
        DBCollection fileCl = db.getCollectionSpace(wsInfo.getName() + "_META")
                .getCollection(scope.getFileClName());
        DBCursor cursor = fileCl.query(cond, null,
                new BasicBSONObject(FieldName.FIELD_CLFILE_ID, 1), null, 0);
        try {
            int submitCount = 0;
            while (cursor.hasNext()) {
                BSONObject record = cursor.getNext();
                fileUpgradeExecutor.submit(wsInfo, scope, record);
                submitCount++;
                if (submitCount >= 10000
                        || fileUpgradeExecutor.getFailedFileCount() > maxUpgradeFailedFileCount) {
                    fileUpgradeExecutor.waitFinish();
                    upgradeStatus.setFailedFile(fileUpgradeExecutor.getFailedFile());
                    upgradeStatus.incProcessedFileOfCurrentWorkspace(submitCount);
                    upgradeStatus.setFileIdMarker(
                            BsonUtils.getStringChecked(record, FieldName.FIELD_CLFILE_ID));
                    upgradeStatus.save();
                    if (fileUpgradeExecutor.getFailedFileCount() > maxUpgradeFailedFileCount) {
                        throw new ScmToolsException(
                                "Too many file failed: " + fileUpgradeExecutor.getFailedFileCount()
                                        + ", threshold: " + maxUpgradeFailedFileCount,
                                ScmBaseExitCode.SYSTEM_ERROR);
                    }
                    submitCount = 0;
                }
            }
            fileUpgradeExecutor.waitFinish();
            upgradeStatus.incProcessedFileOfCurrentWorkspace(submitCount);
            upgradeStatus.setFileIdMarker(CommonDefine.FILE_ID_MARKER_END);
            upgradeStatus.setFailedFile(fileUpgradeExecutor.getFailedFile());
            if (fileUpgradeExecutor.getFailedFileCount() > maxUpgradeFailedFileCount) {
                throw new ScmToolsException(
                        "Too many file failed: " + fileUpgradeExecutor.getFailedFileCount()
                                + ", threshold: " + maxUpgradeFailedFileCount,
                        ScmBaseExitCode.SYSTEM_ERROR);
            }
        }
        finally {
            cursor.close();
        }
    }
}
