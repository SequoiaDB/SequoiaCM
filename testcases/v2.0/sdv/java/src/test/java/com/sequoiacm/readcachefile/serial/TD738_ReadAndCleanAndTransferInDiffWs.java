package com.sequoiacm.readcachefile.serial;

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
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-738:多个ws下并发读取文件、清理文件、迁移文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class TD738_ReadAndCleanAndTransferInDiffWs extends TestScmBase {
    private final int branSitesNum = 2;
    private final int wsNum = 3;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private List< WsWrapper > wsList = null;
    private String author = "TD738";
    private List< ScmId > fileIdList1 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList2 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList3 = new ArrayList< ScmId >();
    private int fileSize = 10;
    private int fileNum = 100;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsList = ScmInfo.getWss( wsNum );

            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            CrossReadFileInWs readInWs = new CrossReadFileInWs();
            readInWs.start();

            StartTransferTaskInWs2 transferInWs2 = new StartTransferTaskInWs2();
            transferInWs2.start();

            StartCleanTaskInWs3 cleanInWs3 = new StartCleanTaskInWs3();
            cleanInWs3.start();

            if ( !( readInWs.isSuccess() && transferInWs2.isSuccess()
                    && cleanInWs3.isSuccess() ) ) {
                Assert.fail(
                        readInWs.getErrorMsg() + transferInWs2.getErrorMsg()
                                + cleanInWs3.getErrorMsg() );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmSession sessionM = null;
        try {
            if ( runSuccess || forceClear ) {
                sessionM = TestScmTools.createSession( rootSite );

                ScmWorkspace ws1 = ScmFactory.Workspace
                        .getWorkspace( wsList.get( 0 ).getName(), sessionM );
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.deleteInstance( ws1, fileId, true );
                }

                ScmWorkspace ws2 = ScmFactory.Workspace
                        .getWorkspace( wsList.get( 1 ).getName(), sessionM );
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.deleteInstance( ws2, fileId, true );
                }

                ScmWorkspace ws3 = ScmFactory.Workspace
                        .getWorkspace( wsList.get( 2 ).getName(), sessionM );
                for ( ScmId fileId : fileIdList3 ) {
                    ScmFactory.File.deleteInstance( ws3, fileId, true );
                }

                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }

        }
    }

    private void writeFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( author + i + UUID.randomUUID() );
            scmfile.setAuthor( author );
            fileIdList.add( scmfile.save() );
        }
    }

    private void readFile( ScmWorkspace ws, List< ScmId > fileIdList )
            throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        }
    }

    private class CrossReadFileInWs extends TestThreadBase {
        private WsWrapper wsp = wsList.get( 0 );

        /**
         * write scmfile from centerA
         *
         * @throws ScmException
         */
        public CrossReadFileInWs() throws ScmException {
            ScmSession sessionA = null;
            try {
                sessionA = TestScmTools.createSession( branSites.get( 0 ) );
                ScmWorkspace wsA = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionA );
                writeFile( wsA, fileIdList1 );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }

        }

        /**
         * read scmfile from centerB
         */
        @Override
        public void exec() throws Exception {
            ScmSession sessionB = null;
            try {
                sessionB = TestScmTools.createSession( branSites.get( 1 ) );
                ScmWorkspace wsB = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionB );
                readFile( wsB, fileIdList1 );

                // check results
                SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                        branSites.get( 1 ) };
                ScmFileUtils.checkMetaAndData( wsp, fileIdList1, expSites,
                        localPath, filePath );
                System.out.println( "read is ok." );
            } finally {
                if ( sessionB != null ) {
                    sessionB.close();
                }
            }
        }
    }

    private class StartTransferTaskInWs2 extends TestThreadBase {
        private WsWrapper wsp = wsList.get( 1 );
        private ScmSession sessionA = null;
        private ScmWorkspace wsA = null;

        /**
         * write scmfile from centerA
         *
         * @throws ScmException
         */
        public StartTransferTaskInWs2() throws ScmException {
            try {
                sessionA = TestScmTools.createSession( branSites.get( 0 ) );
                wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                        sessionA );
                writeFile( wsA, fileIdList2 );
            } catch ( ScmException e ) {
                throw e;
            }

        }

        /**
         * transfer scmfile from centerA
         */
        @Override
        public void exec() throws Exception {
            try {
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmId taskId = ScmSystem.Task.startTransferTask( wsA,
                        condition );

                // check task info
                ScmTask taskInfo = null;
                while ( true ) {
                    taskInfo = ScmSystem.Task.getTask( sessionA, taskId );
                    if ( taskInfo
                            .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ) {
                        break;
                    }
                    Thread.sleep( 200 );
                }

                // check results
                Assert.assertEquals( taskInfo.getWorkspaceName(),
                        wsp.getName() );
                Assert.assertEquals( taskInfo.getType(),
                        CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );

                SiteWrapper[] expSites = { rootSite, branSites.get( 0 ) };
                ScmFileUtils.checkMetaAndData( wsp, fileIdList2, expSites,
                        localPath, filePath );
                System.out.println( "transfer is ok." );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }

    private class StartCleanTaskInWs3 extends TestThreadBase {
        private WsWrapper wsp = wsList.get( 2 );
        // private NodeWrapper node = branSites.get(0).getNode();
        private ScmSession sessionA = null;
        private ScmWorkspace wsA = null;

        /**
         * step1.write scmfile from centerA step2.read scmfile from centerB
         *
         * @throws Exception
         */
        public StartCleanTaskInWs3() throws Exception {
            try {
                sessionA = TestScmTools.createSession( branSites.get( 0 ) );
                wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                        sessionA );
                writeFile( wsA, fileIdList3 );
            } catch ( ScmException e ) {
                throw e;
            }

            ScmSession sessionM = null;
            try {
                sessionM = TestScmTools.createSession( rootSite );
                ScmWorkspace wsM = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), sessionM );
                readFile( wsM, fileIdList3 );
            } finally {
                if ( sessionM != null ) {
                    sessionM.close();
                }
            }

        }

        /**
         * clean scmfile from centerA
         */
        @Override
        public void exec() throws Exception {
            try {
                BSONObject condition = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmId taskId = ScmSystem.Task.startCleanTask( wsA, condition );

                // check task info
                ScmTask taskInfo = null;
                while ( true ) {
                    taskInfo = ScmSystem.Task.getTask( sessionA, taskId );
                    if ( taskInfo
                            .getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH ) {
                        break;
                    }
                    Thread.sleep( 200 );
                }

                // check results
                Assert.assertEquals( taskInfo.getWorkspaceName(),
                        wsp.getName() );
                Assert.assertEquals( taskInfo.getType(),
                        CommonDefine.TaskType.SCM_TASK_CLEAN_FILE );

                SiteWrapper[] expSites = { rootSite };
                ScmFileUtils.checkMetaAndData( wsp, fileIdList3, expSites,
                        localPath, filePath );
                System.out.println( "clean is ok." );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }

}
