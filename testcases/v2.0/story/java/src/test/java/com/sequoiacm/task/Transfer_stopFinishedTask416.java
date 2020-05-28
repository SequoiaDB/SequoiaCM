package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-416: 停止finish状态的任务
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、在分中心A开始迁移任务；
 * 2、任务为finish状态时（任务执行完成，调用ScmTask.getRunningFlag()接口获取任务状态为finish）停止任务；
 * 3、检查执行结果正确性；
 */

public class Transfer_stopFinishedTask416 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 10;
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private BSONObject cond = null;

    private File localPath = null;
    private String filePath = null;
    private String authorName = "StopFinishedTask416";
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;
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
            ScmSystem.Task.stopTask( sessionA, taskId );
            Assert.assertEquals(
                    ScmSystem.Task.getTask( sessionA, taskId ).getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ); // 3: finish
            checkTransfered();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() + " taskId INFO " + taskId.get() );
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
                    ;
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
        // ScmWorkspace ws = TestScmTools.ScmCommon.getWorkspace(session);
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private ScmId transferAllFile( ScmWorkspace ws ) throws ScmException {
        // ScmWorkspace ws = TestScmTools.ScmCommon.getWorkspace(session);
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        return ScmSystem.Task.startTransferTask( ws, condition );
    }

    private void checkTransfered() {
        try {
            SiteWrapper rootSite = ScmInfo.getRootSite();
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}