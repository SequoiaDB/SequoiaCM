/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1397.java
 * @author luweikang
 * @date 2018年5月22日
 */
public class BreakpointFile1397 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String checkFilePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkFilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, IOException {

        BreakpointUtil.createBreakpointFile( ws, filePath, "TestFile1392_3",
                1024 * 512, ScmChecksumType.ADLER32 );
        BreakpointUtil.createBreakpointFile( ws, filePath, "TestFile1392_4",
                1024 * 4096, ScmChecksumType.CRC32 );

        CreateBreakpointFileThread createThread1 = new CreateBreakpointFileThread(
                "TestFile1392_1" );
        createThread1.start();
        CreateBreakpointFileThread createThread2 = new CreateBreakpointFileThread(
                "TestFile1392_2" );
        createThread2.start();

        UploadBreakpointFileThread thread1 = new UploadBreakpointFileThread(
                "TestFile1392_3" );
        thread1.start();
        UploadBreakpointFileThread thread2 = new UploadBreakpointFileThread(
                "TestFile1392_4" );
        thread2.start();

        createThread1.isSuccess();
        createThread2.isSuccess();
        thread1.isSuccess();
        thread2.isSuccess();

        BreakpointUtil.checkScmFile( ws, "TestFile1392_1", filePath,
                checkFilePath );
        BreakpointUtil.checkScmFile( ws, "TestFile1392_2", filePath,
                checkFilePath );
        BreakpointUtil.checkScmFile( ws, "TestFile1392_3", filePath,
                checkFilePath );
        BreakpointUtil.checkScmFile( ws, "TestFile1392_4", filePath,
                checkFilePath );
    }

    @AfterClass
    private void tearDown() {
        try {
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class UploadBreakpointFileThread extends TestThreadBase {
        private String name;

        public UploadBreakpointFileThread( String name ) {
            this.name = name;
        }

        @Override
        public void exec() throws Exception {
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( ws, name );
            breakpointFile.upload( new File( filePath ) );
        }
    }

    private class CreateBreakpointFileThread extends TestThreadBase {

        private String name;

        public CreateBreakpointFileThread( String name ) {
            this.name = name;
        }

        @Override
        public void exec() throws Exception {
            try {
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .createInstance( ws, name );
                breakpointFile.upload( new File( filePath ) );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

    }
}
