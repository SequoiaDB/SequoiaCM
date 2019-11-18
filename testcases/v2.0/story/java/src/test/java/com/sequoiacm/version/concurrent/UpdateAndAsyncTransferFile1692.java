/**
 * 
 */
package com.sequoiacm.version.concurrent;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description UpdateAndAsyncTransferFile1692.java
 * @author luweikang
 * @date 2018年6月13日
 */
public class UpdateAndAsyncTransferFile1692 extends TestScmBase {
    private boolean runSuccess = false;
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1692";
    private byte[] filedata = new byte[1024 * 100];
    private byte[] updatedata = new byte[1024 * 1024 * 2];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );

    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        int historyVersion = 1;

        ScmFactory.File.asyncTransfer( wsA, fileId );

        UpdateFileThread updateFileThread = new UpdateFileThread();
        updateFileThread.start();

        int asyncFileVersion = VersionUtils.waitAsyncTaskFinished2( wsM, fileId, historyVersion, 2 );
        Assert.assertTrue( updateFileThread.isSuccess(), updateFileThread.getErrorMsg() );

        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsA, fileId, asyncFileVersion, expHisSiteList );
        if ( asyncFileVersion == historyVersion ) {
            VersionUtils.CheckFileContentByStream( wsM, fileName, asyncFileVersion, filedata );
        } else {
            VersionUtils.CheckFileContentByStream( wsM, fileName, asyncFileVersion, updatedata );
        }

        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    class UpdateFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            VersionUtils.updateContentByStream( wsA, fileId, updatedata );
        }

    }

}
