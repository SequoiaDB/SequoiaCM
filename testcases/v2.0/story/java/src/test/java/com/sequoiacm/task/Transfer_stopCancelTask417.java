package com.sequoiacm.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-417: 停止cancal状态的任务（重复停止）
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、在分中心A开始迁移任务； 2、任务运行过程中停止任务； 3、重复多次停止该任务； 4、检查执行结果正确性；
 */
public class Transfer_stopCancelTask417 extends TestScmBase {

    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 200;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 3;

    private String authorName = "StopCancelTask417";
    private BSONObject cond = null;

    private SiteWrapper branceSite = null;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branceSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branceSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

        cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        createFile( ws, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException {
        taskId = ScmSystem.Task.startTransferTask( ws, cond,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmInfo.getRootSite().getSiteName() );
        stopTaskAgain();
        checkTaskAttribute();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
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

    private void createFile( ScmWorkspace ws, String filePath )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            ScmId fileId = scmfile.save();
            fileIdList.add( fileId );
        }
    }

    private void stopTaskAgain() {
        ScmTask scmTask = null;
        try {
            while ( true ) {
                scmTask = ScmSystem.Task.getTask( sessionA, taskId );
                int flag = scmTask.getRunningFlag();
                if ( flag == 2 ) {
                    ScmSystem.Task.stopTask( sessionA, taskId );
                    break;
                }
            }
            waitTaskStop();
            ScmSystem.Task.stopTask( sessionA, taskId );
            int progress = ScmSystem.Task.getTask( sessionA, taskId )
                    .getProgress();
            ScmSystem.Task.stopTask( sessionA, taskId );
            int progress1 = ScmSystem.Task.getTask( sessionA, taskId )
                    .getProgress();
            if ( progress != progress1 ) {
                Assert.fail(
                        "Stop task again,the progress shouldn't be changed" );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() + " task INFO " + scmTask.toString() );
        }
    }

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }

    private void checkTaskAttribute() throws ScmException {
        ScmTask task = ScmSystem.Task.getTask( sessionA, taskId );
        Assert.assertEquals( task.getId(), taskId );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL );
        Assert.assertEquals( task.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( task.getWorkspaceName(), ws.getName() );
    }
}
