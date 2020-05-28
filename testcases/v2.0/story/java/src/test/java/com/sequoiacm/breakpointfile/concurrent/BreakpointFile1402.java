/**
 *
 */
package com.sequoiacm.breakpointfile.concurrent;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONException;
import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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

/**
 * @Description BreakpointFile.java 设置断点文件为文件内容时并发创建同名文件
 * @author luweikang
 * @date 2018年5月23日
 */
public class BreakpointFile1402 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean setSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1402";
    private int fileSize = 1024 * 1024 * 10;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private String checkFilePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkFilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws JSONException, ScmException,
            InterruptedException, IOException {

        this.CreateBreakpointFile();

        SetBreakpointFile2ScmFileThread setFileThread = new SetBreakpointFile2ScmFileThread();
        setFileThread.start();

        createScmFileThread createThread = new createScmFileThread();
        createThread.start();

        setSuccess = setFileThread.isSuccess();
        checkBreakpointFile( setSuccess, createThread.isSuccess() );

    }

    @AfterClass
    private void tearDown() {
        try {
            if ( !setSuccess ) {
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            }
            ScmFactory.File.deleteInstance( ws, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void CreateBreakpointFile() throws ScmException {

        ScmBreakpointFile breakpointFile;
        breakpointFile = ScmFactory.BreakpointFile.createInstance( ws, fileName,
                ScmChecksumType.ADLER32 );
        breakpointFile.upload( new File( filePath ) );
    }

    private void checkBreakpointFile( boolean setFileResult,
            boolean createResult ) throws ScmException, IOException {
        if ( setFileResult && !createResult ) {
            checkScmFile( false );
        } else if ( !setFileResult && createResult ) {
            checkScmFile( true );
        } else {
            Assert.fail( "All success" );
        }
    }

    private void checkScmFile( boolean fileIsNull )
            throws ScmException, IOException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        long fileSize = file.getSize();
        if ( fileIsNull ) {
            if ( fileSize != 0 ) {
                Assert.fail(
                        "breakpointFile change to ScmFile fail,fileSize should 0" );
            }
        } else {
            if ( fileSize == 0 ) {
                Assert.fail(
                        "breakpointFile change to ScmFile success,fileSize should not 0" );
            }
            file.getContent( checkFilePath );
            Assert.assertEquals( TestTools.getMD5( checkFilePath ),
                    TestTools.getMD5( filePath ),
                    "check breakpointFile to ScmFile" );
        }
    }

    private class SetBreakpointFile2ScmFileThread extends TestThreadBase {

        @Override
        public void exec() throws ScmException {
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( ws, fileName );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( breakpointFile );
            file.setFileName( fileName );
            file.setTitle( fileName );
            fileId = file.save();
        }

    }

    private class createScmFileThread extends TestThreadBase {

        @Override
        public void exec() throws ScmException {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setTitle( fileName );
            fileId = file.save();
        }
    }
}
