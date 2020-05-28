package com.sequoiacm.net.version;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
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
 * test content:update files exist on multiple sites , delete the file
 * testlink-case:SCM-1679
 *
 * @author wuyan
 * @Date 2018.06.11
 * @version 1.00
 */

public class DeleteUpdateScmFile1679 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 3;
    private List< SiteWrapper > branSites = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private ScmSession sessionC = null;
    private ScmWorkspace wsC = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "file1679";
    private int fileSize = 1024 * 800;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        branSites = ScmInfo.getBranchSites( branSitesNum );

        SiteWrapper rootSite = ScmInfo.getRootSite();
        sessionA = TestScmTools.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        sessionC = TestScmTools.createSession( branSites.get( 2 ) );
        wsC = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionC );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByFile( wsA, fileName, filePath );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        byte[] updateData1 = new byte[ 1024 * 1024 ];
        byte[] updateData2 = new byte[ 1024 * 50 ];
        byte[] updateData3 = new byte[ 1024 * 30 ];
        byte[] updateData4 = new byte[ 10 ];
        VersionUtils.updateContentByStream( wsA, fileId, updateData1 );
        VersionUtils.updateContentByStream( wsB, fileId, updateData2 );
        VersionUtils.updateContentByStream( wsC, fileId, updateData3 );
        VersionUtils.updateContentByStream( wsM, fileId, updateData4 );
        ScmFactory.File.deleteInstance( wsB, fileId, true );
        checkDeleteResult();
        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
            if ( sessionC != null ) {
                sessionC.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void checkDeleteResult() {
        try {
            ScmFactory.File.getInstanceByPath( wsA, fileName );
            Assert.fail( "get  file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                        + e.getMessage() );
            }
        }

        // get current and history version file
        int minVersion = 1;
        int maxVersion = 4;
        for ( int i = minVersion; i <= maxVersion; i++ ) {
            try {
                ScmFactory.File.getInstanceByPath( wsA, fileName, i, 0 );
                Assert.fail(
                        "get currentVersion file must bu fail!version=" + i );
            } catch ( ScmException e ) {
                if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                    Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                            + e.getMessage() );
                }
            }
        }

    }

}