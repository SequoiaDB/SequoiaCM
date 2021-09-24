package com.sequoiacm.task;

import com.google.common.io.NullOutputStream;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A站点并发迁移(迁移到B)清理同一个文件,文件在A、C站点
 */
public class ConcurrentCleanTransferSameFile extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(ConcurrentCleanTransferSameFile.class);
    private ScmSession site2BranchSiteSession;
    private ScmSession site3branchSiteSession;
    private ScmId file;
    private ScmSession site1RootSiteSession;
    private ScmWorkspace wsSite1Session;
    private ScmWorkspace wsSite2Session;
    private ScmWorkspace wsSite3Session;

    @BeforeClass
    public void setUp() throws Exception {
        site1RootSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        site2BranchSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        site3branchSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer3().getUrl(), getScmUser(), getScmPasswd()));
        wsSite1Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site1RootSiteSession);
        wsSite2Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site2BranchSiteSession);
        wsSite3Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site3branchSiteSession);

        // 创建一个文件，文件内容落在A、C两个站点
        prepareFile();
    }

    private void prepareFile() throws Exception {
        file = ScmTestTools.createScmFile(wsSite1Session, 30 * 1024 * 1024).getFileId();
        ScmFactory.File.getInstance(wsSite3Session, file).getContent(new NullOutputStream());
    }

    @Test
    public void doTest() throws Exception {
        String site2Name = ScmTestTools.getSiteInfo(site1RootSiteSession, getSiteId2()).getName();

        ScmId taskId2 = ScmSystem.Task.startTransferTask(wsSite1Session,
                new BasicBSONObject(ScmAttributeName.File.FILE_ID, file.get()),
                ScmType.ScopeType.SCOPE_CURRENT, site2Name);
        ScmId taskId1 = ScmSystem.Task.startCleanTask(wsSite1Session,
                new BasicBSONObject(ScmAttributeName.File.FILE_ID, file.get()));

        ScmTestTools.waitTask(site1RootSiteSession, taskId1, 60000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH,
                CommonDefine.TaskRunningFlag.SCM_TASK_ABORT);
        ScmTestTools.waitTask(site1RootSiteSession, taskId2, 60000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH,
                CommonDefine.TaskRunningFlag.SCM_TASK_ABORT);

        ScmTask task1 = ScmSystem.Task.getTask(site1RootSiteSession, taskId1);
        ScmTask task2 = ScmSystem.Task.getTask(site1RootSiteSession, taskId2);

        ScmFile fileInstance = ScmFactory.File.getInstance(wsSite1Session, file);
        List<ScmFileLocation> locations = fileInstance.getLocationList();
        Set<Integer> siteIdList = new TreeSet<>();
        for (ScmFileLocation l : locations) {
            siteIdList.add(l.getSiteId());
        }

        String message = task1.toString() + "          " + task2.toString();

        if (task1.getActualCount() == 1 && task2.getActualCount() == 1) {
            // 迁移清理都成功
            Assert.assertEquals(siteIdList,
                    new TreeSet<>(Arrays.asList(getSiteId2(), getSiteId3())));
            return;
        }

        if (task2.getActualCount() == 1) {
            // 只有迁移成功
            Assert.assertEquals(siteIdList,
                    new TreeSet<>(Arrays.asList(getSiteId1(), getSiteId2(), getSiteId3())));
            return;
        }

        // 只有清理成功
        Assert.assertEquals(task1.getActualCount(), 1, message);
        Assert.assertEquals(siteIdList, new TreeSet<>(Arrays.asList(getSiteId3())));
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmFactory.File.deleteInstance(wsSite2Session, file, true);
        site1RootSiteSession.close();
        site2BranchSiteSession.close();
        site3branchSiteSession.close();
    }
}
