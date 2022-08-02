package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
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
 * test content:delete breakpoint file ,the file not uploaded
 * testlink-case:SCM-1388
 * 
 * @author wuyan
 * @Date 2018.05.18
 * @version 1.00
 */

public class DeleteBreakpointFile1388 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "breakpointfile1388";
    private int dataSize = 1024 * 200;
    private byte[] data = new byte[ dataSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        createBreakpointFile();
        deleteUploadFile();
    }

    @AfterClass
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        // create breakpointfile
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );

        new Random().nextBytes( data );
        int uploadSize = 1024 * 100;
        byte[] datapart = new byte[ uploadSize ];
        System.arraycopy( data, 0, datapart, 0, uploadSize );
        breakpointFile.incrementalUpload( new ByteArrayInputStream( datapart ),
                false );
    }

    private void deleteUploadFile() throws ScmException {
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        // check the delete result
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

}