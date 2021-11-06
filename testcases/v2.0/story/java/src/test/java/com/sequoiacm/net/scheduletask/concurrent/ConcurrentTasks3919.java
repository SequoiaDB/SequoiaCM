package com.sequoiacm.net.scheduletask.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-3919:清理任务和更新操作并发
 * @author ZhangYanan
 * @createDate 2021.10.27
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
 */
public class ConcurrentTasks3919 extends TestScmBase {
    private String fileName = "file3919";
    private String fileName1 = "file3919_1";
    private String filePath = null;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private SiteWrapper branchSite3;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmWorkspace rootSiteWorkspace = null;
    private ScmWorkspace branchSiteWorkspace1 = null;
    private ScmSession branchSiteSession1;
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
        List< SiteWrapper > branchSitesList = ScmInfo.getBranchSites(3);
        branchSite1 = branchSitesList.get( 0 );
        branchSite2 = branchSitesList.get( 1 );
        branchSite3 = branchSitesList.get( 2 );

        rootSiteSession = TestScmTools.createSession( rootSite );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        branchSiteSession1 = TestScmTools.createSession( branchSite1 );
        branchSiteWorkspace1 = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession1 );

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
        es.addWorker( new ThreadCleanFile() );
        es.addWorker( new ThreadUpdateFile() );
        es.run();

        ScmTaskUtils.waitTaskFinish( branchSiteSession1, taskId );
        ScmTask task = ScmSystem.Task.getTask( branchSiteSession1, taskId );
        SiteWrapper[] expSites = null;
        if ( task.getSuccessCount() == 1 ) {
            expSites = new SiteWrapper[] { rootSite };
        } else {
            expSites = new SiteWrapper[] { rootSite, branchSite1 };
        }
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIdList,
                expSites );
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
                if ( branchSiteSession1 != null ) {
                    branchSiteSession1.close();
                }
            }
        }
    }

    private class ThreadCleanFile extends ResultStore {
        @ExecuteOrder(step = 1)
        private void cleanFile() throws Exception {
            taskId = ScmSystem.Task.startCleanTask( branchSiteWorkspace1,
                    queryCond );
        }
    }

    private class ThreadUpdateFile extends ResultStore {

        @ExecuteOrder(step = 1)
        private void updateFile() throws Exception {
            try {
                ScmFile file = ScmFactory.File.getInstance( rootSiteWorkspace,
                        fileIdList.get( 0 ) );
                file.setFileName( fileName1 );
                Assert.assertEquals( file.getFileName(), fileName1 );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.DATA_WRITE_ERROR
                        .getErrorCode() ) {
                    throw e;
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