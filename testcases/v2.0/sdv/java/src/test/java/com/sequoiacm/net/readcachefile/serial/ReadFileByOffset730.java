package com.sequoiacm.net.readcachefile.serial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.InputStreamType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine.SeekType;
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
 * @Description: SCM-730 : 输出流方式分批读取文件（大并发） 1、在分中心写入文件，文件大小为5M；
 *               2、本地分中心输出流方式并发读取文件，并发数如500个并发； 3、检查文件元数据和内容正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class ReadFileByOffset730 extends TestScmBase {
    private static final String fileName = "ReadFileByOffset730";
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private int fileSize = 1024 * 1024 * 5;
    private int seekSize = 1024 * 1025;
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            branSite = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( branSite );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setFileName( fileName );
            file.setContent( filePath );
            fileId = file.save();
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            ReadFile rThread = new ReadFile();
            rThread.start( 100 );
            if ( !rThread.isSuccess() ) {
                Assert.fail( rThread.getErrorMsg() );
            }
            checkResult();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != sessionA ) {
                sessionA.close();
            }

        }
    }

    private void checkResult() {
        try {
            // check meta data
            SiteWrapper[] expSites = { branSite };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private class ReadFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            readFileFromSubCenterB();
        }

        public void readFileFromSubCenterB() throws ScmException {
            ScmSession session = null;
            ScmInputStream sis = null;
            try {
                // login
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read content
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );

                // FIXME: seek is forbidden. testcase has to be designed again.
                sis = ScmFactory.File
                        .createInputStream( InputStreamType.SEEKABLE, scmfile );
                sis.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
                String tmpPath;
                this.readScmFileByOff( sis, downloadPath );
                tmpPath = TestTools.LocalFile.initDownloadPath( localPath,
                        TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                TestTools.LocalFile.readFile( filePath, seekSize, tmpPath );

                Assert.assertEquals( TestTools.getMD5( downloadPath ),
                        TestTools.getMD5( tmpPath ) );
            } catch ( Exception e ) {
                Assert.fail( e.getMessage() );
            } finally {

                if ( sis != null )
                    sis.close();
                if ( session != null )
                    session.close();
            }
        }

        public void readScmFileByOff( ScmInputStream sisImpl,
                String downloadPath ) throws IOException {
            OutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(
                        new File( downloadPath ) );
                byte[] buffer = new byte[ fileSize ];
                while ( true ) {
                    int readSize = sisImpl.read( buffer, 0,
                            fileSize - seekSize );
                    if ( readSize <= 0 ) {
                        break;
                    }
                    fileOutputStream.write( buffer, 0, readSize );
                }
            } catch ( ScmException | IOException e ) {
                Assert.fail( e.getMessage() );
            } finally {
                fileOutputStream.close();
            }
        }
    }
}
