package com.sequoiacm.scheduletask;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-5228:并发执行多个迁移并清理任务，源站点相同，目标站点不同
 * @Author zhangyanan
 * @Date 2022.09.21
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2022.09.21
 * @version 1.00
 */
public class ScheduleTask5228 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file5228";
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private WsWrapper wsp = null;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private boolean runSuccess = false;
    private CopyOnWriteArrayList< ScmTask > successTasks = new CopyOnWriteArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > branchSites = ScmInfo.getBranchSites( 2 );
        branchSite1 = branchSites.get( 0 );
        branchSite2 = branchSites.get( 1 );

        wsp = ScmInfo.getWs();
        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        fileId = ScmFileUtils.create( rootSiteWs, filename, filePath );

    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        ThreadCreateMoveSchedule threadMoveSchedule1 = new ThreadCreateMoveSchedule(
                rootSite, branchSite1 );
        ThreadCreateMoveSchedule threadMoveSchedule2 = new ThreadCreateMoveSchedule(
                rootSite, branchSite2 );
        es.addWorker( threadMoveSchedule1 );
        es.addWorker( threadMoveSchedule2 );
        es.run();

        ScmTask moveTask1 = ScmSystem.Task.getTask( rootSiteSession,
                threadMoveSchedule1.getTaskId() );
        ScmTask moveTask2 = ScmSystem.Task.getTask( rootSiteSession,
                threadMoveSchedule2.getTaskId() );

        // 校验任务信息，只有一个任务能成功
        SiteWrapper[] expSite;
        if ( moveTask1.getSuccessCount() == 1
                && moveTask2.getSuccessCount() == 0 ) {
            expSite = new SiteWrapper[] { branchSite1 };
        } else if ( moveTask1.getSuccessCount() == 0
                && moveTask2.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { branchSite2 };
        } else {
            throw new Exception(
                    "任务信息错误，只会有一个任务成功！ taskInfo1 = " + moveTask1.toString()
                            + "  taskInfo2:" + moveTask2.toString() );
        }
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSite, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( rootSiteSession != null ) {
                rootSiteSession.close();
            }
        }
    }

    private class ThreadCreateMoveSchedule extends ResultStore {
        private SiteWrapper sourceSite;
        private SiteWrapper targetSite;
        private ScmId taskId;

        public ScmId getTaskId() {
            return taskId;
        }

        public ThreadCreateMoveSchedule( SiteWrapper sourceSite,
                SiteWrapper targetSite ) {
            this.sourceSite = sourceSite;
            this.targetSite = targetSite;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = ScmSessionUtils.createSession( sourceSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                ScmMoveTaskConfig config = new ScmMoveTaskConfig();
                config.setCondition( queryCond );
                config.setTargetSite( targetSite.getSiteName() );
                config.setWorkspace( ws );
                taskId = ScmSystem.Task.startMoveTask( config );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}