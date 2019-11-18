package com.sequoiacm.fileversion.schedule;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.schedule.TestScheduleCommon;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestCopyHistory extends ScmTestMultiCenterBase {
    private ScmSession ssB;
    private ScmWorkspace wsB;
    private String file1Version1File;
    private String file1Version2File;
    private String downFile;
    private String file2Version1File;
    private String file2Version2File;
    private ScmWorkspace wsM;
    private ScmId scheduleId;
    private ScmSession ssM;
    private static final Logger logger = LoggerFactory.getLogger(TestCopyHistory.class);

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

        ScmSiteInfo rootsite = ScmTestTools.getSiteInfo(ssB, getSiteId1());
        ScmSiteInfo site2 = ScmTestTools.getSiteInfo(ssB, getSiteId2());
        ScmScheduleContent content = new ScmScheduleCopyFileContent(site2.getName(),
                rootsite.getName(), "0d", ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                .is(f1.getFileId().get()).get(), ScopeType.SCOPE_HISTORY);
        String cron = "* * * * * ?";
        ScmSchedule sch = ScmSystem.Schedule.create(ssB, getWorkspaceName(), ScheduleType.COPY_FILE,
                "s1", "desc", content, cron);

        scheduleId = sch.getId();

        waitFileBeCopy(f1.getFileId(), 1, 0);

        ScmFile f1History = ScmFactory.File.getInstance(wsM, f1.getFileId(), 1, 0);
        checkTransferedFile(f1History, file1Version1File);

        ScmFile f1Current = ScmFactory.File.getInstance(wsM, f1.getFileId(), 2, 0);
        checkOthreFile(f1Current);

        ScmFile f2Current = ScmFactory.File.getInstance(wsM, f2.getFileId(), 2, 0);
        checkOthreFile(f2Current);

        ScmFile f2History = ScmFactory.File.getInstance(wsM, f2.getFileId(), 1, 0);
        checkOthreFile(f2History);

        f1.delete(true);
        f2.delete(true);
    }

    private void waitFileBeCopy(ScmId id, int majorVersion, int minorVersion)
            throws ScmException, InterruptedException {
        boolean isTaskFinish = false;
        while (!isTaskFinish) {
            ScmFile tmpFile = ScmFactory.File.getInstance(wsB, id, majorVersion, minorVersion);
            List<ScmFileLocation> locationList = tmpFile.getLocationList();
            logger.info(ScmTestTools.formatLocationList(locationList));
            for (ScmFileLocation location : locationList) {
                if (location.getSiteId() == getSiteId1()) {
                    isTaskFinish = true;
                    break;
                }
            }
            Thread.sleep(1000);
        }
    }

    private void checkTransferedFile(ScmFile file, String srcFielData)
            throws IOException, ScmException {
        List<ScmFileLocation> list = file.getLocationList();
        Assert.assertEquals(list.size(), 2, file.toString());
        Assert.assertNotEquals(list.get(0).getSiteId(), list.get(1).getSiteId(), file.toString());
        Assert.assertTrue(
                list.get(1).getSiteId() == getSiteId1() || list.get(1).getSiteId() == getSiteId2());
        Assert.assertTrue(
                list.get(0).getSiteId() == getSiteId1() || list.get(0).getSiteId() == getSiteId2());
        ScmTestTools.deleteFile(downFile);
        file.getContentFromLocalSite(downFile);
        Assert.assertEquals(ScmTestTools.getMD5(downFile), ScmTestTools.getMD5(srcFielData));
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
        TestScheduleCommon.deleteScheduleSilence(ssB, scheduleId);
        ssB.close();
        ssM.close();
    }
}
