package com.sequoiacm.task;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A、B站点并发清理不同文件
 */
public class ConcurrentCleanDiffFileB extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(ConcurrentCleanDiffFileB.class);
    private ScmSession site2BranchSiteSession;
    private List<ScmFile> fileForClean = new ArrayList<>();
    private String task1CleanAuthor = UUID.randomUUID().toString();
    private String task2CleanAuthor = UUID.randomUUID().toString();
    private ScmSession site1RootSiteSession;
    private ScmWorkspace wsSite1Session;
    private ScmWorkspace wsSite2Session;
    private ScmSession site3BranchSiteSession;
    private ScmWorkspace wsSite3Session;

    @BeforeClass
    public void setUp() throws Exception {
        site1RootSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        site2BranchSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        site3BranchSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer3().getUrl(), getScmUser(), getScmPasswd()));
        wsSite1Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site1RootSiteSession);
        wsSite2Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site2BranchSiteSession);
        wsSite3Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site3BranchSiteSession);

        // 创建两批文件，文件内容落在A、B站点
        prepareFile();
    }

    private void prepareFile() throws Exception {

        for (int i = 0; i < 10; i++) {
            ScmFile file = ScmTestTools.createScmFile(wsSite2Session, 10);
            file.setAuthor(task1CleanAuthor);
            fileForClean.add(file);
            ScmFactory.File.getInstance(wsSite1Session, file.getFileId())
                    .getContent(new NullOutputStream());
        }
        for (int i = 0; i < 10; i++) {
            ScmFile file = ScmTestTools.createScmFile(wsSite3Session, 10);
            file.setAuthor(task2CleanAuthor);
            fileForClean.add(file);
            ScmFactory.File.getInstance(wsSite1Session, file.getFileId())
                    .getContent(new NullOutputStream());
        }

    }

    @Test
    public void doTest() throws Exception {
        ScmId taskInSite2Id = ScmSystem.Task.startCleanTask(wsSite2Session,
                new BasicBSONObject(ScmAttributeName.File.AUTHOR, task1CleanAuthor));
        ScmId taskInSite3Id = ScmSystem.Task.startCleanTask(wsSite3Session,
                new BasicBSONObject(ScmAttributeName.File.AUTHOR, task2CleanAuthor));

        ScmTestTools.waitTask(site1RootSiteSession, taskInSite2Id, 60000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
        ScmTestTools.waitTask(site1RootSiteSession, taskInSite3Id, 60000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);

        ScmTask task1 = ScmSystem.Task.getTask(site1RootSiteSession, taskInSite2Id);
        ScmTask task2 = ScmSystem.Task.getTask(site1RootSiteSession, taskInSite3Id);
        String message = task1.toString() + "          " + task2.toString();
        Assert.assertEquals(task1.getActualCount(), 10, message);
        Assert.assertEquals(task2.getActualCount(), 10, message);

        for (ScmFile f : fileForClean) {
            f = ScmFactory.File.getInstance(wsSite1Session, f.getFileId());
            Assert.assertEquals(f.getLocationList().size(), 1);
            Assert.assertEquals(f.getLocationList().get(0).getSiteId(), getSiteId1());
        }

    }

    @AfterClass
    public void tearDown() throws ScmException {
        for (ScmFile file : fileForClean) {
            file.delete(true);
        }
        site1RootSiteSession.close();
        site2BranchSiteSession.close();
        site3BranchSiteSession.close();
    }
}
