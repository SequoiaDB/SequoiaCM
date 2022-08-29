package com.sequoiacm.task.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-3906:并发执行迁移和清理任务，迁移源站点和清理站点相同，操作相同文件
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3906 extends TestScmBase {
    private static final String fileName = "file3906";
    private static final int fileSize = 1024 * 1024 * 50;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private ScmSession branchSite1Session = null;
    private ScmWorkspace branchSite1Ws = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private BSONObject queryCond = null;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
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

        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSite1Session = TestScmTools.createSession( branchSite1 );
        branchSite1Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 分站点1创建文件缓存至主站点
        ScmId fileId = ScmFileUtils.create( branchSite1Ws, fileName, filePath );
        fileIds.add( fileId );
        ScmFactory.File.asyncCache( rootSiteWs, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId, 2 );

        // 创建2个并发任务，主站点清理文件，主站点迁移文件至分站点2
        CreateCleanTask cleanTask = new CreateCleanTask( rootSite, wsp,
                fileName );
        CreateTransferTask transTask = new CreateTransferTask( rootSite,
                branchSite2, wsp, fileName );
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( cleanTask );
        t.addWorker( transTask );
        t.run();

        // 校验文件元数据
        Assert.assertEquals( cleanTask.taskInfo.getSuccessCount(), 1,
                cleanTask.taskInfo.toString() );
        if ( transTask.taskInfo.getSuccessCount() == 1 ) {
            SiteWrapper[] expSite = { branchSite1, branchSite2 };
            ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSite );
        } else if ( transTask.taskInfo.getSuccessCount() == 0 ) {
            SiteWrapper[] expSite = { branchSite1 };
            ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSite );
        } else {
            Assert.fail( "task successCount must equals '0' or '-1', taskInfo="
                    + transTask.taskInfo.toString() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
            branchSite1Session.close();
        }
    }

    private class CreateCleanTask {
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String filename;
        private ScmTask taskInfo;

        public CreateCleanTask( SiteWrapper targetSite, WsWrapper wsp,
                String filename ) {
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.filename = filename;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools
                    .createSession( targetSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject query = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( this.filename ).get();
                ScmId taskId = ScmSystem.Task.startCleanTask( ws, query,
                        ScmType.ScopeType.SCOPE_CURRENT );
                ScmTaskUtils.waitTaskStop( session, taskId );
                this.taskInfo = ScmSystem.Task.getTask( session, taskId );
            }
        }
    }

    private class CreateTransferTask {
        private SiteWrapper sourceSite;
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String filename;
        private ScmTask taskInfo;

        public CreateTransferTask( SiteWrapper sourceSite,
                SiteWrapper targetSite, WsWrapper wsp, String filename ) {
            this.sourceSite = sourceSite;
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.filename = filename;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools
                    .createSession( sourceSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject query = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME )
                        .is( this.filename ).get();
                ScmId taskId = ScmSystem.Task.startTransferTask( ws, query,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        targetSite.getSiteName() );
                ScmTaskUtils.waitTaskStop( session, taskId );
                this.taskInfo = ScmSystem.Task.getTask( session, taskId );
            }
        }
    }
}