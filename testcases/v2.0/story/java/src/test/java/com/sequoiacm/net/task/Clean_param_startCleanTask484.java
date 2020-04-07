package com.sequoiacm.net.task;

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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-484:startCleanTask参数校验
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * ScmSystem.Task.startCleanTask接口参数校验： 1）有效参数：ws:
 * 存在；condition：BSONObject、存在的文件属性、不存在的文件属性； 2）无效参数：ws: 不存在、nullcondition: null
 * 2、检查校验结果
 */
public class Clean_param_startCleanTask484 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private ScmId taskId = null;
    private String authorName = "StartCleanTask484";
    private ScmId fileId = null;
    private BSONObject cond = null;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );
            // login in
            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            writeFileFromSubCenterA();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWSNoExist() throws ScmException {
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( "ws_1", sessionA );
            ScmSystem.Task.startCleanTask( ws, cond );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.WORKSPACE_NOT_EXIST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess1 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testWSIsNull() throws ScmException {
        try {
            ScmSystem.Task.startCleanTask( null, cond );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess2 = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testCondIsNull() throws ScmException {
        try {
            ScmSystem.Task.startCleanTask( ws, null );
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

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess1 || runSuccess2 || runSuccess3 || forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void writeFileFromSubCenterA() {
        try {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            fileId = scmfile.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}
