package com.sequoiacm.task.concurrent;

import java.io.File;
import java.io.IOException;
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
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-741:在分中心A的2个节点并发迁移分中心B文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class Transfer_inDiffNode741 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmSession sessionA = null;
    private ScmWorkspace wsA1 = null;
    private ScmWorkspace wsA2 = null;

    private String author = "TD741";
    private List< ScmId > fileIdList1 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList2 = new ArrayList< ScmId >();
    private int fileSize = 10;
    private int fileNum = 100;
    private File localPath = null;
    private String filePath = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private List< WsWrapper > ws_TList = new ArrayList< WsWrapper >();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_TList = ScmInfo.getWss( 2 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_TList.get( 0 ), cond );
            ScmFileUtils.cleanFile( ws_TList.get( 1 ), cond );

            sessionA = TestScmTools.createSession( branceSite );
            wsA1 = ScmFactory.Workspace
                    .getWorkspace( ws_TList.get( 0 ).getName(), sessionA );
            wsA2 = ScmFactory.Workspace
                    .getWorkspace( ws_TList.get( 1 ).getName(), sessionA );

            this.writeFile( wsA1, fileIdList1 );
            this.writeFile( wsA2, fileIdList2 );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            StartTransferTaskFromNode1 tfWs1FromNode1 = new StartTransferTaskFromNode1();
            tfWs1FromNode1.start();

            StartTransferTaskFromNode2 tfWs2FromNode2 = new StartTransferTaskFromNode2();
            tfWs2FromNode2.start();

            if ( !( tfWs1FromNode1.isSuccess()
                    && tfWs2FromNode2.isSuccess() ) ) {
                Assert.fail( tfWs1FromNode1.getErrorMsg()
                        + tfWs2FromNode2.getErrorMsg() );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( wsA1, fileId, true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( wsA2, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void writeFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( author + "_" + i + UUID.randomUUID() );
            scmfile.setAuthor( author );
            fileIdList.add( scmfile.save() );
        }
    }

    private class StartTransferTaskFromNode1 extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmWorkspace ws = null;
            String wsName = ws_TList.get( 0 ).getName();
            List< ScmId > fileIdList = fileIdList1;
            try {
                session = TestScmTools.createSession( branceSite );
                ws = ScmFactory.Workspace.getWorkspace( wsName, session );

                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmId taskId = ScmSystem.Task.startTransferTask( ws,
                        condition );

                ScmTaskUtils.waitTaskFinish( session, taskId );

                // check task info
                ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
                Assert.assertEquals( taskInfo.getWorkspaceName(), wsName );
                Assert.assertEquals( taskInfo.getType(),
                        CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );

                SiteWrapper[] expSiteIdList = { rootSite, branceSite };
                ScmFileUtils.checkMetaAndData( ws_TList.get( 0 ), fileIdList,
                        expSiteIdList, localPath, filePath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class StartTransferTaskFromNode2 extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmWorkspace ws = null;
            String wsName = ws_TList.get( 1 ).getName();
            List< ScmId > fileIdList = fileIdList2;
            try {
                session = TestScmTools.createSession( branceSite );
                ws = ScmFactory.Workspace.getWorkspace( wsName, session );

                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmId taskId = ScmSystem.Task.startTransferTask( ws,
                        condition );

                ScmTaskUtils.waitTaskFinish( session, taskId );

                // check results
                ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
                Assert.assertEquals( taskInfo.getWorkspaceName(), wsName );
                Assert.assertEquals( taskInfo.getType(),
                        CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );

                SiteWrapper[] expSiteList = { rootSite, branceSite };
                ScmFileUtils.checkMetaAndData( ws_TList.get( 1 ), fileIdList,
                        expSiteList, localPath, filePath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
