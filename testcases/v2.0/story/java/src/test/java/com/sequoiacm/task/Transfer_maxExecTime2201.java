package com.sequoiacm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
 * @FileName SCM-2201:设置maxExecTime,迁移文件
 * @Author fanyu
 * @Date 2018-09-05
 * @Version 1.00
 */

public class Transfer_maxExecTime2201 extends TestScmBase {
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1;
    private String authorName = "Transfer_maxExecTime2201";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > taskIdList = new ArrayList< ScmId >();
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 10;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            session = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            // set maxExecTime = 0;
            prepareFiles( ws );
            long maxExecTime = 0L;
            startTask( maxExecTime );
            ScmTaskUtils.waitTaskFinish( session, taskIdList.get( 0 ) );
            waitTaskStop( taskIdList.get( 0 ) );
            checkTaskAttribute( taskIdList.get( 0 ), maxExecTime );
            checkTransContent( fileIdList.subList( 0, fileNum ) );

            // set maxExecTime < 0;
            prepareFiles( ws );
            maxExecTime = -1L;
            startTask( maxExecTime );
            ScmTaskUtils.waitTaskFinish( session, taskIdList.get( 1 ) );
            waitTaskStop( taskIdList.get( 1 ) );
            checkTaskAttribute( taskIdList.get( 1 ), maxExecTime );
            checkTransContent( fileIdList.subList( fileNum, 2 * fileNum ) );

            // set maxExecTime > 0;
            prepareFiles( ws );
            maxExecTime = 1000 * 60 * 60L;
            startTask( maxExecTime );
            ScmTaskUtils.waitTaskFinish( session, taskIdList.get( 2 ) );
            waitTaskStop( taskIdList.get( 2 ) );
            checkTaskAttribute( taskIdList.get( 2 ), maxExecTime );
            checkTransContent( fileIdList.subList( 2 * fileNum, 3 * fileNum ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setContent( filePath );
            ScmId fileId = scmfile.save();
            fileIdList.add( fileId );
        }
    }

    private void startTask( long maxExecTime ) {
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmId taskId = ScmSystem.Task.startTransferTask( ws, cond,
                    ScopeType.SCOPE_CURRENT, maxExecTime,
                    ScmInfo.getRootSite().getSiteName() );
            taskIdList.add( taskId );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void waitTaskStop( ScmId taskId ) throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( session, taskId ).getStopTime();
        }
    }

    private void checkTaskAttribute( ScmId taskId, long maxExecTime )
            throws ScmException {
        ScmTask task = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( task.getId(), taskId );
        Assert.assertEquals( task.getProgress(), 100 );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
        Assert.assertEquals( task.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( task.getWorkspaceName(), ws.getName() );
        Assert.assertEquals( task.getMaxExecTime(), maxExecTime );
        Assert.assertNotNull( task.getStartTime() );
        Assert.assertNotNull( task.getStopTime() );
    }

    private void checkTransContent( List< ScmId > fileIdList )
            throws IOException, ScmException {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {

            SiteWrapper rootSite = ScmInfo.getRootSite();
            // login
            session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            for ( ScmId fileId : fileIdList ) {
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
                ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList,
                        localPath, filePath );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() + " node INFO" + branceSite.toString()
                    + " fileIdList = " + fileIdList.toString() );
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
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId taskId : taskIdList ) {
                    TestSdbTools.Task.deleteMeta( taskId );
                }
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
