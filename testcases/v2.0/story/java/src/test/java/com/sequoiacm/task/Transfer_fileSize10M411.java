package com.sequoiacm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-411: 迁移单个文件>=10M大小的文件
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、分中心A写入文件，单个文件大小覆盖：大于100M（如120M、100M）； 2、分中心A迁移该文件；
 * 3、检查执行结果、主中心、分中心A文件内容及元数据正确性；
 */
public class Transfer_fileSize10M411 extends TestScmBase {
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int FILE_SIZE =
            new Random().nextInt( 1024 * 1024 * 5 ) + 1024 * 1024 * 10;
    private String authorName = "TransferFile10M411";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private ScmId fileId = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, FILE_SIZE );

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            session = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            prepareFiles( ws );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            startTask();
            ScmTaskUtils.waitTaskFinish( session, taskId );
            waitTaskStop();
            checkTaskAttribute();
            checkTransContent();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        scmfile.setContent( filePath );
        fileId = scmfile.save();
    }

    private void startTask() {
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            taskId = ScmSystem.Task.startTransferTask( ws, cond );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session, taskId ).getStopTime();
        }
    }

    private void checkTaskAttribute() throws ScmException {
        ScmTask task = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( task.getId(), taskId );
        Assert.assertEquals( task.getProgress(), 100 );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
        Assert.assertEquals( task.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( task.getWorkspaceName(), ws.getName() );
        Assert.assertNotNull( task.getStartTime() );
        Assert.assertNotNull( task.getStopTime() );
    }

    private void checkTransContent() throws IOException, ScmException {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {

            SiteWrapper rootSite = ScmInfo.getRootSite();
            // login
            session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), session );
            for ( ScmId fileId : fileIdList ) {
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );
                // check content on main center
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
                // check meta data
                SiteWrapper[] expSiteList = { rootSite, branceSite };
                ScmFileUtils
                        .checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                                filePath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + " node INFO" + branceSite.toString() +
                    " fileId = " + fileId.get() );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( session != null )
                session.close();
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
