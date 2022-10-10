package com.sequoiacm.scheduletask.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5233:迁移并清理任务和清理任务并发，清理任务站点和迁移并清理任务源站点相同
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5233 extends TestScmBase {
    private String fileName = "file5233";
    private String fileAuthor = "author5233";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private ScmId fileId;
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 10;
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

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();

        List< SiteWrapper > branchSites = ScmInfo.getBranchSites( 2 );
        branchSite1 = branchSites.get( 0 );
        branchSite2 = branchSites.get( 1 );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 从主站点向分站点2迁移并清理
        MoveFile moveFileThread = new MoveFile();
        // 清理主站点
        CleanFile cleanFileThread = new CleanFile();
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( moveFileThread );
        t.addWorker( cleanFileThread );
        t.run();

        ScmTask moveTask = ScmSystem.Task.getTask( sessionM,
                moveFileThread.getTaskId() );
        ScmTask cleanTask = ScmSystem.Task.getTask( sessionM,
                cleanFileThread.getTaskId() );
        SiteWrapper[] expSite;
        if ( moveTask.getSuccessCount() == 0
                && cleanTask.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { branchSite1 };
        } else if ( cleanTask.getSuccessCount() == 0
                && moveTask.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { branchSite1, branchSite2 };
        } else {
            throw new Exception( "moveTask:" + moveTask.toString()
                    + "  transferTask:" + cleanTask.toString() );
        }
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSite, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
            } finally {
                sessionM.close();
            }
        }
    }

    private class MoveFile {
        private ScmId taskId;

        public ScmId getTaskId() {
            return taskId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                ScmMoveTaskConfig config = new ScmMoveTaskConfig();
                config.setCondition( queryCond );
                config.setTargetSite( branchSite2.getSiteName() );
                config.setWorkspace( ws );
                taskId = ScmSystem.Task.startMoveTask( config );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private class CleanFile {
        private ScmId taskId;

        public ScmId getTaskId() {
            return taskId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                taskId = ScmSystem.Task.startCleanTask( ws, queryCond );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileAuthor );
        file.setContent( filePath );
        fileId = file.save();
        ScmId task = ScmSystem.Task.startTransferTask( ws, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, branchSite1.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionM, task );
    }
}