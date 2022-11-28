package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

/**
 * @Descreption SCM-5235:并发执行多个迁移并清理任务，源站点相同，开启空间回收
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5235 extends TestScmBase {
    private String fileName = "file5235_";
    private String fileAuthor = "author5235";
    private String wsName = "ws_5235";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmSession sessionM = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int recycleCSNum = 10;
    private boolean runSuccess = false;

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
        branchSite = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        prepare();
    }

    // 问题单SEQUOIACM-1159影响，用例屏蔽
    @Test(groups = { "twoSite", "fourSite" }, enabled = false)
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        MoveFileThread thread1 = new MoveFileThread();
        MoveFileThread thread2 = new MoveFileThread();
        t.addWorker( thread1 );
        t.addWorker( thread2 );
        t.run();
        // 统计两个线程清理的CS
        ScmTask task1 = ScmSystem.Task.getTask( sessionM, thread1.getTaskId() );
        ScmTask task2 = ScmSystem.Task.getTask( sessionM, thread2.getTaskId() );
        Object[] cleanCS1 = ScmScheduleUtils.getTaskExtraInfo( task1 );
        Object[] cleanCS2 = ScmScheduleUtils.getTaskExtraInfo( task2 );
        List< Object > cleanAllCS = new ArrayList<>();
        cleanAllCS.addAll( Arrays.asList( cleanCS1 ) );
        cleanAllCS.addAll( Arrays.asList( cleanCS2 ) );
        Set< String > expCleanCS = ScmScheduleUtils.initLobCSName( wsName,
                ScmShardingType.YEAR, System.currentTimeMillis(),
                recycleCSNum );
        Assert.assertEqualsNoOrder( cleanAllCS.toArray(), expCleanCS.toArray(),
                "act:" + cleanAllCS + " exp:" + expCleanCS );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsName, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmWorkspaceUtil.deleteWs( wsName, sessionM );
            } finally {
                sessionM.close();
            }
        }
    }

    private void prepare() throws Exception {
        ScmScheduleUtils.cleanNullCS( sessionM, wsName );
        Calendar instance = Calendar.getInstance();
        ScmScheduleUtils.checkMonthChange( instance );
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            file.setCreateTime( instance.getTime() );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + i );
            fileIds.add( file.save() );
            instance.add( Calendar.YEAR, -1 );
        }
    }

    private class MoveFileThread {
        private ScmId taskId;

        public ScmId getTaskId() {
            return taskId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                ScmMoveTaskConfig conf = new ScmMoveTaskConfig();
                conf.setWorkspace( ws );
                conf.setTargetSite( branchSite.getSiteName() );
                conf.setCondition( queryCond );
                conf.setRecycleSpace( true );
                taskId = ScmSystem.Task.startMoveTask( conf );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }
}