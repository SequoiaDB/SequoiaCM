package com.sequoiacm.net.readcachefile.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.InputStreamType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine.SeekType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-261:多中心，并发指定不同偏移+长度跨中心读相同文件 1、分中心A写文件 2、并发指定不同偏移+长度跨中心读相同文件
 * @author huangxiaoni init
 * @date 2017.5.8
 */

public class DiffCenterReadFile261 extends TestScmBase {
    private static ScmSession sessionA = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmWorkspace wsA = null;

    private String fileName = "readcachefile261";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 4;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass()
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        sessionA = TestScmTools.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

        fileId = ScmFileUtils.create( wsA, fileName, filePath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        ReadFileFromM readFromM = new ReadFileFromM();
        readFromM.start( 20 );

        ReadFileFromA readFromA = new ReadFileFromA();
        readFromA.start( 20 );

        ReadFileFromB readFromB = new ReadFileFromB();
        readFromB.start( 20 );

        if ( !( readFromM.isSuccess() && readFromA.isSuccess()
                && readFromB.isSuccess() ) ) {
            Assert.fail( readFromM.getErrorMsg() + readFromA.getErrorMsg()
                    + readFromB.getErrorMsg() );
        }

        checkMetadataAndLobs();
        runSuccess = true;
    }

    @AfterClass()
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

    private class ReadFileFromM extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            OutputStream fos = null;
            ScmInputStream in = null;
            int seekSize = 0;
            int off = 1024 * 1023;
            int len = 1024 * 1023;
            try {
                // login
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );

                in = ScmFactory.File
                        .createInputStream( InputStreamType.SEEKABLE, scmfile );
                in.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
                fos = new FileOutputStream( new File( downloadPath ) );
                byte[] buffer = new byte[ off + len ];
                int curOff = 0;
                int curExpReadLen = 0;
                int curActReadLen = 0;
                int readSize = 0;
                while ( readSize < len ) {
                    curOff = off + readSize;
                    curExpReadLen = len - readSize;
                    curActReadLen = in.read( buffer, curOff, curExpReadLen );
                    if ( curActReadLen <= 0 ) {
                        break;
                    }
                    fos.write( buffer, off + readSize, curActReadLen );

                    readSize += curActReadLen;
                }
                fos.flush();

                // check content
                String tmpPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                TestTools.LocalFile.readFile( filePath, seekSize, len,
                        tmpPath );

                Assert.assertEquals( TestTools.getMD5( downloadPath ),
                        TestTools.getMD5( tmpPath ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DATA_IS_IN_USE
                        && e.getError() != ScmError.DATA_ERROR ) {
                    throw e;
                }
            } finally {
                if ( fos != null )
                    fos.close();
                if ( in != null )
                    in.close();
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
            ScmInputStream in = null;
            int seekSize = 1024 * 1023;
            int off = 1024 * 1023;
            int len = 1024 * 1023;
            try {
                // login
                session = TestScmTools.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );

                in = ScmFactory.File
                        .createInputStream( InputStreamType.SEEKABLE, scmfile );
                in.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
                fos = new FileOutputStream( new File( downloadPath ) );
                byte[] buffer = new byte[ off + len ];
                int curOff = 0;
                int curExpReadLen = 0;
                int curActReadLen = 0;
                int readSize = 0;
                while ( readSize < len ) {
                    curOff = off + readSize;
                    curExpReadLen = len - readSize;
                    curActReadLen = in.read( buffer, curOff, curExpReadLen );
                    if ( curActReadLen <= 0 ) {
                        break;
                    }
                    fos.write( buffer, off + readSize, curActReadLen );

                    readSize += curActReadLen;
                }
                fos.flush();

                // check content
                String tmpPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                TestTools.LocalFile.readFile( filePath, seekSize, len,
                        tmpPath );
                Assert.assertEquals( TestTools.getMD5( downloadPath ),
                        TestTools.getMD5( tmpPath ) );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DATA_IS_IN_USE
                        && e.getError() != ScmError.DATA_ERROR ) {
                    throw e;
                }
            } finally {
                if ( fos != null )
                    fos.close();
                if ( in != null )
                    in.close();
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
            ScmInputStream in = null;
            int seekSize = 1024 * 1025;
            int off = 1024 * 1025;
            int len = 1024 * 1024;
            try {
                // login
                session = TestScmTools.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );

                in = ScmFactory.File
                        .createInputStream( InputStreamType.SEEKABLE, scmfile );
                in.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
                fos = new FileOutputStream( new File( downloadPath ) );
                byte[] buffer = new byte[ off + len ];
                int curOff = 0;
                int curExpReadLen = 0;
                int curActReadLen = 0;
                int readSize = 0;
                while ( readSize < len ) {
                    curOff = off + readSize;
                    curExpReadLen = len - readSize;
                    curActReadLen = in.read( buffer, curOff, curExpReadLen );
                    if ( curActReadLen <= 0 ) {
                        break;
                    }
                    fos.write( buffer, off + readSize, curActReadLen );
                    readSize += curActReadLen;
                }
                fos.flush();

                // check content
                String tmpPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                TestTools.LocalFile.readFile( filePath, seekSize, len,
                        tmpPath );
                Assert.assertEquals( TestTools.getMD5( downloadPath ),
                        TestTools.getMD5( tmpPath ) );

            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DATA_IS_IN_USE
                        && e.getError() != ScmError.DATA_ERROR ) {
                    throw e;
                }
            } finally {
                if ( fos != null )
                    fos.close();
                if ( in != null )
                    in.close();
                if ( session != null )
                    session.close();
            }
        }
    }
}
