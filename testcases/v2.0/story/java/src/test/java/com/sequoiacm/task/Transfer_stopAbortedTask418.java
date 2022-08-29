package com.sequoiacm.task;

import java.io.File;
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
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-418: 停止abort状态的任务
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、在分中心A开始迁移任务；
 * 2、任务为abort状态时（调用ScmTask.getRunningFlag()接口获取任务状态）停止任务，模拟方法：任务执行过程中网络异常写元数据失败，
 * 或，直连SDB修改已完成任务状态为abort； 3、检查执行结果正确性；
 */

public class Transfer_stopAbortedTask418 extends TestScmBase {

    private final int fileSize = 200 * 1024;
    private final int fileNum = 10;
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private BSONObject cond = null;

    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;
    private String authorName = "StopAbortedTask418";
    private ScmId taskId = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            // sessionA = TestScmTools.createSession(TestScmBase.hostName2,
            // TestScmBase.port2);
            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            prepareFiles( ws );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            taskId = transferAllFile( ws );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            changeFlagToAbortOnSdb( taskId );
            ScmSystem.Task.stopTask( sessionA, taskId );
            waitTaskStop();
            Assert.assertEquals(
                    ScmSystem.Task.getTask( sessionA, taskId ).getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_ABORT ); // 5: abort

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File.deleteInstance( ws, fileIdList.get( i ),
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
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

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            fileIdList.add( scmfile.save() );
        }
    }

    private ScmId transferAllFile( ScmWorkspace ws ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        return ScmSystem.Task.startTransferTask( ws, condition,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmInfo.getRootSite().getSiteName() );
    }

    private void changeFlagToAbortOnSdb( ScmId taskId ) {
        try ( Sequoiadb sdb = new Sequoiadb( TestScmBase.mainSdbUrl,
                TestScmBase.sdbUserName, TestScmBase.sdbPassword )) {
            DBCollection cl = sdb.getCollectionSpace( TestSdbTools.SCM_CS )
                    .getCollection( TestSdbTools.SCM_CL_TASK );
            cl.update( "{ id: '" + taskId.get() + "' }",
                    "{ $set: { running_flag: 5 } }", null );
        } catch ( BaseException e ) {
            e.printStackTrace();
            throw e;
        }
    }

    private void waitTaskStop() throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }
}