package com.sequoiacm.scheduletask.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ResultStore;
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
 * @description SCM-3902:并发执行迁移任务，相同源站点，不同目标站点，迁移相同文件
 * @author ZhangYanan
 * @createDate 2021.10.27
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
 */
public class ConcurrentTasks3902 extends TestScmBase {
    private String fileName = "file3902";
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
    private List< ScmId > taskIdList = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        String filePath = localPath + File.separator + "localFile_" + fileSizes
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSizes );

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > branchSitesList = ScmInfo.getBranchSites( 3 );
        branchSite1 = branchSitesList.get( 0 );
        branchSite2 = branchSitesList.get( 1 );
        branchSite3 = branchSitesList.get( 2 );

        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        ScmId fileId = ScmFileUtils.create( rootSiteWorkspace, fileName,
                filePath );
        fileIdList.add( fileId );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadFileMigration( branchSite1 ) );
        es.addWorker( new ThreadFileMigration( branchSite2 ) );
        es.addWorker( new ThreadFileMigration( branchSite3 ) );
        es.run();

        SiteWrapper[] expSites1 = { rootSite, branchSite1, branchSite2,
                branchSite3 };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIdList,
                expSites1 );
        for ( int i = 0; i < taskIdList.size(); i++ ) {
            ScmTask task = ScmSystem.Task.getTask( rootSiteSession,
                    taskIdList.get( i ) );
            long successCountSum1 = task.getSuccessCount();
            Assert.assertEquals( successCountSum1, 1 );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId taskID : taskIdList ) {
                    TestSdbTools.Task.deleteMeta( taskID );
                }
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
            ScmId taskID = ScmSystem.Task.startTransferTask( rootSiteWorkspace,
                    queryCond, ScmType.ScopeType.SCOPE_CURRENT,
                    branchSite.getSiteName() );
            ScmTaskUtils.waitTaskFinish( rootSiteSession, taskID );
            taskIdList.add( taskID );
        }
    }
}