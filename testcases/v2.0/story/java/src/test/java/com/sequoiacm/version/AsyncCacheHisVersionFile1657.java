package com.sequoiacm.version;

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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:update Content of the current scm file, than ayncCache the
 * history version file
 * testlink-case:SCM-1657
 *
 * @author wuyan
 * @Date 2018.06.05
 * @version 1.00
 */

public class AsyncCacheHisVersionFile1657 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "file1657";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsM, fileName, filedata );
        VersionUtils.updateContentByStream( wsM, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        asyncCacheCurrentVersionFile( historyVersion );

        //check the history file data and siteinfo
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, historyVersion,
                expHisSiteList.length );
        VersionUtils.checkSite( wsM, fileId, historyVersion, expHisSiteList );
        VersionUtils.CheckFileContentByStream( wsA, fileName, historyVersion,
                filedata );

        //check the currentVersion file only on the rootSite
        SiteWrapper[] expCurSiteList = { rootSite };
        VersionUtils.checkSite( wsA, fileId, currentVersion, expCurSiteList );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( wsA, fileId, true );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void asyncCacheCurrentVersionFile( int majorVersion ) {
        try {
            // cache
            ScmFactory.File.asyncCache( wsA, fileId, majorVersion, 0 );
        } catch ( ScmException e ) {
            Assert.fail( "asynccAche file fail!" + e.getErrorCode() +
                    e.getStackTrace() );
        }
    }
}