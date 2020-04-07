/**
 *
 */
package com.sequoiacm.net.version.concurrent;

import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description UpdateAndAsyncTransferFile1693.java
 * @author luweikang
 * @date 2018年6月13日
 * @modify Date 2018.07.30
 * @version 1.10
 */
public class UpdateAndAsyncTransferFile1693 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionS = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsS = null;
    private ScmWorkspace wsT = null;
    private ScmId fileId = null;
    private ScmBreakpointFile sbFile = null;

    private String fileName = "fileVersion1693";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getSortSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionS = TestScmTools.createSession( sourceSite );
        wsS = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionS );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );

        fileId = VersionUtils.createFileByStream( wsS, fileName, filedata );
        sbFile = VersionUtils
                .createBreakpointFileByStream( wsS, fileName, updatedata );

    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        int historyVersion = 1;

        ScmFactory.File.asyncTransfer( wsS, fileId );

        UpdateFileThread updateFileThread = new UpdateFileThread();
        updateFileThread.start();

        int asyncileVersion = VersionUtils
                .waitAsyncTaskFinished2( wsT, fileId, historyVersion, 2 );

        Assert.assertTrue( updateFileThread.isSuccess(),
                updateFileThread.getErrorMsg() );

        SiteWrapper[] expHisSiteList = { targetSite, sourceSite };
        VersionUtils.checkSite( wsS, fileId, asyncileVersion, expHisSiteList );
        if ( asyncileVersion == historyVersion ) {
            VersionUtils
                    .CheckFileContentByStream( wsT, fileName, asyncileVersion,
                            filedata );
        } else {
            VersionUtils
                    .CheckFileContentByStream( wsT, fileName, asyncileVersion,
                            updatedata );
        }

        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsT, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionS != null ) {
                sessionS.close();
            }
            if ( sessionT != null ) {
                sessionT.close();
            }
        }
    }

    class UpdateFileThread extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            Thread.sleep( 210 );
            ScmFile scmFile = ScmFactory.File.getInstance( wsS, fileId );
            scmFile.updateContent( sbFile );
        }

    }

}
