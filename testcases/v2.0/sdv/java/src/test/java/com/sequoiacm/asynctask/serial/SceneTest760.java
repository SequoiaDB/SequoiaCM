package com.sequoiacm.asynctask.serial;

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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-760 : 场景一至场景八并发（大并发）
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、如下场景并发： 场景一：分中心写文件； 场景二：主中心写文件； 场景三：分中心读取本地文件； 场景四：主中心读取本地文件； 场景五：跨中心读取文件；
 * 场景六：分中心A写入3个文件，分中心B读取该3个文件并写入2个新的文件，分中心A再次读取该5个文件； 场景七：文件只在主中心，分中心异步缓存该文件；
 * 场景八：文件只在分中心，分中心异步迁移该文件； 2、检查执行结果正确性；
 */

public class SceneTest760 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final String author = "file760";
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private List< String > filePathList1 = new ArrayList< String >();
    private List< String > filePathList2 = new ArrayList< String >();
    private List< String > filePathList3 = new ArrayList< String >();
    private List< String > filePathList4 = new ArrayList< String >();
    private List< String > filePathList5 = new ArrayList< String >();
    private List< String > filePathList6 = new ArrayList< String >();
    private List< String > filePathList7 = new ArrayList< String >();
    private List< String > filePathList8 = new ArrayList< String >();
    private List< ScmId > fileIdList1 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList2 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList3 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList4 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList5 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList6 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList7 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList8 = new ArrayList< ScmId >();
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        ScmSession ssM = null;
        ScmSession ssA = null;
        ScmSession ssB = null;
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );

            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSiteList = ScmInfo.getBranchSites( 2 );
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // prepare files for read, asyncTransfer and asyncCache threads.
            ssM = TestScmTools.createSession( rootSite );
            ssA = TestScmTools.createSession( branceSiteList.get( 0 ) );
            ssB = TestScmTools.createSession( branceSiteList.get( 1 ) );

            writeFile( ssB, fileIdList3, filePathList3, 100 );
            writeFile( ssM, fileIdList4, filePathList4, 100 );
            writeFile( ssA, fileIdList5, filePathList5, 100 );
            writeFile( ssM, fileIdList7, filePathList7, 100 );
            writeFile( ssB, fileIdList8, filePathList8, 100 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != ssM ) {
                ssM.close();
            }
            if ( null != ssA ) {
                ssA.close();
            }
            if ( null != ssB ) {
                ssB.close();
            }
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        try {
            SceneThread1 sThd1 = new SceneThread1();
            SceneThread2 sThd2 = new SceneThread2();
            SceneThread3 sThd3 = new SceneThread3();
            SceneThread4 sThd4 = new SceneThread4();
            SceneThread5 sThd5 = new SceneThread5();
            SceneThread6 sThd6 = new SceneThread6();
            SceneThread7 sThd7 = new SceneThread7();
            SceneThread8 sThd8 = new SceneThread8();

            sThd1.start();
            sThd2.start();
            sThd3.start();
            sThd4.start();
            sThd5.start();
            sThd6.start();
            sThd7.start();
            sThd8.start();

            Assert.assertTrue( sThd1.isSuccess(), sThd1.getErrorMsg() );
            Assert.assertTrue( sThd2.isSuccess(), sThd2.getErrorMsg() );
            Assert.assertTrue( sThd3.isSuccess(), sThd3.getErrorMsg() );
            Assert.assertTrue( sThd4.isSuccess(), sThd4.getErrorMsg() );
            Assert.assertTrue( sThd5.isSuccess(), sThd5.getErrorMsg() );
            Assert.assertTrue( sThd6.isSuccess(), sThd6.getErrorMsg() );
            Assert.assertTrue( sThd7.isSuccess(), sThd7.getErrorMsg() );
            Assert.assertTrue( sThd8.isSuccess(), sThd8.getErrorMsg() );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        ScmSession ss = null;
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ss = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), ss );
                for ( ScmId fileId : fileIdList1 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList2 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList3 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList4 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList5 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList6 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList7 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                for ( ScmId fileId : fileIdList8 ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != ss ) {
                ss.close();
            }

        }
    }

    private void writeFile( ScmSession ss, List< ScmId > fileIdList,
            List< String > filePathList, int writeFileNum )
            throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );
        for ( int i = 0; i < writeFileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setAuthor( author );
            scmfile.setFileName( author + "_" + UUID.randomUUID() + i );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
            filePathList.add( filePath );
        }
    }

    private void readFile( ScmSession ss, List< ScmId > fileIdList,
            List< String > filePathList ) throws ScmException {
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    ss );

            for ( int i = 0; i < fileIdList.size(); ++i ) {
                ScmFile scmfile = ScmFactory.File.getInstance( ws,
                        fileIdList.get( i ) );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                scmfile.getContent( downloadPath );

                Assert.assertEquals( TestTools.getMD5( filePathList.get( i ) ),
                        TestTools.getMD5( downloadPath ) );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    /**
     * 场景一：分中心写文件；
     */
    private class SceneThread1 extends TestThreadBase {
        private ScmSession ssA = null;

        @Override
        public void exec() throws Exception {
            try {
                ssA = TestScmTools.createSession( branceSiteList.get( 0 ) );
                writeFile( ssA, fileIdList1, filePathList1, 100 );
            } finally {
                if ( null != ssA ) {
                    ssA.close();
                }
            }
        }

    }

    /**
     * 场景二：主中心写文件；
     */
    private class SceneThread2 extends TestThreadBase {
        private ScmSession ssM = null;

        @Override
        public void exec() throws Exception {
            try {
                ssM = TestScmTools.createSession( rootSite );
                writeFile( ssM, fileIdList2, filePathList2, 100 );
            } finally {
                if ( null != ssM ) {
                    ssM.close();
                }
            }
        }

    }

    /**
     * 场景三：分中心读取本地文件；
     */
    private class SceneThread3 extends TestThreadBase {
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssB = TestScmTools.createSession( branceSiteList.get( 1 ) );
                readFile( ssB, fileIdList3, filePathList3 );
                SiteWrapper[] expSiteArr = { branceSiteList.get( 1 ) };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList3, expSiteArr,
                        localPath, filePath );
            } finally {
                if ( null != ssB ) {
                    ssB.close();
                }
            }
        }

    }

    /**
     * 场景四：主中心读取本地文件；
     */
    private class SceneThread4 extends TestThreadBase {
        private ScmSession ssM = null;

        @Override
        public void exec() throws Exception {
            try {
                ssM = TestScmTools.createSession( rootSite );
                readFile( ssM, fileIdList4, filePathList4 );
                SiteWrapper[] expSiteArr = { rootSite };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList4, expSiteArr,
                        localPath, filePath );
            } finally {
                if ( null != ssM ) {
                    ssM.close();
                }
            }
        }

    }

    /**
     * 场景五：跨中心读取文件；
     */
    private class SceneThread5 extends TestThreadBase {
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssB = TestScmTools.createSession( branceSiteList.get( 1 ) );
                readFile( ssB, fileIdList5, filePathList5 );
                SiteWrapper[] expSiteArr = { rootSite, branceSiteList.get( 0 ),
                        branceSiteList.get( 1 ) };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList5, expSiteArr,
                        localPath, filePath );
            } finally {
                if ( null != ssB ) {
                    ssB.close();
                }
            }
        }

    }

    /**
     * 场景六：分中心A写入3个文件，分中心B读取该3个文件并写入2个新的文件，分中心A再次读取该5个文件；
     */
    private class SceneThread6 extends TestThreadBase {
        private ScmSession ssA = null;
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssA = TestScmTools.createSession( branceSiteList.get( 0 ) );
                ssB = TestScmTools.createSession( branceSiteList.get( 1 ) );

                writeFile( ssA, fileIdList6, filePathList6, 30 );
                readFile( ssB, fileIdList6, filePathList6 );
                SiteWrapper[] expSiteArr = { rootSite, branceSiteList.get( 0 ),
                        branceSiteList.get( 1 ) };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList6, expSiteArr,
                        localPath, filePath );

                writeFile( ssB, fileIdList6, filePathList6, 20 );
                readFile( ssA, fileIdList6, filePathList6 );
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList6, expSiteArr,
                        localPath, filePath );
            } finally {
                if ( null != ssA ) {
                    ssA.close();
                }
                if ( null != ssB ) {
                    ssB.close();
                }
            }
        }

    }

    /**
     * 场景七：文件只在主中心，分中心异步缓存该文件；
     */
    private class SceneThread7 extends TestThreadBase {
        private ScmSession ssA = null;

        @Override
        public void exec() throws Exception {
            try {
                ssA = TestScmTools.createSession( branceSiteList.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), ssA );
                int expSiteNum = 2;
                for ( ScmId fileId : fileIdList7 ) {
                    ScmFactory.File.asyncCache( ws, fileId );
                    ScmTaskUtils.waitAsyncTaskFinished( ws, fileId,
                            expSiteNum );
                }
                SiteWrapper[] expSiteArr = { rootSite,
                        branceSiteList.get( 0 ) };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList7, expSiteArr,
                        localPath, filePath );
            } finally {
                if ( null != ssA ) {
                    ssA.close();
                }
            }
        }

    }

    /**
     * 场景八：文件只在分中心，分中心异步迁移该文件；
     */
    private class SceneThread8 extends TestThreadBase {
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssB = TestScmTools.createSession( branceSiteList.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), ssB );
                int expSiteNum = 2;
                for ( ScmId fileId : fileIdList8 ) {
                    ScmFactory.File.asyncTransfer( ws, fileId );
                    ScmTaskUtils.waitAsyncTaskFinished( ws, fileId,
                            expSiteNum );
                }
                SiteWrapper[] expSiteArr = { rootSite,
                        branceSiteList.get( 1 ) };
                ScmFileUtils.checkMetaAndData( ws_T, fileIdList8, expSiteArr,
                        localPath, filePath );
            } finally {
                if ( null != ssB ) {
                    ssB.close();
                }
            }
        }
    }
}