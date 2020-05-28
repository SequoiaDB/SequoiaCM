package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-419:分中心开始迁移任务，其他中心停止任务 分中心A开始迁移任务，主中心停止任务
 * @author huangxiaoni init
 * @date 2017.6.15
 */

public class Transfer_stopTaskByMainCenter419 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession sessionM = null; // mainCenter
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null; // subCenterA
    private ScmWorkspace wsA = null;

    private ScmId taskId = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "transfer419M";
    private int fileSize = 1024 * 1024;
    private int fileNum = 10;
    private int startNum = 2;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();
    private List< Integer > siteNumList = new ArrayList< Integer >();

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            for ( int i = 0; i < fileNum; i++ ) {
                String filePath = localPath + File.separator + "localFile_"
                        + fileSize + i + ".txt";
                TestTools.LocalFile.createFile( filePath, fileSize + i );
                filePathList.add( filePath );
            }

            rootSite = ScmInfo.getRootSite();

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );

            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            writeFileFromSubCenterA( wsA );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testTransfer() {
        try {
            startTaskFromSubCenterA( wsA );
            // waitTransferPartFile(fileIdList);
            waitTaskRunning();
            ScmSystem.Task.stopTask( sessionA, taskId );
            waitTaskstop( sessionA );

            // check results
            checkMetaAndLobs();
            readFileFromMainCenter( wsM );
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
                    ScmFactory.File.getInstance( wsM, fileId ).delete( true );
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

    private void writeFileFromSubCenterA( ScmWorkspace ws )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePathList.get( i ) );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void startTaskFromSubCenterA( ScmWorkspace ws )
            throws ScmException {
        int value = fileSize + startNum;
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).greaterThanEquals( value )
                .get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition );
    }

    private void checkMetaAndLobs() throws Exception {
        List< Integer > siteNumList = getSiteNumList( wsM );
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
            }
        }
    }

    private void readFileFromMainCenter( ScmWorkspace ws ) throws Exception {
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

    private List< Integer > getSiteNumList( ScmWorkspace ws ) {
        try {
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

    private void waitTaskstop( ScmSession session ) throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session, taskId ).getStopTime();
        }
    }

    private void waitTaskRunning() throws Exception {
        Date startTime = null;
        Date stopTime = null;
        while ( startTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
            startTime = ScmSystem.Task.getTask( sessionA, taskId )
                    .getStartTime();
            if ( stopTime != null ) {
                throw new Exception( "startTime = " + startTime
                        + ", stopTime = " + stopTime
                        + ", stopTime is not null, taskId = " + taskId.get() );
            }
        }
    }
}