package com.sequoiacm.task;

import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
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
 * @FileName SCM-423: 匹配0条任务记录，获取任务列表
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、获取任务列表，查询条件匹配查询0条记录； 2、分别执行hasNext()、getNext()、close()； 3、检查执行结果正确性；
 */
public class Transfer_getCurWhenMatch0Record423 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;
    private String authorName = "GetCurWhenMatch0Record423";
    private ScmId fileId = null;
    private BSONObject cond = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            sessionA = ScmSessionUtils.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            createFile( ws );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        taskId = ScmSystem.Task.startTransferTask( ws, cond,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmInfo.getRootSite().getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionA, taskId );
        checkCursorMethod();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
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
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        fileId = scmfile.save();
        return fileId;
    }

    private ScmCursor< ScmTaskBasicInfo > getTaskList() {
        ScmTask task = null;
        ScmCursor< ScmTaskBasicInfo > cursor = null;
        while ( true ) {
            try {
                task = ScmSystem.Task.getTask( sessionA, taskId );
                if ( task
                        .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ) {
                    BSONObject cond = ScmQueryBuilder
                            .start( ScmAttributeName.Task.TYPE ).is( "aaa2" )
                            .get();
                    cursor = ScmSystem.Task.listTask( sessionA, cond );
                    break;
                } else if ( task
                        .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ) {
                    throw new Exception( "task was aborted" );
                }
            } catch ( Exception e ) {
                Assert.fail( e.getMessage() + "task INFO " + task.toString() );
            }
        }
        return cursor;
    }

    private void checkCursorMethod() {
        ScmCursor< ScmTaskBasicInfo > cursor = getTaskList();
        try {
            Assert.assertEquals( cursor.hasNext(), false );
            Assert.assertEquals( cursor.getNext(), null );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }
}
