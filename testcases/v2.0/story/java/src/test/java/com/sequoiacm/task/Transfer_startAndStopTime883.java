package com.sequoiacm.task;

import java.text.SimpleDateFormat;
import java.util.Date;
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
 * @Testcase: SCM-883:主中心、分中心时间不同步，迁移任务 （分中心本地时间比主中心本地时间慢5分钟30秒）
 * @Author huangxiaoni init
 * @Date 2017.10.10
 */

public class Transfer_startAndStopTime883 extends TestScmBase {
    private final String authorName = "Transfer883";
    private boolean runSuccess = false;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;
    private ScmId fileId = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, Exception {
        try {
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            // set the system time of subCenter
            SimpleDateFormat dateFmt = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss" );
            // 5m30s slower than current time
            Long updateTime = new Date().getTime() - 5 * 60 * 1000 - 30 * 1000;

            TestTools.setSystemTime( branceSite.getNode().getHost(), "\"" +
                    dateFmt.format( updateTime ) + "\"" );

            // login
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // write file
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            fileId = file.save();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testTransfer() {
        try {
            // start transfer task
            this.startTransferTask();
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );

            // get startTime and stopTime, and check
            ScmTask task = ScmSystem.Task.getTask( sessionA, taskId );
            Long startTime = task.getStartTime().getTime();
            Long stopTime = task.getStopTime().getTime();
            if ( startTime >= stopTime ) {
                Assert.fail(
                        "error, expect startTime < stopTime, actual startTime"
                                + " >= stopTime" );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != sessionA ) {
                sessionA.close();
            }
            TestTools.restoreSystemTime( branceSite.getNode().getHost() );
            System.out.println( "wait 10s..." );
            Thread.sleep( 10 * 1000 );
        }
    }

    private void startTransferTask() throws ScmException, InterruptedException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        taskId = ScmSystem.Task.startTransferTask( wsA, condition );
    }
}