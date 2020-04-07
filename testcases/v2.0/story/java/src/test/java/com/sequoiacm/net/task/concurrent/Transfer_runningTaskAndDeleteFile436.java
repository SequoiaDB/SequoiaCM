package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-436:“执行迁移任务”过程中删除文件
 * @author huangxiaoni init
 * @date 2017.6.14
 */

public class Transfer_runningTaskAndDeleteFile436 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private List< Integer > siteNumList = new ArrayList< Integer >();
    private WsWrapper ws_T = null;

    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "transfer436";
    private int fileSize = 1024 * 1024;
    private int fileNum = 20;
    private ScmId taskId = null;
    private File localPath = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String filePath = "";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            session = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );
            readyFile();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            StartTask startTask = new StartTask();
            startTask.start();

            DeleteFile deleteFile = new DeleteFile();
            deleteFile.start();

            if ( !( startTask.isSuccess() && deleteFile.isSuccess() ) ) {
                Assert.fail(
                        startTask.getErrorMsg() + deleteFile.getErrorMsg() );
            }

            waitTaskStop();
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
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
                for ( ScmId fileId : fileIdList ) {
                    try {
                        ScmFactory.File.deleteInstance( ws, fileId, true );
                    } catch ( ScmException e ) {
                        if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                            throw e;
                        }
                    }
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void readyFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void checkMetaAndLobs() throws Exception {
        List< Integer > siteNumList = getSiteNumList();
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            int siteNum = siteNumList.get( i );
            try {
                if ( siteNum == 1 ) {
                    SiteWrapper[] expSiteList = { sourceSite };
                    ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                            localPath, filePath );
                } else if ( siteNum == 2 ) {
                    SiteWrapper[] expSiteList = { sourceSite, targetSite };
                    ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                            localPath, filePath );
                }
            } catch ( Exception e ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    private List< Integer > getSiteNumList() throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), session );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file;
            try {
                file = ScmFactory.File.getInstance( ws, fileIdList.get( i ) );
                int actSiteNum = file.getLocationList().size();
                siteNumList.add( actSiteNum );
            } catch ( ScmException e ) {
                siteNumList.add( 0 );
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                        e.getMessage() );
            }
        }
        return siteNumList;
    }

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session, taskId ).getStopTime();
        }
    }

    private class StartTask extends TestThreadBase {
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
                        .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                        .get();
                taskId = ScmSystem.Task.startTransferTask( ws, condition,
                        ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );

                // check result
                while ( true ) {
                    ScmTask taskInfo = ScmSystem.Task
                            .getTask( session, taskId );
                    if ( taskInfo.getStopTime() != null ) {
                        if ( taskInfo.getRunningFlag() ==
                                CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ) {
                            // task detail
                            try ( Sequoiadb db = TestSdbTools
                                    .getSdb( TestScmBase.mainSdbUrl ) ) {
                                DBCollection clDB = db.getCollectionSpace(
                                        TestSdbTools.SCM_CS )
                                        .getCollection(
                                                TestSdbTools.SCM_CL_TASK );
                                BSONObject matcher = new BasicBSONObject();
                                matcher.put( "id", taskId.get() );
                                String detail = ( String ) clDB
                                        .query( matcher, null, null, null )
                                        .getNext().get( "detail" );
                                if ( !( detail.toString()
                                        .contains( "open lob failed" )
                                        || detail.toString()
                                        .contains( "read lob failed" ) ) ) {
                                    throw new Exception(
                                            "task detail error." +
                                                    detail.toString() +
                                                    " taskId = " +
                                                    taskId.get() );
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

    private class DeleteFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( sourceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );

                // delete
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            } finally {
                if ( session != null )
                    session.close();
            }
        }
    }

}