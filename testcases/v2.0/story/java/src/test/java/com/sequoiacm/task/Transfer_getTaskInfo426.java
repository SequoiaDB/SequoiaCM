package com.sequoiacm.task;

import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-426:获取任务信息
 * @author huangxiaoni init
 * @date 2017.6.14
 */

public class Transfer_getTaskInfo426 extends TestScmBase {

    private boolean runSuccess = false;

    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private ScmId taskId = null;
    private String authorName = "transfer426";

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();
            // login
            // session = TestScmTools.createSession(TestScmBase.hostName2,
            // TestScmBase.port2);
            session = ScmSessionUtils.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            // cleanEnv
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // write scm file
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            fileId = file.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void testGetTaskInfo() {
        try {
            // startTask
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            taskId = ScmSystem.Task.startTransferTask( ws, condition,
                    ScmType.ScopeType.SCOPE_CURRENT,
                    ScmInfo.getRootSite().getSiteName() );

            ScmTaskUtils.waitTaskFinish( session, taskId );

            // check task info
            ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
            Assert.assertEquals( taskInfo.getProgress(), 100 );
            Assert.assertEquals( taskInfo.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
            Assert.assertEquals( taskInfo.getType(),
                    CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
            Assert.assertEquals( taskInfo.getWorkspaceName(), ws.getName() );
            Assert.assertEquals( taskInfo.getContent(), condition );
            Assert.assertNotNull( taskInfo.getId() );

            long taskStartTime = taskInfo.getStartTime().getTime();
            long taskStopTime = taskInfo.getStopTime().getTime();
            Assert.assertNotNull( taskStartTime );
            Assert.assertNotNull( taskStopTime );
            long fileCreateTime = ScmFactory.File.getInstance( ws, fileId )
                    .getCreateTime().getTime();
            if ( fileCreateTime < taskStartTime
                    && taskStartTime < taskStopTime ) {
            } else {
                throw new Exception( "time error, " + "\nfileCreateTime="
                        + fileCreateTime + "\ntaskStartTime =" + taskStartTime
                        + "\ntaskStopTime  =" + taskStopTime );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() + "  node INFO = "
                    + branceSite.toString() + " taskId = " + taskId.get() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

}