package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-476:“执行清理任务”过程中读取文件 （并发在分中心A迁移文件、读取文件内容）
 * @author huangxiaoni init
 * @date 2017.6.26
 */

public class Clean_runningTaskAndReadFile476 extends TestScmBase {
    private boolean runSuccess = false;

    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "Clean476";
    private int fileSize = 1024 * 1024;
    private int fileNum = 50;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private ScmId taskId = null;
    private List< Integer > siteNumList = new ArrayList< Integer >();
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
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

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getSortSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            session = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            writeFileFromSubCenterA();
            readFileFromMainCenter();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    // bug:SEQUOIACM-194
    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            StartTaskFromSubCenterA startTask = new StartTaskFromSubCenterA();
            startTask.start();

            waitTaskFlagIsRunning();

            ReadFileFromSubCenterA readFile = new ReadFileFromSubCenterA();
            readFile.start( 5 );

            if ( !( startTask.isSuccess() && readFile.isSuccess() ) ) {
                Assert.fail( startTask.getErrorMsg() + readFile.getErrorMsg() );
            }
            ScmTaskUtils.waitTaskFinish( session, taskId );
            checkMetaAndLobs();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
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

    private void waitTaskFlagIsRunning()
            throws ScmException, InterruptedException {
        ScmSession session = null;
        try {
            // login
            session = TestScmTools.createSession( sourceSite );
            while ( true ) {
                Thread.sleep( 50 );
                if ( taskId != null ) {
                    ScmTask taskInfo = ScmSystem.Task.getTask( session,
                            taskId );
                    if ( taskInfo
                            .getRunningFlag() != CommonDefine.TaskRunningFlag.SCM_TASK_INIT ) {
                        break;
                    }
                }
            }
        } finally {
            if ( session != null )
                session.close();
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

    private void readFileFromMainCenter() throws Exception {
        ScmSession session = null;
        try {
            // login
            session = TestScmTools.createSession( targetSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );

            for ( int i = 0; i < fileNum; i++ ) {
                ScmId fileId = fileIdList.get( i );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.getContent( downloadPath );
            }
        } finally {
            if ( session != null )
                session.close();
        }
    }

    private void checkMetaAndLobs() throws Exception {
        List< Integer > siteNumList = getSiteNumList();
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String filePath = filePathList.get( i );
            int siteNum = siteNumList.get( i );
            if ( siteNum == 1 ) {
                SiteWrapper[] expSiteList = { targetSite };
                ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                        localPath, filePath );
            } else if ( siteNum == 2 ) {
                SiteWrapper[] expSiteList = { sourceSite, targetSite };
                ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                        localPath, filePath );
            }
        }
    }

    private List< Integer > getSiteNumList() {
        ScmSession session = null;
        try {
            // login
            session = TestScmTools.createSession( targetSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile file = ScmFactory.File.getInstance( ws,
                        fileIdList.get( i ) );
                int actSiteNum = file.getLocationList().size();
                siteNumList.add( actSiteNum );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
        return siteNumList;
    }

    private class StartTaskFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( sourceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );

                // start task
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR )
                        .greaterThanEquals( authorName ).get();
                taskId = ScmSystem.Task.startCleanTask( ws, condition );

                // check result
                // task runningFlag
                while ( true ) {
                    ScmTask taskInfo = ScmSystem.Task.getTask( session,
                            taskId );
                    if ( taskInfo.getStopTime() != null ) {
                        if ( taskInfo
                                .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ) {
                            Assert.assertEquals( taskInfo.getProgress(), 100 );
                        } else if ( taskInfo
                                .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ) {
                            // task detail
                            try ( Sequoiadb db = TestSdbTools
                                    .getSdb( TestScmBase.mainSdbUrl )) {
                                DBCollection clDB = db
                                        .getCollectionSpace(
                                                TestSdbTools.SCM_CS )
                                        .getCollection(
                                                TestSdbTools.SCM_CL_TASK );
                                BSONObject matcher = new BasicBSONObject();
                                matcher.put( "id", taskId.get() );
                                String detail = ( String ) clDB
                                        .query( matcher, null, null, null )
                                        .getNext().get( "detail" );
                                if ( !detail.toString()
                                        .contains( "open lob failed" ) ) {
                                    System.out.println(
                                            "---" + detail.toString() );
                                    throw new Exception( "task detail error." );
                                }
                            } catch ( BaseException e ) {
                                e.printStackTrace();
                                throw e;
                            }
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

    private class ReadFileFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( sourceSite );
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
                    } catch ( ScmException e ) {
                        if ( ScmError.DATA_NOT_EXIST != e.getError()
                                && ScmError.FILE_NOT_FOUND != e.getError()
                                && ScmError.DATA_CORRUPTED != e.getError() ) {
                            e.printStackTrace();
                            throw e;
                        }
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