package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.SkipException;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:delete breakpointfile by other site testlink-case:SCM-1390
 * 
 * @author wuyan
 * @Date 2018.05.15
 * @version 1.00
 */

public class DeleteBreakpointFile1390 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private List< SiteWrapper > branSites = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "breakpointfile1390";
    private int fileSize = 1024 * 10;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        DBSites.remove( 0 );
        if ( DBSites.size() < 2 ) {
            throw new SkipException( "need two DBSites, skip!" );
        }

        branSites = DBSites;
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        createBreakpointFile();
        deleteBreakpointfile();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( wsA, fileName );
            TestTools.LocalFile.removeFile( localPath );

        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        // create file
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( wsA, fileName, checksumType );
        breakpointFile.upload( new File( filePath ) );
    }

    private void deleteBreakpointfile() throws ScmException, IOException {

        // delete the breakpointfile by other site
        try {
            ScmFactory.BreakpointFile.deleteInstance( wsB, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() + e.getError() );
            }
        }

        // check the breakpointfile exist
        Assert.assertNotNull(
                ScmFactory.BreakpointFile.getInstance( wsA, fileName ),
                "file exist!" );
    }
}