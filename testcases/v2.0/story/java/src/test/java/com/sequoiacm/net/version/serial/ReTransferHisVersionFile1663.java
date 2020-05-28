package com.sequoiacm.net.version.serial;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content : the historyVersion file in both the sourceSite and the
 * targetSite, transfer the history version file again. testlink-case: SCM-1663
 *
 * @author wuyan
 * @Date 2018.06.07
 * @modify By wuyan
 * @modify Date 2018.07.26
 * @version 1.10
 */

public class ReTransferHisVersionFile1663 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private ScmSession sessionS = null;
    private ScmWorkspace wsS = null;
    private ScmSession sessionT = null;
    private ScmWorkspace wsT = null;
    private ScmId fileId = null;
    private ScmId taskId1 = null;
    private ScmId taskId2 = null;
    private boolean runSuccess = false;

    private String fileName = "versionfile1663";
    private String authorName = "author1663";
    private byte[] filedata = new byte[ 1024 * 80 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

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

        fileId = VersionUtils.createFileByStream( wsS, fileName, filedata,
                authorName );
        VersionUtils.updateContentByStream( wsS, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int historyVersion = 1;
        // Transfer the history file, is both on sourceSite and targetSite
        taskId1 = startTransferTaskByHistoryVerFile( wsS, sessionS );
        Collection< ScmFileLocation > firstSiteInfo = getSiteInfo(
                historyVersion );

        // Transfer the history file once again
        taskId2 = startTransferTaskByHistoryVerFile( wsS, sessionS );
        Collection< ScmFileLocation > secondSiteInfo = getSiteInfo(
                historyVersion );

        // check the siteinfo is the same
        Assert.assertEquals( firstSiteInfo.toString(),
                secondSiteInfo.toString(),
                "fisrt get siteList:" + firstSiteInfo.toString()
                        + " 2nd get siteList:" + secondSiteInfo.toString() );
        // check the history file data
        VersionUtils.CheckFileContentByStream( wsS, fileName, historyVersion,
                filedata );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestSdbTools.Task.deleteMeta( taskId1 );
                TestSdbTools.Task.deleteMeta( taskId2 );
                ScmFactory.File.deleteInstance( wsT, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionS != null ) {
                sessionS.close();
            }
            if ( sessionT != null ) {
                sessionT.close();
            }
        }
    }

    private ScmId startTransferTaskByHistoryVerFile( ScmWorkspace ws,
            ScmSession session ) throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmId taskId = ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_HISTORY, targetSite.getSiteName() );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
        return taskId;
    }

    private Collection< ScmFileLocation > getSiteInfo( int majorVersion )
            throws ScmException {
        // get the create and last access time
        ScmFile file = ScmFactory.File.getInstance( wsS, fileId, majorVersion,
                0 );
        Collection< ScmFileLocation > actSiteInfo = file.getLocationList();
        return actSiteInfo;
    }

}