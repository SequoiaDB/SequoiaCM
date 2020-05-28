package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-415:停止running状态的任务
 * @author huangxiaoni init
 * @date 2017.6.14
 */

public class Transfer_stopRunningTask415 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmSession sessionM = null; // mainCenter
    private ScmSession sessionA = null; // subCenterA
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;

    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "transfer415";
    private int fileSize = 1024 * 1024;
    private int fileNum = 8;
    private int startNum = 2;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< Integer > siteNumList = new ArrayList< Integer >();
    private BSONObject cond = null;

    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
    private SiteWrapper branceSite = null;
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

            if ( ScmInfo.getSiteNum() > 2 ) {
                branceSiteList = ScmInfo.getBranchSites( 2 );
            } else {
                branceSiteList = ScmInfo.getBranchSites( 1 );
            }
            branceSite = branceSiteList.get( 0 );
            ws_T = ScmInfo.getWs();

            // login
            sessionM = TestScmTools.createSession( rootSite );
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testTransfer() {
        try {
            writeFileFromSubCenterA();
            stopTaskFromSubCenterA( sessionA );
            waitTaskStop();
            // check results
            checkMetaAndLobs();
            readFileFromMainCenter( sessionM );
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
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void writeFileFromSubCenterA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setContent( filePathList.get( i ) );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void stopTaskFromSubCenterA( ScmSession ss )
            throws ScmException, InterruptedException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );
        int value = fileSize + startNum;
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).greaterThanEquals( value )
                .and( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition );

        // check task info
        ScmTask taskInfo = null;
        while ( true ) {
            taskInfo = ScmSystem.Task.getTask( ss, taskId );
            Thread.sleep( new Random().nextInt( 1000 ) );
            if ( taskInfo
                    .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING ) {
                ScmSystem.Task.stopTask( ss, taskId );

                taskInfo = ScmSystem.Task.getTask( ss, taskId );
                if ( taskInfo
                        .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL ) {
                    Assert.assertNotEquals( taskInfo.getProgress(), 100 );
                }
            } else if ( taskInfo.getStopTime() != null ) {
                break;
            }
        }
        Assert.assertEquals( taskInfo.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( taskInfo.getWorkspaceName(), ws.getName() );
        Assert.assertEquals( taskInfo.getContent(), condition );
        Assert.assertNotNull( taskInfo.getId() );
        Assert.assertNotNull( taskInfo.getStartTime() );
        Assert.assertNotNull( taskInfo.getStopTime() );
    }

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }

    private void checkMetaAndLobs() throws Exception {
        List< Integer > siteNumList = getSiteNumList( sessionM );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String filePath = filePathList.get( i );
            int siteNum = siteNumList.get( i );
            if ( siteNum == 2 ) {
                SiteWrapper[] expSiteList = { rootSite, branceSite };
                ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                        localPath, filePath );
            } else {
                SiteWrapper[] expSiteList = { branceSite };
                ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                        localPath, filePath );
                ;
            }
        }

        // check site which does not have any data
        checkFreeSite();
    }

    private void readFileFromMainCenter( ScmSession ss ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );
        for ( int i = startNum; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String filePath = filePathList.get( i );

            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );

            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.getContent( downloadPath );

            // check content
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        }
    }

    private List< Integer > getSiteNumList( ScmSession ss ) {
        ScmWorkspace ws;
        try {
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), ss );
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile file = ScmFactory.File.getInstance( ws,
                        fileIdList.get( i ) );
                int actSiteNum = file.getLocationList().size();
                siteNumList.add( actSiteNum );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        return siteNumList;
    }

    private void checkFreeSite() throws Exception {
        ScmSession ss = null;
        int randNum = 0;
        try {
            if ( ScmInfo.getSiteNum() > 2 ) {
                ss = TestScmTools.createSession( branceSiteList.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), ss );
                randNum = new Random().nextInt( fileNum );
                ScmFileUtils.checkData( ws, fileIdList.get( randNum ),
                        localPath, filePathList.get( randNum ) );
                Assert.assertFalse( true,
                        "expect result is fail but actual is success.wsName"
                                + ws_T.getName() );
            }
        } catch ( ScmException e ) {
            if ( ScmError.DATA_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        } finally {
            if ( ss != null ) {
                ss.close();
            }
        }
    }
}