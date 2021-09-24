package com.sequoiacm.task;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * A站点并发清理同一个文件，文件内容存在于 A、B站点
 */
public class ConcurrentCleanSameFileB extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(ConcurrentCleanSameFileB.class);
    private ScmSession site2BranchSiteSession;
    private ScmId file;
    private ScmSession site1RootSiteSession;
    private ScmWorkspace wsSite1Session;
    private ScmWorkspace wsSite2Session;

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

        // 创建一个文件，文件内容落在A、B站点
        prepareFile();
    }

    private void prepareFile() throws Exception {
        file = ScmTestTools.createScmFile(wsSite2Session, 10).getFileId();
        ScmFactory.File.getInstance(wsSite1Session, file).getContent(new NullOutputStream());
    }

    @Test
    public void doTest() throws Exception {
        ScmId taskId1 = ScmSystem.Task.startCleanTask(wsSite2Session,
                new BasicBSONObject(ScmAttributeName.File.FILE_ID, file.get()));
        ScmId taskId2 = ScmSystem.Task.startCleanTask(wsSite2Session,
                new BasicBSONObject(ScmAttributeName.File.FILE_ID, file.get()));

        ScmTestTools.waitTask(site1RootSiteSession, taskId1,60000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
        ScmTestTools.waitTask(site1RootSiteSession, taskId2,60000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);

        ScmFile fileInstance = ScmFactory.File.getInstance(wsSite1Session, file);
        List<ScmFileLocation> locations = fileInstance.getLocationList();
        ScmTask task1 = ScmSystem.Task.getTask(site1RootSiteSession, taskId1);
        ScmTask task2 = ScmSystem.Task.getTask(site1RootSiteSession, taskId2);
        String message = task1.toString() + "          " + task2.toString();
        if (task1.getActualCount() == 1) {
            Assert.assertEquals(task2.getActualCount(), 0,
                    message);
        } else {
            Assert.assertEquals(task1.getActualCount(), 0,
                    message);
            Assert.assertEquals(task2.getActualCount(), 1,
                    message);
        }
        Assert.assertEquals(locations.size(), 1, fileInstance.toString());
        Assert.assertEquals(locations.get(0).getSiteId(), getSiteId1());
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmFactory.File.deleteInstance(wsSite2Session, file, true);
        site1RootSiteSession.close();
        site2BranchSiteSession.close();
    }
}
