package com.sequoiacm.net.version.serial;

import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Transfer file, specify the fields in transfer condition are not
 * in the history table. the current version file transfer is test by
 * testcase-1660 testlink-case:SCM-1666
 *
 * @author wuyan
 * @Date 2018.06.08
 * @modify Date 2018.07.26
 * @version 1.10
 */

public class TransferFile1666 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionS = null;
    private ScmWorkspace wsS = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsT = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1666";
    private String authorName = "transfer1666";
    private byte[] writeData = new byte[ 1024 * 2 ];
    private byte[] updateData = new byte[ 1 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionS = TestScmTools.createSession( sourceSite );
        wsS = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionS );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );
        fileId = VersionUtils.createFileByStream( wsS, fileName, writeData,
                authorName );
        VersionUtils.updateContentByStream( wsS, fileId, updateData );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        startTransferTaskError( wsS, sessionS );

        // check the file siteinfo
        SiteWrapper[] expSiteList = { sourceSite };
        VersionUtils.checkSite( wsS, fileId, historyVersion, expSiteList );
        VersionUtils.checkSite( wsS, fileId, currentVersion, expSiteList );
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

    private void startTransferTaskError( ScmWorkspace ws, ScmSession session )
            throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();

        try {
            ScmSystem.Task.startTransferTask( ws, condition,
                    ScopeType.SCOPE_HISTORY, targetSite.getSiteName() );
            Assert.fail( "transfer file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                Assert.fail( "expErrorCode:-101  actError:" + e.getError()
                        + e.getMessage() );
            }
        }

        try {
            ScmSystem.Task.startTransferTask( ws, condition,
                    ScopeType.SCOPE_ALL );
            Assert.fail( "transfer file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                Assert.fail( "expErrorCode:-101  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

}