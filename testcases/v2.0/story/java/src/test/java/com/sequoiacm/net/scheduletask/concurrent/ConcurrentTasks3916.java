package com.sequoiacm.net.scheduletask.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-3916:迁移任务和跨中心读取文件并发，源站点不同，目标站点相同
 * @author ZhangYanan
 * @createDate 2021.10.27
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
 */
public class ConcurrentTasks3916 extends TestScmBase {
    private String fileName = "file3916";
    private String filePath = null;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private SiteWrapper branchSite3;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmWorkspace rootSiteWorkspace = null;

    private File localPath = null;
    private BSONObject queryCond = null;
    private int fileSizes = 100 * 1024;
    private WsWrapper wsp;
    private List< ScmId > fileIdList = new ArrayList<>();
    private ScmId taskId;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSizes
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSizes );

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > branchSitesList = ScmInfo.getBranchSites( 3 );
        branchSite1 = branchSitesList.get( 0 );
        branchSite2 = branchSitesList.get( 1 );
        branchSite3 = branchSitesList.get( 2 );

        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        ScmId fileId = ScmFileUtils.create( rootSiteWorkspace, fileName,
                filePath );
        fileIdList.add( fileId );

        asyncTransfer( branchSite1 );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        ThreadFileMigration fileMigration = new ThreadFileMigration(
                branchSite2 );
        ThreadReadFile readFile = new ThreadReadFile( branchSite2 );
        es.addWorker( fileMigration );
        es.addWorker( readFile );
        es.run();

        ScmTaskUtils.waitTaskFinish( rootSiteSession, taskId );
        SiteWrapper[] expSites1 = { rootSite, branchSite1, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIdList,
                expSites1 );

        long successCountSum = 0;
        ScmTask task = ScmSystem.Task.getTask( rootSiteSession, taskId );
        successCountSum = task.getSuccessCount();
        Assert.assertEquals( successCountSum, 1 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            } finally {
                if ( rootSiteSession != null ) {
                    rootSiteSession.close();
                }
            }
        }
    }

    private class ThreadFileMigration extends ResultStore {
        SiteWrapper branchSite;

        public ThreadFileMigration( SiteWrapper branchSite ) {
            this.branchSite = branchSite;
        }

        @ExecuteOrder(step = 1)
        private void fileMigration() throws Exception {
            taskId = ScmSystem.Task.startTransferTask( rootSiteWorkspace,
                    queryCond, ScmType.ScopeType.SCOPE_CURRENT,
                    branchSite.getSiteName() );
        }
    }

    private class ThreadReadFile extends ResultStore {
        SiteWrapper branchSite;

        public ThreadReadFile( SiteWrapper branchSite ) {
            this.branchSite = branchSite;
        }

        @ExecuteOrder(step = 1)
        private void readFile() throws Exception {
            ScmSession session = null;
            OutputStream os = null;
            try {
                session = TestScmTools.createSession( branchSite );
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( workspace,
                        fileIdList.get( 0 ) );
                os = new FileOutputStream( filePath );
                // 下载文件内容，指定跨站点读
                file.getContent( os );
            } finally {
                session.close();
                if ( os != null ) {
                    os.close();
                }
            }
        }
    }

    public void asyncTransfer( SiteWrapper branchSite ) throws Exception {
        ScmFactory.File.asyncTransfer( rootSiteWorkspace, fileIdList.get( 0 ),
                branchSite.getSiteName() );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWorkspace,
                fileIdList.get( 0 ), 2 );
    }
}