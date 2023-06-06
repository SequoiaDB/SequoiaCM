package com.sequoiacm.scheduletask.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-3903:并发执行迁移任务，不同源站点，相同目标站点，迁移相同文件
 * @author ZhangYanan
 * @createDate 2021.10.27
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
 */
public class ConcurrentTasks3903 extends TestScmBase {
    private String fileName = "file3903";
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmWorkspace rootSiteWorkspace;
    private File localPath = null;
    private BSONObject queryCond = null;
    private int fileSizes = 1024;
    private WsWrapper wsp;
    private ScmId cleanTaskId;
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< ScmId > taskIdList = Collections
            .synchronizedList( new ArrayList< ScmId >() );
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
        List< SiteWrapper > branchSitesList = ScmInfo.getBranchSites( 2 );
        branchSite1 = branchSitesList.get( 0 );
        branchSite2 = branchSitesList.get( 1 );

        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        ScmId fileId = ScmFileUtils.create( rootSiteWorkspace, fileName,
                filePath );
        fileIdList.add( fileId );

        // 缓存文件到branchSite1和branchSite2
        asyncCache();

        cleanTaskId = ScmSystem.Task.startCleanTask( rootSiteWorkspace,
                queryCond );
        ScmTaskUtils.waitTaskFinish( rootSiteSession, cleanTaskId );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadFileMigration( branchSite1 ) );
        es.addWorker( new ThreadFileMigration( branchSite2 ) );
        es.run();

        SiteWrapper[] expSites1 = { rootSite, branchSite1, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace, fileIdList,
                expSites1 );

        int successCountSum = 0;
        for ( int i = 0; i < taskIdList.size(); i++ ) {
            ScmTask task = ScmSystem.Task.getTask( rootSiteSession,
                    taskIdList.get( i ) );
            successCountSum += task.getSuccessCount();
        }
        Assert.assertEquals( successCountSum, 1 );

        taskIdList.add( cleanTaskId );
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
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( branchSite );
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmId taskID = ScmSystem.Task.startTransferTask( workspace,
                        queryCond, ScmType.ScopeType.SCOPE_CURRENT,
                        rootSite.getSiteName() );
                ScmTaskUtils.waitTaskFinish( session, taskID );
                taskIdList.add( taskID );
            } finally {
                session.close();
            }
        }
    }

    public void asyncCache() throws Exception {
        ScmFactory.File.asyncTransfer( rootSiteWorkspace, fileIdList.get( 0 ),
                branchSite1.getSiteName() );
        ScmFactory.File.asyncTransfer( rootSiteWorkspace, fileIdList.get( 0 ),
                branchSite2.getSiteName() );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWorkspace,
                fileIdList.get( 0 ), 3 );
    }
}