package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-5269:ScmTask.getTaskConfig接口测试
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class GetTaskConfig5269 extends TestScmBase {
    private String fileName = "fileName5269";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmSession session = null;
    private ScmWorkspace wsM = null;
    private BSONObject querycond = null;
    private WsWrapper wsp = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        querycond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 测试迁移任务
        testTransferTask();

        // 测试清理任务
        testCleanTask();

        // 测试迁移并清理任务
        testMoveFileTask();

        // 测试清理任务
        testSpaceRecycleTask();
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            session.close();
        }
    }

    private void testSpaceRecycleTask() throws ScmException {
        ScmSpaceRecyclingTaskConfig config = new ScmSpaceRecyclingTaskConfig();
        config.setWorkspace( wsM );
        config.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 2 ) );
        config.setMaxExecTime( 120000 );
        BSONObject expBson = config.getBSONObject();

        ScmId task = ScmSystem.Task.startSpaceRecyclingTask( config );
        ScmTaskConfig conf = ScmSystem.Task.getTask( session, task )
                .getTaskConfig();
        BSONObject actBson = conf.getBSONObject();
        Assert.assertEquals( actBson, expBson );
    }

    private void testMoveFileTask() throws ScmException {
        ScmMoveTaskConfig config = new ScmMoveTaskConfig();
        config.setWorkspace( wsM );
        config.setTargetSite( branchSite.getSiteName() );
        config.setScope( ScmType.ScopeType.SCOPE_CURRENT );
        config.setDataCheckLevel( ScmDataCheckLevel.STRICT );
        config.setQuickStart( true );
        config.setCondition( querycond );
        config.setRecycleSpace( true );
        BSONObject expBson = config.getBSONObject();

        ScmId task = ScmSystem.Task.startMoveTask( config );
        ScmTaskConfig conf = ScmSystem.Task.getTask( session, task )
                .getTaskConfig();
        BSONObject actBson = conf.getBSONObject();
        Assert.assertEquals( actBson, expBson );
    }

    private void testCleanTask() throws ScmException {
        ScmCleanTaskConfig config = new ScmCleanTaskConfig();
        config.setWorkspace( wsM );
        config.setScope( ScmType.ScopeType.SCOPE_CURRENT );
        config.setDataCheckLevel( ScmDataCheckLevel.STRICT );
        config.setQuickStart( true );
        config.setCondition( querycond );
        config.setRecycleSpace( true );
        BSONObject expBson = config.getBSONObject();

        ScmId task = ScmSystem.Task.startCleanTask( config );
        ScmTaskConfig conf = ScmSystem.Task.getTask( session, task )
                .getTaskConfig();
        BSONObject actBson = conf.getBSONObject();
        Assert.assertEquals( actBson, expBson );
    }

    private void testTransferTask() throws ScmException {
        ScmTransferTaskConfig config = new ScmTransferTaskConfig();
        config.setWorkspace( wsM );
        config.setTargetSite( branchSite.getSiteName() );
        config.setScope( ScmType.ScopeType.SCOPE_CURRENT );
        config.setDataCheckLevel( ScmDataCheckLevel.STRICT );
        config.setQuickStart( true );
        config.setCondition( querycond );
        BSONObject expBson = config.getBSONObject();

        ScmId task = ScmSystem.Task.startTransferTask( config );
        ScmTaskConfig conf = ScmSystem.Task.getTask( session, task )
                .getTaskConfig();
        BSONObject actBson = conf.getBSONObject();
        Assert.assertEquals( actBson, expBson );
    }
}