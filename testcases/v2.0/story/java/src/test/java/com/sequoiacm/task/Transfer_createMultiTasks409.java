package com.sequoiacm.task;

import java.io.File;
import java.util.*;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.apache.log4j.Logger;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-409: 创建同一个ws下多个迁移任务
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、开始多个迁移任务，ws相同; 2、检查迁移任务执行结果；
 */

public class Transfer_createMultiTasks409 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( Transfer_createMultiTasks409.class );

    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int FILE_SIZE = new Random().nextInt( 1024 ) + 1;
    private ScmSession sessionA = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;
    private String authorName = "CreateMultiTasks409";
    private ScmId fileId = null;
    private BSONObject cond = null;
    private List< ScmId > taskIdList = new ArrayList<>();

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + FILE_SIZE
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, FILE_SIZE );

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            sessionA = ScmSessionUtils.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            createFile( ws, filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        startTask();
        waitTaskStop();
        getSuccessTask();
        checkTaskAttribute();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                for ( ScmId taskID : taskIdList ) {
                    TestSdbTools.Task.deleteMeta( taskID );
                }
                ScmFileUtils.cleanFile( ws_T, cond );
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
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        fileId = scmfile.save();
    }

    private void startTask() throws ScmException {
        try {
            for ( int i = 0; i < 6; i++ ) {
                ScmId taskID = ScmSystem.Task.startTransferTask( ws, cond,
                        ScmType.ScopeType.SCOPE_CURRENT,
                        ScmInfo.getRootSite().getSiteName() );
                taskIdList.add( taskID );
            }
        } catch ( ScmException e ) {
            if ( ScmError.TASK_DUPLICATE != e.getError() ) {
                logger.error( "transfer cond INFO " + cond.toString() );
                throw e;
            }
        }
    }

    private void waitTaskStop() throws Exception {
        for ( ScmId taskID : taskIdList ) {
            ScmTaskUtils.waitTaskStop( sessionA, taskID );
        }
    }

    private void getSuccessTask() throws Exception {
        for ( ScmId taskID : taskIdList ) {
            ScmTask task = ScmSystem.Task.getTask( sessionA, taskID );
            if ( task.getSuccessCount() == 1 ) {
                taskId = task.getId();
            }
        }
    }

    private void checkTaskAttribute() {
        ScmTask task = null;
        try {
            task = ScmSystem.Task.getTask( sessionA, taskId );
            Assert.assertEquals( task.getId(), taskId );
            Assert.assertEquals( task.getProgress(), 100 );
            Assert.assertEquals( task.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
            Assert.assertEquals( task.getType(),
                    CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
            Assert.assertEquals( task.getWorkspaceName(), ws.getName() );

            SiteWrapper rootSite = ScmInfo.getRootSite();
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() + "  task INFO " + task.toString() );
        }
    }
}
