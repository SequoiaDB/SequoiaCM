/**
 *
 */
package com.sequoiacm.version.concurrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description UpdateAndDownloadVersionFile1701.java
 * @author luweikang
 * @date 2018年6月19日
 */
public class UpdateAndDownloadVersionFile1701 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSiteA = null;
    private SiteWrapper branSiteB = null;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1701";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];
    private byte[] downloadData = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > siteList = new ArrayList< SiteWrapper >();
        siteList = ScmInfo.getBranchSites( 2 );
        branSiteA = siteList.get( 0 );
        branSiteB = siteList.get( 1 );
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSiteA );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( branSiteB );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );

    }

    @Test(groups = { "twoSite" })
    private void test() throws Exception {

        UpdateFileThread updateFileThread = new UpdateFileThread();
        updateFileThread.start();

        DownloadFileThread downloadFileThread = new DownloadFileThread();
        downloadFileThread.start();

        Assert.assertTrue( updateFileThread.isSuccess(),
                updateFileThread.getErrorMsg() );
        Assert.assertTrue( downloadFileThread.isSuccess(),
                downloadFileThread.getErrorMsg() );

        VersionUtils.assertByteArrayEqual( filedata, downloadData );
        VersionUtils.CheckFileContentByStream( wsA, fileName, 2, updatedata );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    class UpdateFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            VersionUtils.updateContentByStream( wsA, fileId, updatedata );
        }

    }

    class DownloadFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmFile file = ScmFactory.File.getInstance( wsB, fileId );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            file.getContent( outputStream );
            downloadData = outputStream.toByteArray();
            outputStream.close();
        }

    }
}
