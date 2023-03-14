package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.element.ScmTask;
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
 * @Descreption SCM-5236:开启了空间回收的迁移并清理任务和上传文件操作并发
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5236 extends TestScmBase {
    private String fileName = "file5236_";
    private String fileAuthor = "author5236";
    private String titleA = "titleA5236";
    private String titleB = "titleB5236";
    private String wsName = "ws_5236";
    private long now;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
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
        branchSite = ScmInfo.getBranchSite();
        sessionM = ScmSessionUtils.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        long year = 365L * 24L * 60L * 60L * 1000L;
        long fileACreate_time = now - year;
        long fileBCreate_time = now - year * 2;
        MoveFileThread moveFileA = new MoveFileThread( titleA );
        CreateFileThread createFileA = new CreateFileThread( titleA,
                fileACreate_time );
        CreateFileThread createFileB = new CreateFileThread( titleB,
                fileBCreate_time );
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( moveFileA );
        t.addWorker( createFileA );
        t.addWorker( createFileB );
        t.run();

        // 文件B不受影响
        SiteWrapper[] expSite = { rootSite };
        ScmFileUtils.checkMetaAndData( wsName, createFileB.getFileId(), expSite,
                localPath, filePath );

        // 获取任务执行结果
        ScmTask task = ScmSystem.Task.getTask( sessionM,
                moveFileA.getTaskId() );
        Object[] taskExtraInfo = ScmScheduleUtils.getTaskExtraInfo( task );
        List< Object > cleanCS = Arrays.asList( taskExtraInfo );
        // 生成A、B文件所在的LobCS
        String fileALobCS = ScmScheduleUtils.initCSNameByTimestamp( wsName,
                ScmShardingType.YEAR, fileACreate_time );
        String fileBLobCS = ScmScheduleUtils.initCSNameByTimestamp( wsName,
                ScmShardingType.YEAR, fileBCreate_time );
        // 构造预期结果排除B所在LobCS
        Set< String > expCleanCSName = ScmScheduleUtils.initLobCSName( wsName,
                ScmShardingType.YEAR, now, recycleCSNum );
        expCleanCSName.remove( fileBLobCS );
        // 判断文件A所在的站点，生成预期结果
        if ( cleanCS.contains( fileALobCS ) ) {
            expSite = new SiteWrapper[] { branchSite };
        } else {
            expSite = new SiteWrapper[] { rootSite };
            expCleanCSName.remove( fileALobCS );
        }
        // 根据判断结果校验文件A所在的站点,校验回收的空间
        ScmFileUtils.checkMetaAndData( wsName, createFileA.getFileId(), expSite,
                localPath, filePath );
        Assert.assertEqualsNoOrder( taskExtraInfo, expCleanCSName.toArray(),
                "act:" + Arrays.toString( taskExtraInfo ) + ",exp:"
                        + expCleanCSName );

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsName, queryCond );
                ScmWorkspaceUtil.deleteWs( wsName, sessionM );
            } finally {
                sessionM.close();
            }
        }
    }

    private void prepare() throws Exception {
        ScmScheduleUtils.cleanNullCS( sessionM, wsName );
        long year = 365L * 24L * 60L * 60L * 1000L;
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            Date date = new Date( now - year * i );
            file.setCreateTime( date );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + i );
            file.setTitle( titleA );
            file.setContent( filePath );
            file.save();
        }
    }

    private class MoveFileThread {
        private String title;
        private ScmId taskId;

        public MoveFileThread( String title ) {
            this.title = title;
        }

        public ScmId getTaskId() {
            return taskId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = ScmSessionUtils.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.TITLE ).is( title ).get();
                ScmMoveTaskConfig conf = new ScmMoveTaskConfig();
                conf.setWorkspace( ws );
                conf.setTargetSite( branchSite.getSiteName() );
                conf.setCondition( cond );
                conf.setRecycleSpace( true );
                taskId = ScmSystem.Task.startMoveTask( conf );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private class CreateFileThread {
        private String title;
        private ScmId fileId;
        private long create_time;

        public CreateFileThread( String title, long create_time ) {
            this.title = title;
            this.create_time = create_time;
        }

        public ScmId getFileId() {
            return fileId;
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
                file.setTitle( title );
                file.setFileName( fileName + UUID.randomUUID() );
                file.setContent( filePath );
                fileId = file.save();
            } finally {
                session.close();
            }
        }
    }
}