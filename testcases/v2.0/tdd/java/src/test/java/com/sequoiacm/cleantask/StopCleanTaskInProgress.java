package com.sequoiacm.cleantask;

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
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 通过2中心执行清理任务，将A中心的文件清理掉, 过程中停止任务
 * @author linyoubin
 *
 */
public class StopCleanTaskInProgress extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(StopCleanTaskInProgress.class);

    private String workingDir;
    private ScmSession site2Session;
    private ScmWorkspace ws;

    private long createFileTid;
    private List<ScmId> fileList = new ArrayList<>();
    private static int fileCount = 10;

    @BeforeClass
    public void setUp() throws ScmException {
        workingDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workingDir);
        ScmTestTools.createDir(workingDir);

        createFileTid = Thread.currentThread().getId();
        String fileDir = workingDir + File.separator + createFileTid;
        ScmTestTools.createDir(fileDir);

        site2Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), site2Session);
        for (int i = 0; i < fileCount; i++) {
            String file = fileDir + File.separator + StopCleanTaskInProgress.class.getName()+"file" + i + ".txt";
            ScmTestTools.createFile(file, ScmTestTools.generateString(fileCount), 1025 * 1024);
            ScmId fileId = ScmTestTools.createScmFile(ws, file, "file" + i, "", "").getFileId();
            fileList.add(fileId);
        }

        //read files in order to cache file in all centers
        ScmSession tmpSession3 = null;
        try {
            String downFile1 = fileDir + File.separator + "down1.txt";
            tmpSession3 = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(getServer3().getUrl(), getScmUser(),
                            getScmPasswd()));
            ScmWorkspace ws3 = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), tmpSession3);
            for (int i = 0; i< fileCount; i++) {
                ScmTestTools.deleteDir(downFile1);
                ScmFile myFile = ScmFactory.File.getInstance(ws3, fileList.get(i));
                myFile.getContent(downFile1);
            }
        }
        finally {
            if (null != tmpSession3) {
                tmpSession3.close();
            }
        }
    }

    @Test
    public void stopCleanTaskInProgress() throws ScmException, InterruptedException, IOException {

        List<String> idList = new ArrayList<>();
        for(ScmId fileId : fileList) {
            idList.add(fileId.get());
        }

        BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).in(idList).get();
        logger.info("condition=" + condition.toString());

        //start task
        ScmId taskId = ScmSystem.Task.startCleanTask(ws, condition);
        Thread.sleep(20);

        // stop task
        ScmSystem.Task.stopTask(site2Session, taskId);

        int progress = 0;
        while (true) {
            ScmTask taskInfo = ScmSystem.Task.getTask(site2Session, taskId);
            logger.info(taskInfo);
            if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL) {
                if (taskInfo.getStopTime() != null) {
                    progress = taskInfo.getProgress();
                    break;
                }
            }

            if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
                logger.warn("task have been finished");
                break;
            }

            Thread.sleep(500);
        }

        long tid = Thread.currentThread().getId();
        String outputDir = workingDir + File.separator + ScmTestTools.getMethodName()
        + File.separator + tid;
        ScmTestTools.createDir(outputDir);

        int cleanedCount = 0;
        for(ScmId fileId : fileList) {
            ScmFile file = ScmFactory.File.getInstance(ws, fileId);
            List<ScmFileLocation> locationList = file.getLocationList();

            logger.info("locationList=" + ScmTestTools.formatLocationList(locationList));
            Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId1()),
                    ScmTestTools.formatLocationList(locationList));
            Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId3()),
                    ScmTestTools.formatLocationList(locationList));

            if (!ScmTestTools.isSiteExist(locationList, getSiteId2())) {
                cleanedCount++;
            }
        }

        logger.info("cleanedCount=" + cleanedCount + ",progress=" + progress);
        if (progress < 80) {
            Assert.assertTrue(cleanedCount < fileList.size(), "cleanedCount=" + cleanedCount
                    + ",progress=" + progress);
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
