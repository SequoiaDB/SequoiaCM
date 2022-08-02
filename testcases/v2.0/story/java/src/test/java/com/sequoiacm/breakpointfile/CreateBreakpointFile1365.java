package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
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

/**
 * test content:create the same filename by breakpoint file testlink
 * case:seqDB-1365
 *
 * @author wuyan
 * @Date 2018.05.11
 * @version 1.00
 */

public class CreateBreakpointFile1365 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "breakpointfile1365";
    private String filename = "testfile1365";
    private int fileSize = 0;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        createBreakpointFile();
        createBreakpointfileByFileName();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            ScmFactory.BreakpointFile.deleteInstance( ws, filename );
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

    private void createBreakpointFile() {
        try {
            // create file
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .createInstance( ws, fileName );
            breakpointFile.upload( new File( filePath ) );

            // create the same file name
            ScmFactory.BreakpointFile.createInstance( ws, fileName );
            byte[] data = new byte[ 10 ];
            new Random().nextBytes( data );
            breakpointFile.upload( new ByteArrayInputStream( data ) );

        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }

        }
    }

    private void createBreakpointfileByFileName() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( filename );
            file.setContent( filePath );
            fileId = file.save();

            // create the same file name
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .createInstance( ws, filename );
            byte[] data = new byte[ 10 ];
            new Random().nextBytes( data );
            breakpointFile.upload( new ByteArrayInputStream( data ) );
            file.setContent( filename );
            file.save();

        } catch ( ScmException e ) {
            if ( ScmError.OPERATION_UNSUPPORTED != e.getError() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() + e.getErrorCode() );
            }
        }

    }

}