package com.sequoiacm.task.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
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
 * @descreption SCM-3907:并发执行迁移和清理任务，迁移目标站点和清理站点相同，操作相同文件
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3907 extends TestScmBase {
    private static final String fileName = "file3907";
    private static final int fileSize = 1024 * 1024 * 50;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private BSONObject queryCond = null;
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
        branchSite = ScmInfo.getBranchSite();

        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 主站点创建文件
        ScmId fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath );
        fileIds.add( fileId );

        // 创建2个并发任务，分站点清理文件，主站点迁移文件至分站点1
        CreateCleanTask cleanTask = new CreateCleanTask( branchSite, wsp,
                fileName );
        CreateTransferTask transTask = new CreateTransferTask( rootSite,
                branchSite, wsp, fileName );
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( cleanTask );
        t.addWorker( transTask );
        t.run();

        // 校验文件元数据
        SiteWrapper[] expSite;
        if ( cleanTask.taskInfo.getSuccessCount() == 1
                && transTask.taskInfo.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { rootSite };
        } else if ( cleanTask.taskInfo.getSuccessCount() == 0
                && transTask.taskInfo.getSuccessCount() == 1 ) {
            expSite = new SiteWrapper[] { rootSite, branchSite };
        } else {
            throw new Exception( "clean task successCount: "
                    + cleanTask.taskInfo.getSuccessCount()
                    + ",trans task successCount:"
                    + transTask.taskInfo.getSuccessCount() );
        }
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIds, expSite );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            rootSiteSession.close();
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