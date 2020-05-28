package com.sequoiacm.version;

import java.io.IOException;
import java.util.Collection;

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
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:the historyVersion file in both the rootSite and the branSite,
 * ayncTransfer the history version file again. testlink-case:SCM-1654
 *
 * @author wuyan
 * @Date 2018.06.05
 * @version 1.00
 */

public class ReAsyncTransferHisVersionFile1654 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1654";
    private byte[] filedata = new byte[ 500 ];
    private byte[] updatedata = new byte[ 10 ];

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
        VersionUtils.updateContentByStream( wsA, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        // asyncTransfer history file
        asyncTransferCurrentVersionFile( historyVersion );
        Collection< ScmFileLocation > getSiteInfo1 = getSiteInfo(
                historyVersion );

        // asyncTransfer history file once again
        asyncTransferCurrentVersionFile( historyVersion );
        Collection< ScmFileLocation > getSiteInfo2 = getSiteInfo(
                historyVersion );

        // check the siteinfo is the same
        Assert.assertEquals( getSiteInfo1.toString(), getSiteInfo2.toString(),
                "fisrt get siteList:" + getSiteInfo1.toString()
                        + " 2nd get siteList:" + getSiteInfo2.toString() );

        // check the historyVersion file data and siteinfo
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsA, fileId, historyVersion, expHisSiteList );
        VersionUtils.CheckFileContentByStream( wsM, fileName, historyVersion,
                filedata );

        // check the currentVersion file only on the branSiteA
        SiteWrapper[] expCurSiteList = { branSite };
        VersionUtils.checkSite( wsA, fileId, currentVersion, expCurSiteList );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( wsA, fileId, true );
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

    private void asyncTransferCurrentVersionFile( int majorVersion )
            throws Exception {
        ScmFactory.File.asyncTransfer( wsA, fileId, majorVersion, 0 );

        // wait task finished
        int sitenums = 2;
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, majorVersion,
                sitenums );
    }

    private Collection< ScmFileLocation > getSiteInfo( int majorVersion )
            throws ScmException {
        // get the create and last access time
        ScmFile file = ScmFactory.File.getInstance( wsA, fileId, majorVersion,
                0 );
        Collection< ScmFileLocation > actSiteInfo = file.getLocationList();
        return actSiteInfo;
    }

}