package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:modify the contents of uploaded files, then breakpoint
 * continuation file testlink-case:SCM-1371
 * 
 * @author wuyan
 * @Date 2018.05.21
 * @version 1.00
 */

public class UploadBreakpointFile1371 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "breakpointfile1371";
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 5;
    private byte[] fileData = new byte[ fileSize ];
    private byte[] updateData = new byte[ updateSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        // randomly generated data
        new Random().nextBytes( fileData );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        createBreakpointFile();
        // test a: update the contents of uploaded file
        uploadFileContentUpdate();
        // test b: delete the part contents of uploaded file
        uploadFileContentdelete();
        // test c: append contents of uploaded file
        uploadFileAddContent();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        // create breakpointfile
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );
        System.arraycopy( fileData, 0, updateData, 0, updateSize );
        breakpointFile.incrementalUpload(
                new ByteArrayInputStream( updateData ), false );
    }

    private void uploadFileAddContent() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        int addLen = 1024 * 3;
        byte[] addFileContent = new byte[ addLen ];
        new Random().nextBytes( addFileContent );

        byte[] newData = new byte[ fileData.length + addLen ];
        System.arraycopy( fileData, 0, newData, 0, fileSize );
        System.arraycopy( addFileContent, 0, newData, fileSize, addLen );

        breakpointFile.upload( new ByteArrayInputStream( newData ) );
    }

    private void uploadFileContentUpdate() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        int updateLen = 1024 * 3;
        byte[] updateFileContent = new byte[ updateLen ];
        new Random().nextBytes( updateFileContent );
        byte[] testData = new byte[ fileSize ];
        System.arraycopy( fileData, 0, testData, 0, fileSize );
        System.arraycopy( updateFileContent, 0, testData, 1024 * 2, updateLen );

        try {
            breakpointFile.upload( new ByteArrayInputStream( testData ) );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.getStackTrace();
                Assert.fail( e.getMessage() + e.getErrorCode() );
            }
        }
    }

    private void uploadFileContentdelete() throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        int deleteLen = 1024 * 2;
        byte[] updateFile = Arrays.copyOfRange( fileData, deleteLen, fileSize );
        byte[] testdata = new byte[ updateSize ];
        System.arraycopy( updateFile, 0, testdata, 0, updateSize );

        // upload updatefile fail
        try {
            breakpointFile.upload( new ByteArrayInputStream( updateFile ) );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.getStackTrace();
                Assert.fail( e.getMessage() + e.getErrorCode() );
            }
        }
    }

}