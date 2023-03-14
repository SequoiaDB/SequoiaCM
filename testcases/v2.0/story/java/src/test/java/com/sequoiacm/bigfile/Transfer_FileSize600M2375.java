package com.sequoiacm.bigfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description：迁移600M文件
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */

public class Transfer_FileSize600M2375 extends TestScmBase {
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private long fileSize = 1024 * 1024 * 600;
    private String authorName = "TransferFile600M";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;
    private ScmId fileId = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        branceSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( branceSite );
        ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( ws_T, cond );
        System.out.println( "fileSize = " + new File( filePath ).length() );
        prepareFiles( ws );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        startTask();
        waitTaskStop();
        ScmTaskUtils.waitTaskFinish( session, taskId );
        checkTaskAttribute();
        checkTransContent();
        runSuccess = true;
    }

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        scmfile.setContent( filePath );
        fileId = scmfile.save();
        System.out.println( "fileId = " + fileId.get() );
    }

    private void startTask() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        taskId = ScmSystem.Task.startTransferTask( ws, cond,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmInfo.getRootSite().getSiteName() );
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

    private void checkTransContent() throws Exception {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            SiteWrapper rootSite = ScmInfo.getRootSite();
            // login
            session = ScmSessionUtils.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis = ScmFactory.File.createInputStream( scmfile );
            sis.read( fos );
            // check content on main center
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
            // check meta data
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
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
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
