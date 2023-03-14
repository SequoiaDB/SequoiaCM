package com.sequoiacm.task.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-430:“执行迁移任务”过程中读取文件内容 （并发：在分中心A迁移文件、在分中心B读取文件内容）
 * @author huangxiaoni init
 * @date 2017.6.14
 */

public class Transfer_runningTaskAndReadFileB430 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( Transfer_runningTaskAndReadFileB430.class );
    private boolean runSuccess = false;

    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "transfer430B";
    private int fileSize = 1024 * 1024;
    private int fileNum = 20;
    private ScmId taskId = null;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            for ( int i = 0; i < fileNum; i++ ) {
                String filePath = localPath + File.separator + "localFile_"
                        + fileSize + i + ".txt";
                TestTools.LocalFile.createFile( filePath, fileSize + i );
                filePathList.add( filePath );
            }

            rootSite = ScmInfo.getRootSite();
            branceSiteList = ScmInfo.getBranchSites( 2 );
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            session = ScmSessionUtils.createSession( branceSiteList.get( 0 ) );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );
            writeFileFromSubCenterA();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite", GroupTags.base })
    private void test() throws Exception {
        StartTaskFromSubCenterA startTask = new StartTaskFromSubCenterA();
        startTask.start();

        ReadFileFromSubCenterB readFile = new ReadFileFromSubCenterB();
        readFile.start( 10 );

        if ( !( startTask.isSuccess() && readFile.isSuccess() ) ) {
            Assert.fail( startTask.getErrorMsg() + readFile.getErrorMsg() );
        }

        ScmTaskUtils.waitTaskFinish( session, taskId );

        checkMetaAndLobs();

        // check task info
        ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
        logger.info( "taskInfo \n" + taskInfo );
        Assert.assertEquals( taskInfo.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
        Assert.assertEquals( taskInfo.getProgress(), 100 );
        Assert.assertEquals( taskInfo.getEstimateCount(), fileNum );
        // Assert.assertEquals(taskInfo.getActualCount(), fileNum); //unfixed
        // value
        Assert.assertEquals( taskInfo.getFailCount(),
                taskInfo.getActualCount() - taskInfo.getSuccessCount() );
        Assert.assertEquals( taskInfo.getSuccessCount(),
                taskInfo.getActualCount() - taskInfo.getFailCount() );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void writeFileFromSubCenterA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePathList.get( i ) );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void checkMetaAndLobs() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                String filePath = filePathList.get( i );
                SiteWrapper[] expSiteList = { rootSite, branceSiteList.get( 0 ),
                        branceSiteList.get( 1 ) };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList.get( i ),
                        expSiteList, localPath, filePath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private class StartTaskFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branceSiteList.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );

                // start task
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                        .get();
                taskId = ScmSystem.Task.startTransferTask( ws, condition,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        rootSite.getSiteName() );
                // check result
                // task runningFlag
                while ( true ) {
                    ScmTask taskInfo = ScmSystem.Task.getTask( session,
                            taskId );
                    if ( taskInfo.getStopTime() != null ) {
                        if ( taskInfo
                                .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ) {
                            break;
                        } else if ( taskInfo
                                .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ) {
                            logger.error(
                                    "taskInfo: \n" + taskInfo.toString() );
                            throw new Exception( "task is abort." );
                        }
                        break;
                    }
                }
            } finally {
                if ( session != null )
                    session.close();
            }
        }
    }

    private class ReadFileFromSubCenterB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branceSiteList.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );

                for ( int i = 0; i < fileNum; i++ ) {
                    OutputStream fos = null;
                    ScmInputStream sis = null;
                    ScmId fileId = fileIdList.get( i );

                    try {
                        ScmFile file = ScmFactory.File.getInstance( ws,
                                fileId );
                        String downloadPath = TestTools.LocalFile
                                .initDownloadPath( localPath,
                                        TestTools.getMethodName(),
                                        Thread.currentThread().getId() );
                        fos = new FileOutputStream( new File( downloadPath ) );
                        sis = ScmFactory.File.createInputStream( file );
                        sis.read( fos );

                        // check content
                        Assert.assertEquals(
                                TestTools.getMD5( filePathList.get( i ) ),
                                TestTools.getMD5( downloadPath ) );
                    } finally {
                        if ( fos != null )
                            fos.close();
                        if ( sis != null )
                            sis.close();
                    }
                }
            } finally {
                if ( session != null )
                    session.close();
            }
        }
    }
}