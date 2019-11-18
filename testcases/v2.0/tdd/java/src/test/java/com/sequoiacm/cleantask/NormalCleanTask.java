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
 * 通过2中心执行清理任务，将A中心的文件清理掉 文件 中心id file1 2 1 3 file2 2 1 3 file3 2
 *
 * 从2中心执行清理任务 file2/file3 预期结果 文件 中心id file1 2 1 3 file2 1 3 file3 2
 * 
 * @author linyoubin
 *
 */
public class NormalCleanTask extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(NormalCleanTask.class);

    private String workingDir;
    private ScmSession site2Session;
    private ScmWorkspace ws;

    private long createFileTid;
    ScmId fileId1;
    ScmId fileId2;
    ScmId fileId3;

    @BeforeClass
    public void setUp() throws ScmException {
        workingDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workingDir);
        ScmTestTools.createDir(workingDir);

        createFileTid = Thread.currentThread().getId();
        String fileDir = workingDir + File.separator + createFileTid;
        ScmTestTools.createDir(fileDir);

        site2Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), site2Session);
        String file = fileDir + File.separator + "file.txt";
        ScmTestTools.createFile(file, ScmTestTools.generateString(100), 1025 * 1024);

        String downFile1 = fileDir + File.separator + "down1.txt";
        String downFile2 = fileDir + File.separator + "down2.txt";
        System.out.println("asdas-------------------------------");
        fileId1 = ScmTestTools.createScmFile(ws, file, NormalCleanTask.class.getName() + "file1",
                "", "").getFileId();
        fileId2 = ScmTestTools.createScmFile(ws, file, NormalCleanTask.class.getName() + "file2",
                "", "").getFileId();
        fileId3 = ScmTestTools.createScmFile(ws, file, NormalCleanTask.class.getName() + "file3",
                "", "").getFileId();

        ScmSession tmpSession3 = null;
        try {
            tmpSession3 = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(getServer3().getUrl(), getScmUser(), getScmPasswd()));
            ScmWorkspace ws3 = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), tmpSession3);
            ScmFile myFile = ScmFactory.File.getInstance(ws3, fileId1);
            myFile.getContent(downFile1);

            myFile = ScmFactory.File.getInstance(ws3, fileId2);
            myFile.getContent(downFile2);
        }
        finally {
            if (null != tmpSession3) {
                tmpSession3.close();
            }
        }
    }

    @Test
    public void cleanFile() throws ScmException, InterruptedException, IOException {

        List<String> idList = new ArrayList<>();
        idList.add(fileId2.get());
        idList.add(fileId3.get());

        BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).in(idList)
                .get();
        logger.info("condition=" + condition.toString());
        ScmId taskId = ScmSystem.Task.startCleanTask(ws, condition);
        while (true) {
            ScmTask taskInfo = ScmSystem.Task.getTask(site2Session, taskId);
            logger.info(taskInfo);
            if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
                break;
            }

            Thread.sleep(500);
        }
        /*
         * 预期结果 中心id file1 2 1 3 file2 1 3 file3 2
         */
        ScmFile file = ScmFactory.File.getInstance(ws, fileId1);
        List<ScmFileLocation> locationList = file.getLocationList();
        logger.info("locationList=" + ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId1()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId2()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId3()),
                ScmTestTools.formatLocationList(locationList));

        file = ScmFactory.File.getInstance(ws, fileId2);
        locationList = file.getLocationList();
        logger.info("locationList=" + ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId1()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(!ScmTestTools.isSiteExist(locationList, getSiteId2()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId3()),
                ScmTestTools.formatLocationList(locationList));

        file = ScmFactory.File.getInstance(ws, fileId3);
        locationList = file.getLocationList();
        logger.info("locationList=" + ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(!ScmTestTools.isSiteExist(locationList, getSiteId1()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId2()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(!ScmTestTools.isSiteExist(locationList, getSiteId3()),
                ScmTestTools.formatLocationList(locationList));
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.removeScmFileSilence(ws, fileId1);
        ScmTestTools.removeScmFileSilence(ws, fileId2);
        ScmTestTools.removeScmFileSilence(ws, fileId3);

        site2Session.close();
    }
}
