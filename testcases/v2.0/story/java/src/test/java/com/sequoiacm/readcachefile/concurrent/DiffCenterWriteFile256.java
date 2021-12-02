package com.sequoiacm.readcachefile.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
 * @Testcase:SCM-256 多中心，并发在不同中心写文件 （A/B网络不通） 1、并发在所有中心写文件； 2、写完文件后跨中心读取文件；
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */

public class DiffCenterWriteFile256 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private List< ScmId > fileIdList = Collections
            .synchronizedList( new ArrayList< ScmId >() );
    private String fileName = "readcachefile256";
    private int fileSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;

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
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            session = TestScmTools.createSession( branSites.get( 0 ) );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            WriteFileFromM writeFromM = new WriteFileFromM();
            writeFromM.start( 15 );

            WriteFileFromA WriteFromA = new WriteFileFromA();
            WriteFromA.start( 15 );

            WriteFileFromB WriteFromB = new WriteFileFromB();
            WriteFromB.start( 15 );

            if ( !( writeFromM.isSuccess() && WriteFromA.isSuccess()
                    && WriteFromB.isSuccess() ) ) {
                Assert.fail( writeFromM.getErrorMsg() + WriteFromA.getErrorMsg()
                        + WriteFromB.getErrorMsg() );
            }
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
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private ScmId writeFile( SiteWrapper site ) {
        ScmSession ss = null;
        ScmId fileId = null;
        try {
            ss = TestScmTools.createSession( site );
            ScmWorkspace wks = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    ss );

            fileId = ScmFileUtils.create( wks,
                    fileName + "_" + UUID.randomUUID(), filePath );
            fileIdList.add( fileId );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( ss != null ) {
                ss.close();
            }
        }
        return fileId;
    }

    private void readFile( SiteWrapper site, ScmId fileId ) throws Exception {
        ScmSession ss = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            ss = TestScmTools.createSession( site );
            ScmWorkspace wks = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    ss );

            // read
            ScmFile file = ScmFactory.File.getInstance( wks, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            sis = ScmFactory.File.createInputStream( file );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis.read( fos );

            // check content
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ), "downloadPath:"
                            + downloadPath + " fileId:" + fileId.get() );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( ss != null )
                ss.close();
        }
    }

    private class WriteFileFromM extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            try {
                // write file from mainCenter
                ScmId fileId = writeFile( rootSite );

                // read file from subCenterA
                synchronized ( fileId ) {
                    readFile( branSites.get( 0 ), fileId );
                    // check meta data
                    SiteWrapper[] expSites = { rootSite, branSites.get( 0 ) };
                    ScmFileUtils.checkMetaAndData( wsp, fileId, expSites,
                            localPath, filePath );
                }
            } catch ( ScmException e ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class WriteFileFromA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            try {
                // write file from subCenterA
                ScmId fileId = writeFile( branSites.get( 0 ) );

                // read file from subCenterB
                synchronized ( fileId ) {
                    readFile( branSites.get( 1 ), fileId );
                    // check meta data
                    SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                            branSites.get( 1 ) };
                    ScmFileUtils.checkMetaAndData( wsp, fileId, expSites,
                            localPath, filePath );
                }
            } catch ( ScmException e ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class WriteFileFromB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            try {
                // write file from subCenterB
                ScmId fileId = writeFile( branSites.get( 1 ) );

                // read file from mainCenter
                synchronized ( fileId ) {
                    readFile( rootSite, fileId );
                    // check meta data
                    SiteWrapper[] expSites = { rootSite, branSites.get( 1 ) };
                    ScmFileUtils.checkMetaAndData( wsp, fileId, expSites,
                            localPath, filePath );
                }
            } catch ( ScmException e ) {
                Assert.fail( e.getMessage() );
            }
        }
    }
}
