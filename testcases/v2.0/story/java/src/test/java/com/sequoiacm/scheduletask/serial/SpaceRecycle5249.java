package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmSpaceRecyclingTaskConfig;
import com.sequoiacm.client.exception.ScmException;
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
 * @Descreption SCM-5249:空间回收任务和上传文件并发执行
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class SpaceRecycle5249 extends TestScmBase {
    private String fileName = "file5249_";
    private String fileAuthor = "author5249";
    private String wsName = "ws_5249";
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM;
    private ScmId fileId;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private long now;
    private long year = 365L * 24L * 60L * 60L * 1000L;
    private File localPath = null;
    private String filePath = null;
    private int recycleCSNum = 10;
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
        sessionM = ScmSessionUtils.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsName, queryCond );
        prepare();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        CleanCSThread cleanCSThread = new CleanCSThread();
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( cleanCSThread );
        t.addWorker( new CreateFileThread( now - year * 2 ) );
        t.run();

        // 校验结果
        String csName = ScmScheduleUtils.initCSNameByTimestamp( wsName,
                ScmShardingType.YEAR, now - year * 2 );
        Set< String > expCleanCSNames = ScmScheduleUtils.initLobCSName( wsName,
                ScmShardingType.YEAR, now, recycleCSNum );
        Object[] taskExtraInfo = ScmScheduleUtils.getTaskExtraInfo(
                ScmSystem.Task.getTask( sessionM, cleanCSThread.getTaskId() ) );
        if ( expCleanCSNames.size() != taskExtraInfo.length ) {
            expCleanCSNames.remove( csName );
        }
        Assert.assertEqualsNoOrder( taskExtraInfo, expCleanCSNames.toArray(),
                "act:" + Arrays.toString( taskExtraInfo ) + ",exp:"
                        + expCleanCSNames );
        SiteWrapper[] expSites = { rootSite };
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                filePath );
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
    private void prepare() throws Exception {
        Calendar instance = Calendar.getInstance();
        ScmScheduleUtils.checkMonthChange( instance );
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            file.setCreateTime( instance.getTime() );
            file.setAuthor( fileAuthor );
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
            ScmSession session = ScmSessionUtils.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                ScmSpaceRecyclingTaskConfig conf = new ScmSpaceRecyclingTaskConfig();
                conf.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 0 ) );
                conf.setWorkspace( ws );
                taskId = ScmSystem.Task.startSpaceRecyclingTask( conf );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private class CreateFileThread {
        private long create_time;

        public CreateFileThread( long create_time ) {
            this.create_time = create_time;
        }

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setCreateTime( new Date( create_time ) );
                file.setAuthor( fileAuthor );
                file.setFileName( fileName + UUID.randomUUID() );
                file.setContent( filePath );
                fileId = file.save();
            } finally {
                session.close();
            }
        }
    }
}