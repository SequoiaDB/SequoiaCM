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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-475: 分中心执行任务过程中停止任务
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、在分中心A开始清理任务，清理条件匹配清理多个文件； 2、清理任务过程中停止任务； 3、检查执行结果；
 */
public class Clean_stopRunningTaskFromSubCenter475 extends TestScmBase {
    private boolean runSuccess = false;
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private String authorName = "StopRunningCleanTaskFromSubCenter475";
    private BSONObject cond = null;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;
    private int fileNum = 50;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private SiteWrapper rootSite = null;
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
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );
            // login in
            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            writeFileFromSubCenterA();
            readFileFromMainCenter();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            startCleanTaskFromSubCenterA();
            ScmSystem.Task.stopTask( sessionA, taskId );
            waitTaskstop();
            checkCleanTaskResult();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestSdbTools.Task.deleteMeta( taskId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void writeFileFromSubCenterA() {
        ScmId fileId = null;
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setContent( filePath );
                scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
                scmfile.setAuthor( authorName );
                fileId = scmfile.save();
                fileIdList.add( fileId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }

    }

    private void readFileFromMainCenter() throws Exception {
        ScmSession sessionM = null;
        try {
            // login
            sessionM = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    sessionM );
            for ( int i = 0; i < fileNum; i++ ) {
                // read content
                ScmFile file = ScmFactory.File.getInstance( ws,
                        fileIdList.get( i ) );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                file.getContent( downloadPath );
            }
        } finally {
            if ( sessionM != null )
                sessionM.close();
        }
    }

    private void startCleanTaskFromSubCenterA() {
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            taskId = ScmSystem.Task.startCleanTask( ws, cond );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkCleanTaskResult() {
        try {
            // check task Attribute
            ScmTask task = ScmSystem.Task.getTask( sessionA, taskId );
            Assert.assertEquals( task.getId(), taskId );
            Assert.assertEquals(
                    task.getProgress() >= 0 && task.getProgress() < 100, true );
            Assert.assertEquals( task.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL );
            Assert.assertEquals( task.getType(),
                    CommonDefine.TaskType.SCM_TASK_CLEAN_FILE );
            Assert.assertEquals( task.getWorkspaceName(), ws_T.getName() );

            List< Integer > siteNumList = getSiteNumList();
            for ( int i = 0; i < fileNum; i++ ) {
                ScmId fileId = fileIdList.get( i );
                int siteIdList = siteNumList.get( i );
                if ( siteIdList == 1 ) {
                    SiteWrapper[] expSiteList = { rootSite };
                    ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                            localPath, filePath );
                } else if ( siteIdList == 2 ) {
                    SiteWrapper[] expSiteList = { rootSite, branceSite };
                    ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                            localPath, filePath );
                } else {
                    Assert.fail( "the result is not the expected result" );
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void waitTaskstop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }

    private List< Integer > getSiteNumList() {
        List< Integer > siteNumList = new ArrayList< Integer >();
        ScmFile file;
        try {
            for ( ScmId fileId : fileIdList ) {
                file = ScmFactory.File.getInstance( ws, fileId );
                int actSiteNum = file.getLocationList().size();
                siteNumList.add( actSiteNum );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return siteNumList;
    }
}
