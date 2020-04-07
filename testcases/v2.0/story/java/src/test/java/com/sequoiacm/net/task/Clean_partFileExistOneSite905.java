package com.sequoiacm.net.task;

import java.io.File;
import java.util.ArrayList;
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
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-905:部分文件只存在于一个站点，清理文件
 * @author huangxiaoni init
 * @date 2017.8.31
 */

/*
 * 1、写入多个文件，其中部分文件只存在于分中心，其他文件在主中心和分中心均存在，覆盖（如在A中心清理文件）； 1）文件只在A中心； 2）文件只在B中心；
 * 2、清理写入的所有文件； 3、检查任务执行结果以及任务进度；
 */

public class Clean_partFileExistOneSite905 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private ScmId taskId = null;
    private String authorName = "clean905";
    private int fileSize = 100;
    private int fileNum = 10;
    private List< ScmId > fileIdList1 = new ArrayList<>();
    private List< ScmId > fileIdList2 = new ArrayList<>();
    private List< ScmId > fileIdList3 = new ArrayList<>();
    private List< ScmId > fileIdList4 = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;

    private List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
    private WsWrapper ws_T = null;

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
            siteList = ScmNetUtils.getAllSite( ws_T );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // login
            sessionM = TestScmTools.createSession( siteList.get( 2 ) );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );

            sessionA = TestScmTools.createSession( siteList.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            sessionB = TestScmTools.createSession( siteList.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionB );

            // ready scmfile
            this.writeFile( wsM, authorName, fileIdList1 );
            this.writeFile( wsA, authorName, fileIdList2 );
            this.writeFile( wsB, authorName, fileIdList3 );
            this.writeFile( wsA, authorName, fileIdList4 );
            this.readScmFile( wsM, fileIdList4 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            this.startCleanTask( sessionA, authorName );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );

            ScmTask taskInfo = ScmSystem.Task.getTask( sessionA, taskId );
            int totolFileSize = fileIdList1.size() + fileIdList2.size() +
                    fileIdList3.size() + fileIdList4.size();
            Assert.assertEquals( taskInfo.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
            Assert.assertEquals( taskInfo.getProgress(), 100 );
            Assert.assertEquals( taskInfo.getEstimateCount(), totolFileSize );
            Assert.assertEquals( taskInfo.getActualCount(),
                    fileIdList4.size() );
            Assert.assertEquals( taskInfo.getFailCount(), 0 );
            Assert.assertEquals( taskInfo.getSuccessCount(),
                    fileIdList4.size() );

            this.checkResults();
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
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.getInstance( wsM, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.getInstance( wsM, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList3 ) {
                    ScmFactory.File.getInstance( wsM, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList4 ) {
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
            if ( sessionB != null ) {
                sessionB.close();
            }

        }
    }

    private void writeFile( ScmWorkspace ws, String author,
            List< ScmId > fileIdList ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( author + "_" + i + UUID.randomUUID() );
            file.setAuthor( authorName );
            file.setContent( filePath );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void readScmFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws Exception {
        for ( int i = 0; i < fileIdList.size(); i++ ) {
            ScmId fileId = fileIdList.get( i );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            file.getContent( downloadPath );
        }
    }

    private void startCleanTask( ScmSession ss, String author )
            throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), ss );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
        taskId = ScmSystem.Task.startCleanTask( ws, condition );
    }

    private void checkResults() throws Exception {
        // file exists only in one site
        List< List< ScmId > > fileIdLists = new ArrayList<>();
        fileIdLists.add( fileIdList1 );
        fileIdLists.add( fileIdList2 );
        fileIdLists.add( fileIdList3 );
        SiteWrapper[] siteArr = { siteList.get( 2 ), siteList.get( 0 ),
                siteList.get( 1 ) };
        for ( int i = 0; i < siteArr.length; i++ ) {
            SiteWrapper[] expSiteIdList1 = { siteArr[ i ] };
            ScmFileUtils.checkMetaAndData( ws_T, fileIdLists.get( i ),
                    expSiteIdList1, localPath, filePath );
        }

        // clean files
        SiteWrapper[] expSiteList2 = { siteList.get( 2 ) };
        ScmFileUtils
                .checkMetaAndData( ws_T, fileIdList4, expSiteList2, localPath,
                        filePath );
    }
}