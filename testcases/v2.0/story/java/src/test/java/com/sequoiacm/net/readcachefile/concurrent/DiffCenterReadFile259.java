package com.sequoiacm.net.readcachefile.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase:SCM-256 多中心，并发在不同中心读相同文件 （A/B网络不通） 1、在分中心A写文件； 2、在分中心B读取该文件；
 *                   3、不同中心跨中心读该文件；
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */
public class DiffCenterReadFile259 extends TestScmBase {
    private static ScmSession sessionA = null;
    private static String filePath = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmWorkspace wsA = null;
    private String fileName = "readcachefile259";
    private int fileSize = 1024 * 1024 * 2;
    private ScmId fileId = null;
    private File localPath = null;

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

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            fileId = ScmFileUtils.create( wsA, fileName, filePath );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            ReadFileFromM ReadFromM = new ReadFileFromM();
            ReadFromM.start( 20 );

            ReadFileFromB ReadFromB = new ReadFileFromB();
            ReadFromB.start( 20 );

            if ( !( ReadFromM.isSuccess() && ReadFromB.isSuccess() ) ) {
                Assert.fail(
                        ReadFromM.getErrorMsg() + ReadFromB.getErrorMsg() );
            }

            // check results
            SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                    branSites.get( 1 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private class ReadFileFromM extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                // login
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                sis = ScmFactory.File.createInputStream( file );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis.read( fos );
                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } catch ( Exception e ) {
                e.printStackTrace();
            } finally {
                if ( fos != null )
                    fos.close();
                if ( sis != null )
                    sis.close();
                if ( session != null )
                    session.close();
            }
        }
    }

    private class ReadFileFromB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                // login
                session = TestScmTools.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                sis = ScmFactory.File.createInputStream( file );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis.read( fos );
                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } finally {
                if ( fos != null )
                    fos.close();
                if ( sis != null )
                    sis.close();
                if ( session != null )
                    session.close();
            }
        }
    }
}
