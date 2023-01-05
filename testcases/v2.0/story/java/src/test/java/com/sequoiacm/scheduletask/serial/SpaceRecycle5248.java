package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmSpaceRecyclingTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.Sequoiadb;
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
 * @Descreption SCM-5248:多个空间回收任务并发执行
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class SpaceRecycle5248 extends TestScmBase {
    private String fileName = "file5248_";
    private String fileAuthor = "author5248";
    private String wsName = "ws_5248";
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int recycleCSNum = 10;
    private long now;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        rootSite = ScmInfo.getRootSite();
        sessionM = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        prepare();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        CleanCSThread thread1 = new CleanCSThread();
        CleanCSThread thread2 = new CleanCSThread();
        CleanCSThread thread3 = new CleanCSThread();
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( thread1 );
        t.addWorker( thread2 );
        t.addWorker( thread3 );
        t.run();

        // 校验统计信息
        Set< String > expCleanCS = ScmScheduleUtils.initLobCSName( wsName,
                ScmShardingType.YEAR, now, recycleCSNum );
        ScmTask task1 = ScmSystem.Task.getTask( sessionM, thread1.getTaskId() );
        ScmTask task2 = ScmSystem.Task.getTask( sessionM, thread2.getTaskId() );
        ScmTask task3 = ScmSystem.Task.getTask( sessionM, thread3.getTaskId() );
        Object[] cleanCS1 = ScmScheduleUtils.getTaskExtraInfo( task1 );
        Object[] cleanCS2 = ScmScheduleUtils.getTaskExtraInfo( task2 );
        Object[] cleanCS3 = ScmScheduleUtils.getTaskExtraInfo( task3 );
        List< Object > actCleanCS = new ArrayList<>();
        actCleanCS.addAll( Arrays.asList( cleanCS1 ) );
        actCleanCS.addAll( Arrays.asList( cleanCS2 ) );
        actCleanCS.addAll( Arrays.asList( cleanCS3 ) );
        Assert.assertEqualsNoOrder( actCleanCS.toArray(), expCleanCS.toArray(),
                "act:" + actCleanCS + " exp:" + expCleanCS );
        // 直连数据源校验
        checkLobCS( expCleanCS );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmWorkspaceUtil.deleteWs( wsName, sessionM );
            } finally {
                sessionM.close();
            }
        }
    }

    // 构造空的cs
    private void prepare() throws ScmException, InterruptedException {
        Calendar instance = Calendar.getInstance();
        ScmScheduleUtils.checkMonthChange( instance );
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            file.setCreateTime( instance.getTime() );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            file.setFileName( fileName + i );
            file.save();
            instance.add( Calendar.YEAR, -1 );
        }
        ScmFileUtils.cleanFile( wsName, queryCond );
    }

    private class CleanCSThread {
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
                ScmSpaceRecyclingTaskConfig conf = new ScmSpaceRecyclingTaskConfig();
                conf.setWorkspace( ws );
                conf.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 0 ) );
                taskId = ScmSystem.Task.startSpaceRecyclingTask( conf );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private void checkLobCS( Set< String > csNames ) {
        Sequoiadb sdb = new Sequoiadb( TestScmBase.mainSdbUrl,
                TestScmBase.sdbUserName, TestScmBase.sdbPassword );
        try {
            for ( String csName : csNames ) {
                Assert.assertFalse( sdb.isCollectionSpaceExist( csName ) );
            }
        } finally {
            sdb.close();
        }
    }
}