package com.sequoiacm.net.version;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:the historyVersion file in both the rootSite and the branSite, 
 *               ayncTransfer the current version file.
 * testlink-case:SCM-1655
 *
 * @author wuyan
 * @Date 2018.06.05
 * @version 1.00
 */

public class AsyncTransferCurVersionFile1655 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper asyncTransferSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsT = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1655";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        // clean file
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        List< SiteWrapper > siteList = ScmNetUtils.getSortSites( wsp );
        asyncTransferSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionA = TestScmTools.createSession( asyncTransferSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionT = TestScmTools.createSession( targetSite );
        wsT = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionT );

        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
        VersionUtils.updateContentByStream( wsA, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        //rootSite exists the historyVersion file
        asyncTransferCurrentVersionFile( historyVersion );
        Collection< ScmFileLocation > getSiteInfo1 = getSiteInfo(
                historyVersion );

        //asyncTransfer the current version file
        asyncTransferCurrentVersionFile( currentVersion );

        //check the currentVersion file data and siteinfo
        SiteWrapper[] expCurSiteList = { targetSite, asyncTransferSite };
        VersionUtils.checkSite( wsA, fileId, currentVersion, expCurSiteList );
        VersionUtils.CheckFileContentByStream( wsT, fileName, currentVersion,
                updatedata );

        //check the historyVersion file sitelist, the sitelist no update
        Collection< ScmFileLocation > getSiteInfo2 = getSiteInfo(
                historyVersion );
        Assert.assertEquals( getSiteInfo1.toString(), getSiteInfo2.toString(),
                "fisrt get siteList:"
                        + getSiteInfo1.toString() + " 2nd get siteList:" +
                        getSiteInfo2.toString() );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( wsT, fileId, true );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionT != null ) {
                sessionT.close();
            }
        }
    }

    private void asyncTransferCurrentVersionFile( int majorVersion )
            throws Exception {
        ScmFactory.File.asyncTransfer( wsA, fileId, majorVersion, 0 );

        //wait task finished
        int sitenums = 2;
        VersionUtils
                .waitAsyncTaskFinished( wsT, fileId, majorVersion, sitenums );
    }

    private Collection< ScmFileLocation > getSiteInfo( int majorVersion )
            throws ScmException {
        //get the create and last access time
        ScmFile file = ScmFactory.File
                .getInstance( wsA, fileId, majorVersion, 0 );
        Collection< ScmFileLocation > actSiteInfo = file.getLocationList();
        return actSiteInfo;
    }

}