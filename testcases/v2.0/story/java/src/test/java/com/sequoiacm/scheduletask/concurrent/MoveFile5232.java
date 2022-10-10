package com.sequoiacm.scheduletask.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.ScmTransferTaskConfig;
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
import java.util.List;

/**
 * @Descreption SCM-5232:迁移并清理任务和迁移任务并发，源站点不同，目标站点相同
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5232 extends TestScmBase {
    private String fileName = "file5232_";
    private String fileAuthor = "author5232";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private ScmId fileId;
    private ScmSession sessionM = null;
    private ScmSession sessionB1 = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private ScmWorkspace wsB1;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
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
        sessionB1 = TestScmTools.createSession( branchSite1 );
        wsB1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB1 );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 从分站点1向主站点迁移并清理
        MoveFile moveFileThread = new MoveFile();
        // 从分站点2向主站点迁移
        TransferFile transferFileThread = new TransferFile();
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( moveFileThread );
        t.addWorker( transferFileThread );
        t.run();

        ScmTask moveTask = ScmSystem.Task.getTask( sessionM,
                moveFileThread.getTaskId() );
        ScmTask transferTask = ScmSystem.Task.getTask( sessionM,
                transferFileThread.getTaskId() );
        SiteWrapper[] expSite;
        if ( moveTask.getSuccessCount() == 0
                && transferTask.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { rootSite, branchSite1, branchSite2 };
        } else if ( transferTask.getSuccessCount() == 0
                && moveTask.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { rootSite, branchSite2 };
        } else if ( transferTask.getSuccessCount() == 1
                && moveTask.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { rootSite, branchSite2 };
        } else {
            throw new Exception( "moveTask:" + moveTask.toString()
                    + "  transferTask:" + transferTask.toString() );
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
                sessionB1.close();
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
            ScmSession session = TestScmTools.createSession( branchSite1 );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                ScmMoveTaskConfig config = new ScmMoveTaskConfig();
                config.setCondition( queryCond );
                config.setTargetSite( rootSite.getSiteName() );
                config.setWorkspace( ws );
                taskId = ScmSystem.Task.startMoveTask( config );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private class TransferFile {
        private ScmId taskId;

        public ScmId getTaskId() {
            return taskId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools.createSession( branchSite2 );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                ScmTransferTaskConfig config = new ScmTransferTaskConfig();
                config.setCondition( queryCond );
                config.setTargetSite( rootSite.getSiteName() );
                config.setWorkspace( ws );
                taskId = ScmSystem.Task.startTransferTask( config );
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

        // 缓存到两个分站点(兼容星状下不能分迁分)
        ScmId task = ScmSystem.Task.startTransferTask( ws, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, branchSite2.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionM, task );
        task = ScmSystem.Task.startTransferTask( ws, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, branchSite1.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionM, task );
        // 清理主站点
        task = ScmSystem.Task.startCleanTask( wsM, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT );
        ScmTaskUtils.waitTaskFinish( sessionM, task );
    }
}