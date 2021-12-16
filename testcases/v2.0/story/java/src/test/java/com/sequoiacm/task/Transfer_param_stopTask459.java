package com.sequoiacm.task;

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
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-460:stopTask参数校验
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * ScmSystem.Task.stopTask参数校验： 1）有效参数： session: 存在 taskId：存在 2）无效参数： session:
 * null taskId：不存在、null 2、检查校验结果
 */
public class Transfer_param_stopTask459 extends TestScmBase {
    private boolean runSuccess = true;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;
    private ScmId fileId = null;
    private String authorName = "StopTaskArgValid459";

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            fileId = createFile( ws );
            taskId = ScmSystem.Task.startTransferTask( ws, cond,
                    ScmType.ScopeType.SCOPE_CURRENT,ScmInfo.getRootSite().getSiteName());
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testInvalidSeeionArg() throws ScmException {
        try {
            ScmSystem.Task.stopTask( null, taskId );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testNullTaskIdArg() throws ScmException {
        try {
            ScmSystem.Task.stopTask( sessionA, null );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testInvalidTaskId() throws ScmException {
        try {
            ScmSystem.Task.stopTask( sessionA, fileId );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.TASK_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmTaskUtils.waitTaskFinish( sessionA, taskId );
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private ScmId createFile( ScmWorkspace ws ) throws ScmException {
        ScmId fileId = null;
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        fileId = scmfile.save();
        return fileId;
    }
}
