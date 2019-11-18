package com.sequoiacm.transfertask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 通过A中心执行迁移任务，将文件迁移到主中心时，终止任务
 *
 * @author linyoubin
 *
 */
public class StopTaskInProgress extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(StopTaskInProgress.class);

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
            ScmId fileId = ScmTestTools.createScmFile(ws, file, StopTaskInProgress.class.getName()+i, "", "").getFileId();
            fileList.add(fileId);
        }
    }

    @Test
    public void stopTaskInProgress() throws ScmException, InterruptedException, IOException {

        List<String> idList = new ArrayList<>();
        for(ScmId fileId : fileList) {
            idList.add(fileId.get());
        }

        BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).in(idList).get();
        logger.info("condition=" + condition.toString());

        //start task
        ScmId taskId = ScmSystem.Task.startTransferTask(ws, condition);

        ScmTask taskInfo = ScmSystem.Task.getTask(site2Session, taskId);
        logger.info(taskInfo);
        // wait for a moment to let task transfer some files
        Thread.sleep(500);
        taskInfo = ScmSystem.Task.getTask(site2Session, taskId);
        logger.info(taskInfo);

        // stop task
        ScmSystem.Task.stopTask(site2Session, taskId);
        while (true) {
            taskInfo = ScmSystem.Task.getTask(site2Session, taskId);
            logger.info(taskInfo);
            if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL
                    || taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
                if (taskInfo.getStopTime() != null) {
                    break;
                }
            }

            Thread.sleep(500);
        }

        int index = 0;
        long tid = Thread.currentThread().getId();
        String outputDir = workingDir + File.separator + ScmTestTools.getMethodName()
        + File.separator + tid;
        ScmTestTools.createDir(outputDir);

        for(ScmId fileId : fileList) {
            ScmFile file = ScmFactory.File.getInstance(ws, fileId);
            List<ScmFileLocation> locationList = file.getLocationList();

            logger.info("locationList=" + ScmTestTools.formatLocationList(locationList));
            if (ScmTestTools.isSiteExist(locationList, getSiteId1())) {
                Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId2()),
                        ScmTestTools.formatLocationList(locationList));
                Assert.assertFalse(ScmTestTools.isSiteExist(locationList, getSiteId3()),
                        ScmTestTools.formatLocationList(locationList));

                String srcFile = workingDir + File.separator + createFileTid + File.separator + "file"
                        + index + ".txt";
                String downFile = outputDir + File.separator + "file" + index + ".txt";
                logger.info("check lob:fileId=" + fileId.get() + ",lobId=" + file.getDataId().get()
                        + ",src=" + srcFile);
                logger.info("downfile=" + downFile);

                // check site2
                ScmTestTools.checkLob(getServer2().getUrl(), getScmUser(),
                        getScmPasswd(), getWorkspaceName(), file.getFileId(), srcFile, downFile);

                // check mainsite
                ScmTestTools.checkLob(getServer1().getUrl(), getScmUser(),
                        getScmPasswd(), getWorkspaceName(), file.getFileId(), srcFile, downFile);

                try {
                    // check site3
                    ScmTestTools.checkLob(getServer3().getUrl(), getScmUser(),
                            getScmPasswd(), getWorkspaceName(), file.getFileId(), srcFile, downFile);
                }
                catch (ScmException e) {
                    Assert.assertEquals(e.getErrorCode(),
                            ScmError.DATA_NOT_EXIST.getErrorCode(), e.getMessage());
                }
            }

            index++;
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
