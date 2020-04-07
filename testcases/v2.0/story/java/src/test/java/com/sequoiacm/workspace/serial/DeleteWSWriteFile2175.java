/**
 *
 */
package com.sequoiacm.workspace.serial;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * test content:删除ws后继续对该ws写文件
 * testlink-case:SCM-2175
 *
 * @author luweikang
 * @Date 2018.06.21
 * @version 1.00
 */
public class DeleteWSWriteFile2175 extends TestScmBase {
    private static SiteWrapper siteA = null;
    private static SiteWrapper siteB = null;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;
    private String wsName = "ws2175";
    private String fileName = "file2175";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        siteA = ScmInfo.getRootSite();
        siteB = ScmInfo.getBranchSite();
        sessionA = TestScmTools.createSession( siteA );
        sessionB = TestScmTools.createSession( siteB );
        ScmWorkspaceUtil.deleteWs( wsName, sessionA );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int siteNum = ScmInfo.getAllSites().size();
        ScmWorkspaceUtil.createWS( sessionA, wsName, siteNum );
        ScmWorkspaceUtil.wsSetPriority( sessionB, wsName );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, sessionB );
        ScmWorkspaceUtil.deleteWs( wsName, sessionA );

        checkDeleteWs( wsName, sessionB );

        byte[] data = new byte[ 1024 ];
        new Random().nextBytes( data );
        InputStream is = new ByteArrayInputStream( data );
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setContent( is );
            file.save();
            Assert.fail( "ws is not exist, upload file should faild" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST &&
                    e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                throw e;
            }
        } finally {
            is.close();
        }
    }

    /**
     * @param wsName2
     * @param sessionB2
     * @throws InterruptedException
     * @throws ScmException
     */
    private void checkDeleteWs( String wsName, ScmSession session )
            throws InterruptedException, ScmException {
        for ( int i = 0; i < 300; i++ ) {
            Thread.sleep( 1000 );
            try {
                ScmFactory.Workspace.getWorkspace( wsName, session );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                    throw e;
                }
                TestSdbTools.Workspace.checkWsCs( wsName, session );
                return;
            }
        }
        Assert.fail( "delete ws is not done in 300 seconds" );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.Workspace.deleteWorkspace( sessionA, wsName, true );
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

}
