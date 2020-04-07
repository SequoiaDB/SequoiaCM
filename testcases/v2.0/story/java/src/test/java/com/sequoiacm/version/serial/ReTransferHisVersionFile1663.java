package com.sequoiacm.version.serial;

import java.io.IOException;
import java.util.Collection;

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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content :the historyVersion file in both the rootSite and the branSite, 
 *               transfer the history version file again.
 * testlink-case:SCM-1663
 *
 * @author wuyan
 * @Date 2018.06.07
 * @version 1.00
 */

public class ReTransferHisVersionFile1663 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private ScmId taskId = null;
    private boolean runSuccess = false;

    private String fileName = "versionfile1663";
    private String authorName = "author1663";
    private byte[] filedata = new byte[ 1024 * 80 ];
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

        fileId = VersionUtils
                .createFileByStream( wsA, fileName, filedata, authorName );
        VersionUtils.updateContentByStream( wsA, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int historyVersion = 1;
        //asyncTransfer the  history file, is both on rootSite and branchSite
        AsyncTransferHisVersionFile( historyVersion );
        Collection< ScmFileLocation > firstSiteInfo = getSiteInfo(
                historyVersion );

        // Transfer the history file once again
        startTransferTaskByHistoryVerFile( wsA, sessionA );
        Collection< ScmFileLocation > secondSiteInfo = getSiteInfo(
                historyVersion );

        //check the siteinfo is the same
        Assert.assertEquals( firstSiteInfo.toString(),
                secondSiteInfo.toString(), "fisrt get siteList:"
                        + firstSiteInfo.toString() + " 2nd get siteList:" +
                        secondSiteInfo.toString() );
        //check the history file data
        VersionUtils.CheckFileContentByStream( wsA, fileName, historyVersion,
                filedata );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestSdbTools.Task.deleteMeta( taskId );
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
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

    private void AsyncTransferHisVersionFile( int majorVersion )
            throws Exception {
        // the first asyncCache history version file
        ScmFactory.File.asyncTransfer( wsA, fileId, majorVersion, 0 );
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, majorVersion,
                expHisSiteList.length );
    }

    private void startTransferTaskByHistoryVerFile( ScmWorkspace ws,
            ScmSession session ) throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        taskId = ScmSystem.Task
                .startTransferTask( ws, condition, ScopeType.SCOPE_HISTORY );

        //wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );
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