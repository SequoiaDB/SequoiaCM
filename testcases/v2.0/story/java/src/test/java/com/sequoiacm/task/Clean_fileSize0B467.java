package com.sequoiacm.task;

import java.io.File;
import java.util.Date;
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
 * @FileName SCM-467: 清理0个文件
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、在分中心A开始清理任务，清理条件匹配清理0个文件； 2、任务执行完成后检查清理文件结果正确性；
 */

public class Clean_fileSize0B467 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmId fileId = null;
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private String authorName = "Clean0File467";
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );
            // login in
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            writeFileFromSubCenterB();
            readFileFromMainCenter();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR )
                    .is( authorName + "_NoExist" ).get();
            taskId = ScmSystem.Task.startCleanTask( wsA, cond );
            waitTaskStop();
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
                ScmFactory.File.deleteInstance( wsA, fileId, true );
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

    private void writeFileFromSubCenterB() {
        try {
            ScmFile scmfile = ScmFactory.File.createInstance( wsA );
            scmfile.setContent( filePath );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            fileId = scmfile.save();
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
            // read content
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
        } finally {
            if ( sessionM != null )
                sessionM.close();
        }
    }

    private void checkCleanTaskResult() {
        try {
            // check task Attribute
            ScmTask task = ScmSystem.Task.getTask( sessionA, taskId );
            Assert.assertEquals( task.getId(), taskId );
            Assert.assertEquals( task.getProgress(), 100 );
            Assert.assertEquals( task.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
            Assert.assertEquals( task.getType(),
                    CommonDefine.TaskType.SCM_TASK_CLEAN_FILE );
            Assert.assertEquals( task.getWorkspaceName(), ws_T.getName() );
            // check meta data
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }
}