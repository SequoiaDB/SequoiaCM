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
 * @descreption SCM-3918:清理任务和跨中心读取文件并发，清理文件源站点
 * @author YiPan
 * @date 2021/10/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ConcurrentTasks3918 extends TestScmBase {
    private static final String fileName = "file3918";
    private static final int fileSize = 1024 * 1024 * 5;
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

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 主站点创建文件,缓存至分站点1
        ScmId fileId = ScmFileUtils.create( rootSiteWs, fileName, filePath );
        fileIds.add( fileId );
        ScmFactory.File.asyncCache( branchSite1Ws, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( branchSite1Ws, fileId, 2 );

        // 创建2个并发任务，分站点1清理文件，分站点2跨中心读
        ThreadExecutor t = new ThreadExecutor();
        CreateCleanTask cleanTask = new CreateCleanTask( branchSite1, wsp,
                fileName );
        t.addWorker( cleanTask );
        t.addWorker( new CacheReadFile( branchSite2, wsp, fileId ) );
        t.run();

        // 校验文件存在站点
        Assert.assertEquals( cleanTask.taskInfo.getSuccessCount(), 1,
                cleanTask.taskInfo.toString() );
        SiteWrapper[] expSite = new SiteWrapper[] { rootSite, branchSite2 };
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
            branchSite1Session.close();
        }
    }

    private class CreateCleanTask {
        private SiteWrapper targetSite;
        private WsWrapper wsp;
        private String filename;
        private BSONObject cond;
        private ScmTask taskInfo;

        public CreateCleanTask( SiteWrapper targetSite, WsWrapper wsp,
                String filename ) throws ScmException {
            this.targetSite = targetSite;
            this.wsp = wsp;
            this.filename = filename;
            cond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                    .is( this.filename ).get();
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = TestScmTools
                    .createSession( targetSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmId taskId = ScmSystem.Task.startCleanTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT );
                ScmTaskUtils.waitTaskFinish( session, taskId );
                taskInfo = ScmSystem.Task.getTask( session, taskId );
            }
        }
    }

    private class CacheReadFile {
        private SiteWrapper downloadSite;
        private WsWrapper wsp;
        private ScmId fileId;

        public CacheReadFile( SiteWrapper downloadSite, WsWrapper wsp,
                ScmId fileId ) {
            this.downloadSite = downloadSite;
            this.wsp = wsp;
            this.fileId = fileId;
        }

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            try ( ScmSession session = TestScmTools
                    .createSession( downloadSite )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.getContent( downloadPath );
            }
        }
    }
}
