package com.sequoiacm.transfertask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.*;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 通过A中心执行迁移任务，将文件迁移到主中心, 过程中列取任务
 *
 * @author linyoubin
 *
 */
public class ListTask extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(ListTask.class);

    private String workingDir;
    private ScmSession site2Session;
    private String coord2;
    private String mainCoord;
    private ScmWorkspace ws;

    private long createFileTid;
    private List<ScmId> fileList = new ArrayList<>();
    private static int fileCount = 10;

    @BeforeClass
    public void setUp() throws ScmException {
        coord2 = getServer2().getUrl();
        mainCoord = getServer1().getUrl();

        workingDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workingDir);
        ScmTestTools.createDir(workingDir);

        createFileTid = Thread.currentThread().getId();
        String fileDir = workingDir + File.separator + createFileTid;
        ScmTestTools.createDir(fileDir);

        site2Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), site2Session);
        for (int i = 0; i < fileCount ; i++) {
            String file = fileDir + File.separator + "file" + i + ".txt";
            ScmTestTools.createFile(file, ScmTestTools.generateString(fileCount), 1025 * 1024);
            ScmId fileId = ScmTestTools.createScmFile(ws, file, ListTask.class.getName()+i, "", "").getFileId();
            fileList.add(fileId);
        }
    }

    private void displayAllTask() throws ScmException {
        BSONObject c2 = ScmQueryBuilder.start().get();
        ScmCursor<ScmTaskBasicInfo> cursor = null;
        try {
            cursor = ScmSystem.Task.listTask(site2Session, c2);
            while(cursor.hasNext()) {
                ScmTaskBasicInfo basickInfo = cursor.getNext();
                logger.info(basickInfo);
            }
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    @Test
    public void transferFile() throws ScmException, InterruptedException, IOException {
        List<String> idList = new ArrayList<>();
        for(ScmId fileId : fileList) {
            idList.add(fileId.get());
        }

        logger.info("before start task, display all tasks");
        displayAllTask();

        logger.info("start task");
        BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).in(idList).get();
        logger.info("condition=" + condition.toString());
        ScmId taskId = ScmSystem.Task.startTransferTask(ws, condition);
        while (true) {
            displayAllTask();

            ScmTask taskInfo = ScmSystem.Task.getTask(site2Session, taskId);
            if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
                break;
            }

            Thread.sleep(500);
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        for(ScmId fileId : fileList) {
            ScmTestTools.removeScmFileSilence(ws, fileId);
        }

        site2Session.close();
    }
}
