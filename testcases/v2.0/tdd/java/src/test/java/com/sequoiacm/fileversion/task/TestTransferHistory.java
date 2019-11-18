package com.sequoiacm.fileversion.task;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestTransferHistory extends ScmTestMultiCenterBase {
    private ScmSession ssB;
    private ScmWorkspace wsB;
    private String file1Version1File;
    private String file1Version2File;
    private String downFile;
    private String file2Version1File;
    private String file2Version2File;
    private ScmSession ssM;
    private ScmWorkspace wsM;
    private static final Logger logger = LoggerFactory.getLogger(TestTransferHistory.class);

    @BeforeClass
    public void init() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workDir);
        ScmTestTools.createDir(workDir);
        file1Version1File = workDir + File.separator + "file1version1.data";
        file1Version2File = workDir + File.separator + "file1version2.data";
        ScmTestTools.createFile(file1Version1File, "file1version1", 1024);
        ScmTestTools.createFile(file1Version2File, "file1version2", 2048);

        file2Version1File = workDir + File.separator + "file2version1.data";
        file2Version2File = workDir + File.separator + "file2version2.data";
        ScmTestTools.createFile(file2Version1File, "file2version1", 1024);
        ScmTestTools.createFile(file2Version2File, "file2version2", 2048);

        downFile = workDir + File.separator + "downFile.data";

        ssB = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        wsB = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ssB);

        ssM = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        wsM = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ssM);
        clearEnv();
    }

    private void clearEnv() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(wsB, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR)
                .is(this.getClass().getSimpleName()).get());
        try {
            while (c.hasNext()) {
                ScmFactory.File.deleteInstance(wsB, c.getNext().getFileId(), true);
            }
        }
        finally {
            c.close();
        }
    }

    @Test
    public void test() throws ScmException, InterruptedException, IOException {
        ScmFile f1 = ScmFactory.File.createInstance(wsB);
        f1.setFileName(this.getClass().getSimpleName() + "1");
        f1.setAuthor(this.getClass().getSimpleName());
        f1.setContent(file1Version1File);
        f1.save();
        f1.updateContent(file1Version2File);

        ScmFile f2 = ScmFactory.File.createInstance(wsB);
        f2.setFileName(this.getClass().getSimpleName() + "2");
        f2.setContent(file2Version1File);
        f2.setAuthor(this.getClass().getSimpleName());
        f2.save();
        f2.updateContent(file2Version2File);

        ScmId taskId = ScmSystem.Task
                .startTransferTask(wsB,
                        ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                        .is(f1.getFileId().get()).get(),
                        ScopeType.SCOPE_HISTORY);
        ScmTask taskInfo;
        while (true) {
            taskInfo = ScmSystem.Task.getTask(ssB, taskId);
            if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH
                    || taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_ABORT) {
                break;
            }
            logger.info(taskInfo.toString());
            Thread.sleep(500);
        }

        Assert.assertTrue(taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH,
                taskInfo.toString());

        Assert.assertEquals(taskInfo.getEstimateCount(), 1, taskInfo.toString());
        Assert.assertEquals(taskInfo.getActualCount(), 1, taskInfo.toString());
        Assert.assertEquals(taskInfo.getSuccessCount(), 1, taskInfo.toString());

        ScmFile f1History = ScmFactory.File.getInstance(wsM, f1.getFileId(), 1, 0);
        List<ScmFileLocation> list = f1History.getLocationList();
        Assert.assertEquals(list.size(), 2, f1History.toString());
        Assert.assertNotEquals(list.get(0).getSiteId(), list.get(1).getSiteId(),
                f1History.toString());
        Assert.assertTrue(
                list.get(1).getSiteId() == getSiteId1() || list.get(1).getSiteId() == getSiteId2());
        Assert.assertTrue(
                list.get(0).getSiteId() == getSiteId1() || list.get(0).getSiteId() == getSiteId2());
        f1History.getContentFromLocalSite(downFile);
        Assert.assertEquals(ScmTestTools.getMD5(downFile), ScmTestTools.getMD5(file1Version1File));

        ScmFile f1Current = ScmFactory.File.getInstance(wsM, f1.getFileId(), 2, 0);
        checkOthreFile(f1Current);

        ScmFile f2Current = ScmFactory.File.getInstance(wsM, f2.getFileId(), 2, 0);
        checkOthreFile(f2Current);

        ScmFile f2History = ScmFactory.File.getInstance(wsM, f2.getFileId(), 1, 0);
        checkOthreFile(f2History);

        f1.delete(true);
        f2.delete(true);
    }


    private void checkOthreFile(ScmFile file) throws ScmException {
        List<ScmFileLocation> currentList = file.getLocationList();
        Assert.assertEquals(currentList.size(), 1, file.toString());
        Assert.assertEquals(currentList.get(0).getSiteId(), getSiteId2(), file.toString());

        try {
            ScmTestTools.deleteFile(downFile);
            file.getContentFromLocalSite(downFile);
            Assert.fail("root site should not have data:" + file.toString());
        }
        catch (ScmException e) {

        }
    }
    @AfterClass
    public void tearDown() {
        ssB.close();
    }
}
