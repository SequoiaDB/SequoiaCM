package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-747 : 在分中心A的2个节点并发清理不同分中心文件
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、在分中心A写入文件，多个ws均写入文件； 2、在主中心读取文件； 3、在分中心A的多个节点并发清理多个ws下的文件； 3、检查清理结果正确性；
 */

public class Clean_inDiffNode747 extends TestScmBase {
    private static ScmSession session1 = null;
    private static ScmSession session2 = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private ScmWorkspace ws2 = null;
    private List< ScmId > taskIdList = Collections
            .synchronizedList( new ArrayList< ScmId >() );
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = "CleanInDiffNode747";
    private int fileSize = 256 * 1024;
    private int fileNum = 16;
    private int startNum = 2;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();

    private SiteWrapper sourceSite1 = null;
    private SiteWrapper targetSite1 = null;
    private SiteWrapper sourceSite2 = null;
    private SiteWrapper targetSite2 = null;
    private List< WsWrapper > ws_TList = new ArrayList< WsWrapper >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            for ( int i = 0; i < fileNum; i++ ) {
                String filePath = localPath + File.separator + "localFile_"
                        + fileSize + i + ".txt";
                TestTools.LocalFile.createFile( filePath, fileSize + i );
                filePathList.add( filePath );
            }

            ws_TList = ScmInfo.getWss( 2 );
            List< SiteWrapper > siteList1 = ScmNetUtils
                    .getCleanSites( ws_TList.get( 0 ) );
            sourceSite1 = siteList1.get( 0 );
            targetSite1 = siteList1.get( 1 );
            List< SiteWrapper > siteList2 = ScmNetUtils
                    .getCleanSites( ws_TList.get( 1 ) );
            sourceSite2 = siteList2.get( 0 );
            targetSite2 = siteList2.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_TList.get( 0 ), cond );
            ScmFileUtils.cleanFile( ws_TList.get( 1 ), cond );

            session1 = TestScmTools.createSession( sourceSite1 );
            session2 = TestScmTools.createSession( sourceSite2 );
            ws = ScmFactory.Workspace.getWorkspace( ws_TList.get( 0 ).getName(),
                    session1 );
            ws2 = ScmFactory.Workspace
                    .getWorkspace( ws_TList.get( 1 ).getName(), session2 );

            readyFile( ws );
            transferFile( session1, ws, targetSite1.getSiteName() );
            readyFile( ws2 );
            transferFile( session2, ws2, targetSite2.getSiteName() );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            StartTaskInWs stWs = new StartTaskInWs();
            stWs.start();

            StartTaskInWs2 stWs2 = new StartTaskInWs2();
            stWs2.start();

            if ( !( stWs.isSuccess() && stWs2.isSuccess() ) ) {
                Assert.fail( stWs.getErrorMsg() + stWs2.getErrorMsg() );
            }

            checkMetaAndLobs();

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                /*
                 * for (int i = 0; i < fileIdList.size(); i++) { ScmId fileId =
                 * fileIdList.get(i); ScmFactory.File.deleteInstance(ws, fileId,
                 * true); }
                 */
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( ws_TList.get( 0 ), cond );
                ScmFileUtils.cleanFile( ws_TList.get( 1 ), cond );

                TestTools.LocalFile.removeFile( localPath );

                for ( ScmId taskId : taskIdList ) {
                    TestSdbTools.Task.deleteMeta( taskId );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
            if ( session2 != null ) {
                session2.close();
            }

        }
    }

    private void readyFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePathList.get( i ) );
            file.setFileName( author + "_" + i + UUID.randomUUID() );
            file.setAuthor( author );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void transferFile( ScmSession session, ScmWorkspace ws,
            String siteName ) throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
        ScmId taskId = ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_CURRENT, siteName );
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    private void checkMetaAndLobs() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmId fileId = fileIdList.get( i );
                String filePath = filePathList.get( i );
                List< ScmId > fileIdList1 = new ArrayList< ScmId >();
                fileIdList1.add( fileId );
                if ( i >= startNum ) {
                    SiteWrapper[] expSiteList = { targetSite1 };
                    ScmFileUtils.checkMetaAndData( ws_TList.get( 0 ),
                            fileIdList1, expSiteList, localPath, filePath );
                } else {
                    SiteWrapper[] expSiteList = { sourceSite1, targetSite1 };
                    ScmFileUtils.checkMetaAndData( ws_TList.get( 0 ),
                            fileIdList1, expSiteList, localPath, filePath );
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private class StartTaskInWs extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession ss = null;
            try {
                // login
                ss = TestScmTools.createSession( sourceSite1 );
                ScmWorkspace ws1 = ScmFactory.Workspace
                        .getWorkspace( ws_TList.get( 0 ).getName(), ss );

                // start task
                int value = fileSize + startNum;
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.SIZE )
                        .greaterThanEquals( value )
                        .and( ScmAttributeName.File.AUTHOR ).is( author ).get();
                ScmId taskId = ScmSystem.Task.startCleanTask( ws1, condition );
                taskIdList.add( taskId );

                // check task info
                ScmTask taskInfo = null;
                while ( true ) {
                    taskInfo = ScmSystem.Task.getTask( ss, taskId );
                    if ( taskInfo
                            .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ) {
                        break;
                    }
                    Thread.sleep( 200 );
                }
                Assert.assertEquals( taskInfo.getProgress(), 100 );
                Assert.assertEquals( taskInfo.getRunningFlag(),
                        CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
                Assert.assertEquals( taskInfo.getWorkspaceName(),
                        ws_TList.get( 0 ).getName() );
                Assert.assertEquals( taskInfo.getContent(), condition );
                Assert.assertNotNull( taskInfo.getStopTime() );
            } finally {
                if ( ss != null ) {
                    ss.close();
                }
            }
        }
    }

    private class StartTaskInWs2 extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession ss = null;
            try {
                // login
                ss = TestScmTools.createSession( sourceSite2 );
                ScmWorkspace ws2 = ScmFactory.Workspace
                        .getWorkspace( ws_TList.get( 1 ).getName(), ss );

                // start task
                int value = fileSize + startNum;
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.SIZE )
                        .greaterThanEquals( value )
                        .and( ScmAttributeName.File.AUTHOR ).is( author ).get();
                ScmId taskId = ScmSystem.Task.startCleanTask( ws2, condition );
                taskIdList.add( taskId );

                // check task info
                ScmTask taskInfo = null;
                while ( true ) {
                    taskInfo = ScmSystem.Task.getTask( ss, taskId );
                    if ( taskInfo
                            .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ) {
                        break;
                    }
                    Thread.sleep( 200 );
                }
                Assert.assertEquals( taskInfo.getProgress(), 100 );
                Assert.assertEquals( taskInfo.getRunningFlag(),
                        CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
                Assert.assertEquals( taskInfo.getWorkspaceName(),
                        ws_TList.get( 1 ).getName() );
                Assert.assertEquals( taskInfo.getContent(), condition );
                Assert.assertNotNull( taskInfo.getStopTime() );
            } finally {
                if ( ss != null ) {
                    ss.close();
                }
            }
        }
    }
}