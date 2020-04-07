package com.sequoiacm.version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:specify that the breakpoint file update Content of the current
 *              scm file, the breakpoint file not uploaded. 
 * testlink-case:SCM-1643 * 
 * @author wuyan
 * @Date 2018.06.01
 * @version 1.00
 */

public class UpdateContentByBreakPointFile1643 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private String fileName = "file1643";
    private int breakpointfileSize = 1024 * 1024 * 3;
    private byte[] filedata = new byte[ 1024 * 500 ];
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        VersionUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + breakpointfileSize +
                        ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, breakpointfileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        fileId = VersionUtils.createFileByStream( ws, fileName, filedata );
        updateContentByBreakPointFile();

        int currentVersion = 1;
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                filedata );
        VersionUtils.checkFileCurrentVersion( ws, fileId, currentVersion );
        checkBreakPointFile();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void updateContentByBreakPointFile()
            throws ScmException, IOException {
        // create breakpointfile
        createBreakPointFile();
        // updataContent of file
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        try {
            file.updateContent( breakpointFile );
            Assert.fail(
                    "updateContent by  not uploaded breakpoint file must be " +
                            "fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                Assert.fail( "expErrorCode:-101  actError:" + e.getError() +
                        e.getMessage() );
            }
        }

    }

    private void createBreakPointFile() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        int uploadSize = 1024 * 51;
        InputStream inputStream = new BreakpointStream( filePath, uploadSize );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
    }

    private void checkBreakPointFile() throws ScmException {
        // check breakpointfile is exist
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertEquals( breakpointFile.getFileName(), fileName );
    }

    class BreakpointStream extends InputStream {

        private FileInputStream in = null;
        private int finishByteNum = 0;
        private int breakNum;

        public BreakpointStream( String filePath, int breakNum )
                throws FileNotFoundException {
            this.in = new FileInputStream( filePath );
            this.breakNum = breakNum;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public int read() throws IOException {
            int rs = in.read();
            if ( finishByteNum >= breakNum ) {
                rs = -1;
            }
            finishByteNum++;
            return rs;
        }
    }

}