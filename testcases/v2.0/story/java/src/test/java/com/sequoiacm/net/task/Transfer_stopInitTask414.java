package com.sequoiacm.net.task;

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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @FileName SCM-414: 停止init状态的任务
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、在分中心A开始迁移任务； 2、任务为init状态时（调用ScmTask.getRunningFlag()接口获取任务状态）停止任务；
 * 3、检查执行结果正确性；
 */
public class Transfer_stopInitTask414 extends TestScmBase {

    private boolean runSuccess = false;

    private File localPath = null;
    private String filePath = null;
    private int FILE_SIZE = new Random().nextInt( 1024 ) + 1;
    private ScmId fileId = null;

    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > taskIdList = new ArrayList< ScmId >();
    private BSONObject cond = null;

    private String authorName = "StopInitTask414";
    private Date expStartTime = null;
    private Date expStopTime = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
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

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            session = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            createFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException {
        // it is difficult to meet with init status
        ScmId taskId = stopAndStartTask();
        checkTaskAttribute( taskId );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
                for ( int i = 0; i < taskIdList.size(); i++ ) {
                    TestSdbTools.Task.deleteMeta( taskIdList.get( i ) );
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

    private void createFile() throws ScmException {
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        fileId = scmfile.save();
    }

    private ScmId startTask() {
        ScmId taskId = null;
        try {
            expStartTime = new Date();
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            taskId = ScmSystem.Task
                    .startTransferTask( ws, cond, ScopeType.SCOPE_CURRENT,
                            targetSite.getSiteName() );
            taskIdList.add( taskId );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return taskId;
    }

    private ScmId stopAndStartTask() {
        ScmId taskId = null;
        try {
            for ( int i = 0; i < 100; i++ ) {
                taskId = startTask();
                ScmTask scmTask = ScmSystem.Task.getTask( session, taskId );
                int flag = scmTask.getRunningFlag();
                if ( flag == CommonDefine.TaskRunningFlag.SCM_TASK_INIT ) {
                    ScmSystem.Task.stopTask( session, taskId );
                    waitTaskStop( taskId );
                    break;
                } else {
                    waitTaskStop( taskId );
                    taskId = null;
                }
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() + " taskId INFO " + taskId.get() );
        }
        return taskId;
    }

    private ScmTask waitTaskStop( ScmId taskId ) throws ScmException {
        ScmTask scmTask = null;
        Date stopTime = null;
        while ( stopTime == null ) {
            scmTask = ScmSystem.Task.getTask( session, taskId );
            stopTime = scmTask.getStopTime();
        }
        return scmTask;
    }

    private void checkTaskAttribute( ScmId taskId ) throws ScmException {
        if ( null != taskId ) {
            ScmTask task = waitTaskStop( taskId );
            Assert.assertEquals( task.getId(), taskId );
            Assert.assertEquals( task.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL );
            Assert.assertEquals( task.getType(),
                    CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
            Assert.assertEquals( task.getWorkspaceName(), ws.getName() );
            checkTime( expStartTime, task.getStartTime() );
            checkTime( expStopTime, task.getStopTime() );
        }
    }

    private void checkTime( Date expDate, Date actDate ) {
        long time = expDate.getTime() - actDate.getTime();
        if ( Math.abs( time ) > 100000 ) {
            Assert.fail( "time is different: expTime=" + expDate.getTime() +
                    ",actTime=" + actDate.getTime() );
        }
    }

}
