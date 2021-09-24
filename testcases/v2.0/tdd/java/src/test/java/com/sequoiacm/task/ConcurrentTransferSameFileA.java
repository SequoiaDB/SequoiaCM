package com.sequoiacm.task;

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
 * A站点并发向B、C站点迁移同一个文件，文件内容只在A
 */
public class ConcurrentTransferSameFileA extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(ConcurrentTransferSameFileA.class);
    private ScmId file;
    private ScmSession site1RootSiteSession;
    private ScmWorkspace wsSite1Session;

    @BeforeClass
    public void setUp() throws Exception {
        site1RootSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        wsSite1Session = ScmFactory.Workspace.getWorkspace(getWorkspaceName(),
                site1RootSiteSession);

        // 创建一个文件，文件内容落在A主站点
        prepareFile();
    }

    private void prepareFile() throws Exception {
        file = ScmTestTools.createScmFile(wsSite1Session, 10*1024).getFileId();
    }

    @Test
    public void doTest() throws Exception {
        String site2Name = ScmTestTools.getSiteInfo(site1RootSiteSession, getSiteId2()).getName();
        String site3Name = ScmTestTools.getSiteInfo(site1RootSiteSession, getSiteId3()).getName();
        ScmId taskId1 = ScmSystem.Task.startTransferTask(wsSite1Session, new BasicBSONObject(ScmAttributeName.File.FILE_ID, file.get()), ScmType.ScopeType.SCOPE_CURRENT, site2Name);
        ScmId taskId2 = ScmSystem.Task.startTransferTask(wsSite1Session, new BasicBSONObject(ScmAttributeName.File.FILE_ID, file.get()), ScmType.ScopeType.SCOPE_CURRENT, site3Name);

        ScmTestTools.waitTask(site1RootSiteSession, taskId1,600000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
        ScmTestTools.waitTask(site1RootSiteSession, taskId2,60000,
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);

        ScmTask task1 = ScmSystem.Task.getTask(site1RootSiteSession, taskId1);
        ScmTask task2 = ScmSystem.Task.getTask(site1RootSiteSession, taskId2);
        String message = task1.toString() + "          " + task2.toString();

        Assert.assertEquals(task1.getActualCount(), 1, message);
        Assert.assertEquals(task2.getActualCount(), 1, message);

        ScmFile fileInstance = ScmFactory.File.getInstance(wsSite1Session, file);
        List<ScmFileLocation> locations = fileInstance.getLocationList();

        Set<Integer> siteIdList = new TreeSet<>();
        for (ScmFileLocation l : locations) {
            siteIdList.add(l.getSiteId());
        }
        Assert.assertEquals(siteIdList,
                new TreeSet<>(Arrays.asList(getSiteId1(), getSiteId2(), getSiteId3())));
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmFactory.File.deleteInstance(wsSite1Session, file, true);
        site1RootSiteSession.close();
    }
}
