package com.sequoiacm.scheduletask.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.ScmTransferTaskConfig;
import com.sequoiacm.client.exception.ScmException;
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
import java.io.IOException;

/**
 * @Descreption SCM-5230:迁移并清理任务和迁移任务并发，源站点和目标站点相同
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5230 extends TestScmBase {
    private String fileName = "file5230_";
    private String fileAuthor = "author5230";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
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
    public void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        MoveFile moveFileThread = new MoveFile();
        TransferFile transferFileThread = new TransferFile();
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
            expSite = new SiteWrapper[] { rootSite, branchSite };
        } else if ( transferTask.getSuccessCount() == 0
                && moveTask.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { branchSite };
        } else if ( transferTask.getSuccessCount() == 1
                && moveTask.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { branchSite };
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
                config.setTargetSite( branchSite.getSiteName() );
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
            ScmSession session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            try {
                ScmTransferTaskConfig config = new ScmTransferTaskConfig();
                config.setCondition( queryCond );
                config.setTargetSite( branchSite.getSiteName() );
                config.setWorkspace( ws );
                taskId = ScmSystem.Task.startTransferTask( config );
                ScmTaskUtils.waitTaskFinish( session, taskId );
            } finally {
                session.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileAuthor );
        file.setContent( filePath );
        fileId = file.save();
    }
}