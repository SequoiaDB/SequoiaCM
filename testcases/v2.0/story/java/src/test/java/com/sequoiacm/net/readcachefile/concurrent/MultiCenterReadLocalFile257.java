package com.sequoiacm.net.readcachefile.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import com.sequoiacm.client.core.ScmInputStream;
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

/**
 * @Testcase:SCM-256 多中心，并发在本地读相同文件（A/B网络不通） 1、分别在各中心写文件；
 *                   2、并发在各中心本地读取相同文件，覆盖所有中心；
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */

public class MultiCenterReadLocalFile257 extends TestScmBase {
    private static String filePath = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private String fileName = "readcachefile257";
    private int fileSize = 1024 * 1024 * 2;
    private ScmId mFileId = null;
    private ScmId aFileId = null;
    private ScmId bFileId = null;
    private File localPath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            sessionB = TestScmTools.createSession( branSites.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

            mFileId = ScmFileUtils
                    .create( wsM, fileName + "_M" + UUID.randomUUID(),
                            filePath );
            aFileId = ScmFileUtils
                    .create( wsA, fileName + "_A" + UUID.randomUUID(),
                            filePath );
            bFileId = ScmFileUtils
                    .create( wsB, fileName + "_B" + UUID.randomUUID(),
                            filePath );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            ReadFileFromM readFromM = new ReadFileFromM();
            readFromM.start( 20 );

            ReadFileFromA readFromA = new ReadFileFromA();
            readFromA.start( 20 );

            ReadFileFromB readFromB = new ReadFileFromB();
            readFromB.start( 20 );

            if ( !( readFromM.isSuccess() && readFromA.isSuccess() &&
                    readFromB.isSuccess() ) ) {
                Assert.fail( readFromM.getErrorMsg() + readFromA.getErrorMsg() +
                        readFromB.getErrorMsg() );
            }

        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsM, mFileId, true );
                ScmFactory.File.deleteInstance( wsM, aFileId, true );
                ScmFactory.File.deleteInstance( wsM, bFileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null )
                sessionM.close();
            if ( sessionA != null )
                sessionA.close();
            if ( sessionB != null )
                sessionB.close();

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
                ScmFile file = ScmFactory.File.getInstance( ws, mFileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                sis = ScmFactory.File.createInputStream( file );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis.read( fos );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );

                // check meta data and lobs
                SiteWrapper[] expSites = { rootSite };
                ScmFileUtils
                        .checkMetaAndData( wsp, mFileId, expSites, localPath,
                                downloadPath );
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

    private class ReadFileFromA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                // login
                session = TestScmTools.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, aFileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                sis = ScmFactory.File.createInputStream( file );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis.read( fos );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );

                // check meta data and lobs
                SiteWrapper[] expSites = { branSites.get( 0 ) };
                ScmFileUtils
                        .checkMetaAndData( wsp, aFileId, expSites, localPath,
                                downloadPath );
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
                ScmFile file = ScmFactory.File.getInstance( ws, bFileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                sis = ScmFactory.File.createInputStream( file );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis.read( fos );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );

                // check meta data and lobs
                SiteWrapper[] expSites = { branSites.get( 1 ) };
                ScmFileUtils
                        .checkMetaAndData( wsp, bFileId, expSites, localPath,
                                downloadPath );
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
