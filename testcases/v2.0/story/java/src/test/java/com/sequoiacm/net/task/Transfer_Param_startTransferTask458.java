package com.sequoiacm.net.task;

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
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-458:startTransferTask参数校验
 *            ScmSystem.Task.startTransferTask接口参数校验： 1）有效参数： （基本功能里面覆盖） ws: 存在；
 *            condition：BSONObject、存在的文件属性、不存在的文件属性； 2）无效参数： ws: 不存在、null
 *            condition: null
 * @author huangxiaoni init
 * @date 2017.6.15
 */

public class Transfer_Param_startTransferTask458 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private boolean runSuccess4 = false;

    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String authorName = "transfer458";

    private ScmId taskId = null;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );
            // cleanEnv
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // login
            session = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            // write scm file
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            fileId = file.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAttriNotExist() {
        try {
            // startTask
            BSONObject condition = ScmQueryBuilder.start( "test" )
                    .greaterThanEquals( "aa" )
                    .and( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
            taskId = ScmSystem.Task
                    .startTransferTask( ws, condition, ScopeType.SCOPE_CURRENT,
                            targetSite.getSiteName() );
            ScmTaskUtils.waitTaskFinish( session, taskId );
            // check task info
            ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
            Assert.assertEquals( taskInfo.getContent(), condition );
            ScmTaskUtils.waitTaskFinish( session, taskId );
            SiteWrapper[] expSiteList = { sourceSite };
            ScmFileUtils.checkMeta( ws, fileId, expSiteList );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWsNotExist() throws ScmException {
        try {
            ScmWorkspace ws1 = ScmFactory.Workspace
                    .getWorkspace( "testaaa", session );
            BSONObject condition = ScmQueryBuilder.start( "test" )
                    .greaterThanEquals( "aa" ).get();
            ScmSystem.Task
                    .startTransferTask( ws1, condition, ScopeType.SCOPE_CURRENT,
                            targetSite.getSiteName() );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.WORKSPACE_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess2 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWsIsNull() throws ScmException {
        try {
            BSONObject condition = ScmQueryBuilder.start( "test" )
                    .greaterThanEquals( "aa" ).get();
            ScmSystem.Task.startTransferTask( null, condition,
                    ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess3 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testConditionIsNull() throws ScmException {
        try {
            ScmSystem.Task.startTransferTask( ws, null, ScopeType.SCOPE_CURRENT,
                    targetSite.getSiteName() );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess4 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( ( runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4 ) ||
                    forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
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