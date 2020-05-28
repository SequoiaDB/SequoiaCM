package com.sequoiacm.net.readcachefile.concurrent;

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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @FileName SCM-735 : 场景四（跨中心读文件）、场景五并发
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、场景四、场景五并发，不同文件； 场景四：文件在A中心，在B中心读取文件；
 * 场景五：文件在A中心，B中心读取文件后写入新的文件，分中心A再次读取所有文件； 2、检查场景并发后数据正确性；
 */

public class ComprehensiveReadFile735 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final String author = "file735";
    private boolean runSuccess = false;
    private SiteWrapper site1 = null;
    private SiteWrapper site2 = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private List< String > filePathList4 = new ArrayList< String >();
    private List< String > filePathList5 = new ArrayList< String >();
    private List< ScmId > fileIdList4 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList5 = new ArrayList< ScmId >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            wsp = ScmInfo.getWs();
            List< SiteWrapper > sites = ScmNetUtils.getAllSite( wsp );
            site1 = sites.get( 0 );
            site2 = sites.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        try {
            SceneThread4 sThd4 = new SceneThread4();
            SceneThread5 sThd5 = new SceneThread5();
            sThd4.start();
            sThd5.start();
            Assert.assertTrue( sThd4.isSuccess(), sThd4.getErrorMsg() );
            Assert.assertTrue( sThd5.isSuccess(), sThd5.getErrorMsg() );
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
                ss = TestScmTools.createSession( site1 );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), ss );
                for ( ScmId fileId : fileIdList4 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList5 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
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
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
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
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
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
     * 场景四：文件在A中心，在B中心读取文件；
     */
    private class SceneThread4 extends TestThreadBase {
        private ScmSession ssA = null;
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssA = TestScmTools.createSession( site1 );
                ssB = TestScmTools.createSession( site2 );
                writeFile( ssA, fileIdList4, filePathList4, 40 );
                readFile( ssB, fileIdList4, filePathList4 );
                SiteWrapper[] expSites = { site1, site2 };
                ScmFileUtils.checkMetaAndData( wsp, fileIdList4, expSites,
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
     * 场景五：文件在A中心，B中心读取文件后写入新的文件，分中心A再次读取所有文件；
     */
    private class SceneThread5 extends TestThreadBase {
        private ScmSession ssA = null;
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssA = TestScmTools.createSession( site1 );
                ssB = TestScmTools.createSession( site2 );
                writeFile( ssA, fileIdList5, filePathList5, 20 );
                readFile( ssB, fileIdList5, filePathList5 );
                // checkMetaAndLob(fileIdList5, filePathList5);
                SiteWrapper[] expSites = { site1, site2 };
                ScmFileUtils.checkMetaAndData( wsp, fileIdList5, expSites,
                        localPath, filePath );

                writeFile( ssB, fileIdList5, filePathList5, 20 );
                readFile( ssA, fileIdList5, filePathList5 );
                // checkMetaAndLob(fileIdList5, filePathList5);
                SiteWrapper[] expSites1 = { site1, site2 };
                ScmFileUtils.checkMetaAndData( wsp, fileIdList5, expSites1,
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
}