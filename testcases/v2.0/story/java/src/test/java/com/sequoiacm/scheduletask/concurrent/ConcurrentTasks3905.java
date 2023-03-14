package com.sequoiacm.scheduletask.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
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
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-3905:并发执行迁移任务，不同源站点，不同目标站点，迁移不同文件
 * @author ZhangYanan
 * @createDate 2021.10.27
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
 */
public class ConcurrentTasks3905 extends TestScmBase {
    private String fileAuthor = "file3905";
    private String fileName1 = "file3905_1";
    private String fileName2 = "file3905_2";
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private SiteWrapper branchSite3;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmWorkspace rootSiteWorkspace;
    private ScmSession branchSite1Session;
    private ScmWorkspace branchSite1Workspace;
    private File localPath = null;
    private BSONObject queryCond = null;
    private int fileSizes[] = { 1024, 1024 * 2 };
    private WsWrapper wsp;
    private List< String > filePathList = new ArrayList<>();
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< ScmId > taskIdList = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSizes[ i ] + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSizes[ i ] );
            filePathList.add( filePath );
        }
        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        List< SiteWrapper > branchSitesList = ScmInfo.getBranchSites( 3 );
        branchSite1 = branchSitesList.get( 0 );
        branchSite2 = branchSitesList.get( 1 );
        branchSite3 = branchSitesList.get( 2 );

        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        branchSite1Session = ScmSessionUtils.createSession( branchSite1 );
        branchSite1Workspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        createFile( branchSite1Workspace, fileName1, filePathList.get( 0 ) );
        createFile( rootSiteWorkspace, fileName2, filePathList.get( 1 ) );
    }

    @Test(groups = { "fourSite", "net" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadFileMigration( branchSite2, branchSite1,
                fileName1 ) );
        es.addWorker(
                new ThreadFileMigration( branchSite3, rootSite, fileName2 ) );
        es.run();

        SiteWrapper[] expSites1 = { branchSite1, branchSite2 };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace,
                fileIdList.subList( 0, 1 ), expSites1 );

        SiteWrapper[] expSites2 = { rootSite, branchSite3 };
        ScmScheduleUtils.checkScmFile( rootSiteWorkspace,
                fileIdList.subList( 1, 2 ), expSites2 );

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
                if ( branchSite1Session != null ) {
                    branchSite1Session.close();
                }
            }
        }
    }

    private class ThreadFileMigration extends ResultStore {

        SiteWrapper targetSite;
        SiteWrapper sourceSite;
        String fileName;

        public ThreadFileMigration( SiteWrapper targetSite,
                SiteWrapper sourceSite, String fileName ) {
            this.targetSite = targetSite;
            this.sourceSite = sourceSite;
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void fileMigration() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( sourceSite );
                ScmWorkspace workspace = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                BSONObject queryCond = ScmQueryBuilder
                        .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                        .get();
                ScmId taskID = ScmSystem.Task.startTransferTask( workspace,
                        queryCond, ScmType.ScopeType.SCOPE_CURRENT,
                        targetSite.getSiteName() );
                ScmTaskUtils.waitTaskFinish( session, taskID );
                taskIdList.add( taskID );

            } finally {
                session.close();
            }
        }
    }

    public void createFile( ScmWorkspace workspace, String fileName,
            String filePath ) throws Exception {
        ScmFile file = ScmFactory.File.createInstance( workspace );
        file.setFileName( fileName );
        file.setAuthor( fileAuthor );
        file.setContent( filePath );
        fileIdList.add( file.save() );
    }
}