package com.sequoiacm.schedule;

import com.google.common.io.NullOutputStream;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TestConcurrentSchedule extends ScmTestMultiCenterBase {
    private final static Logger logger = Logger.getLogger(TestConcurrentSchedule.class);
    private ScmSession site2BranchSiteSession;
    private ScmId fileForClean;
    private ScmSession site1RootSiteSession;
    private ScmWorkspace wsSite1Session;
    private ScmWorkspace wsSite2Session;
    private ScmId fileForTransfer;
    private ScmId copyScheduleId;
    private ScmId cleanScheduleId;

    @BeforeClass
    public void setUp() throws Exception {
        site1RootSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        site2BranchSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        wsSite1Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site1RootSiteSession);
        wsSite2Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site2BranchSiteSession);

        prepareFile();
    }

    private void prepareFile() throws Exception {
        fileForClean = ScmTestTools.createScmFile(wsSite1Session, 30 * 1024 * 1024).getFileId();
        ScmFactory.File.getInstance(wsSite2Session, fileForClean)
                .getContent(new NullOutputStream());

        fileForTransfer = ScmTestTools.createScmFile(wsSite2Session, 30 * 1024 * 1024).getFileId();
    }

    @Test
    public void createSchedule() throws ScmException {

        try {
            ScmSiteInfo rootSite = ScmTestTools.getSiteInfo(site1RootSiteSession, getSiteId1());
            ScmSiteInfo site2 = ScmTestTools.getSiteInfo(site1RootSiteSession, getSiteId2());

            ScmScheduleContent copyFileContent = new ScmScheduleCopyFileContent(site2.getName(),
                    rootSite.getName(), "0d",
                    new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileForTransfer.get()));
            String cron = "* * * * * ?";
            ScmSchedule copySch = ScmSystem.Schedule.create(site1RootSiteSession,
                    getWorkspaceName(), ScheduleType.COPY_FILE, "s1", "desc", copyFileContent,
                    cron);
            copyScheduleId = copySch.getId();

            ScmScheduleCleanFileContent cleanContent = new ScmScheduleCleanFileContent(
                    site2.getName(), "0d",
                    new BasicBSONObject(FieldName.FIELD_CLFILE_ID, fileForClean.get()));
            ScmSchedule cleanSch = ScmSystem.Schedule.create(site1RootSiteSession,
                    getWorkspaceName(), ScheduleType.CLEAN_FILE, "s1", "desc", cleanContent, cron);
            cleanScheduleId = cleanSch.getId();

            ScmTask cleanTask;
            ScmTask transferTask;
            while ((cleanTask = cleanSch.getLatestTask()) == null
                    || (transferTask = copySch.getLatestTask()) == null) {
                Thread.sleep(1000);
            }
            List<ScmTask> cleanTasks = cleanSch.getLatestTasks(10000);
            ScmTask firstCleanTask = cleanTasks.get(cleanTasks.size() - 1);

            List<ScmTask> copyTasks = copySch.getLatestTasks(10000);
            ScmTask firstCopyTask = copyTasks.get(copyTasks.size() - 1);

            ScmTestTools.waitTask(site1RootSiteSession, firstCleanTask.getId(), 60000,
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
            ScmTestTools.waitTask(site1RootSiteSession, firstCopyTask.getId(), 60000,
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
            ScmFile fileForCleanInstance = ScmFactory.File.getInstance(wsSite1Session,
                    fileForClean);
            Assert.assertEquals(fileForCleanInstance.getLocationList().size(), 1);
            Assert.assertEquals(fileForCleanInstance.getLocationList().get(0).getSiteId(),
                    getSiteId1());

            ScmFile fileForTransferInstance = ScmFactory.File.getInstance(wsSite1Session,
                    fileForTransfer);
            Assert.assertEquals(fileForTransferInstance.getLocationList().size(), 2);
            Set<Integer> siteIdList = new TreeSet<>();
            for (ScmFileLocation l : fileForTransferInstance.getLocationList()) {
                siteIdList.add(l.getSiteId());
            }
            Assert.assertEquals(siteIdList,
                    new TreeSet<>(Arrays.asList(getSiteId1(), getSiteId2())));

        }
        catch (Exception e) {
            logger.error("createSchedule failed", e);
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmFactory.File.deleteInstance(wsSite2Session, fileForTransfer, true);
        ScmFactory.File.deleteInstance(wsSite2Session, fileForClean, true);
        ScmSystem.Schedule.delete(site1RootSiteSession, cleanScheduleId);
        ScmSystem.Schedule.delete(site1RootSiteSession, copyScheduleId);
        site1RootSiteSession.close();
        site2BranchSiteSession.close();
    }
}