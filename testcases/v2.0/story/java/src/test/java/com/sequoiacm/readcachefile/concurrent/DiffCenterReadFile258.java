package com.sequoiacm.readcachefile.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-258:多中心，不同中心并发读取分中心A的文件 1、分中心A写文件 2、不同中心并发读取分中心A的文件，覆盖所有中心
 * @author huangxiaoni init
 * @date 2017.5.8
 */

public class DiffCenterReadFile258 extends TestScmBase {
    private static ScmSession sessionA = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmWorkspace wsA = null;

    private String fileName = "readcachefile258";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    // readThreads涉及以下读取文件时，每个站点分配多少个读取线程同时读取。
    // 如果测试机器性能不理想，建议将这个值适当改小，保证大于3即可。
    private final int readThreads = 5;

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

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            fileId = ScmFileUtils.create( wsA, fileName, filePath );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite", GroupTags.base })
    private void test() {
        try {
            ReadFileFromMainCenter readFromM = new ReadFileFromMainCenter();
            readFromM.start( readThreads );

            ReadFileFromSubCenterA readFromA = new ReadFileFromSubCenterA();
            readFromA.start( readThreads );

            ReadFileFromSubCenterB readFromB = new ReadFileFromSubCenterB();
            readFromB.start( readThreads );

            if ( !( readFromM.isSuccess() && readFromA.isSuccess()
                    && readFromB.isSuccess() ) ) {
                Assert.fail( readFromM.getErrorMsg() + readFromA.getErrorMsg()
                        + readFromB.getErrorMsg() );
            }

            checkMetadataAndLobs();

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
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void checkMetadataAndLobs() throws Exception {
        try {
            SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                    branSites.get( 1 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void read( ScmFile file, String downloadPath )
            throws ScmException, IOException {
        ScmInputStream sis = null;
        OutputStream fos = null;
        try {
            sis = ScmFactory.File.createInputStream( file );
            fos = new FileOutputStream( downloadPath );
            sis.read( fos );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
        }
    }

    private class ReadFileFromMainCenter extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                read( file, downloadPath );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ReadFileFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                read( file, downloadPath );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ReadFileFromSubCenterB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                read( file, downloadPath );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
