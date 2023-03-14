package com.sequoiacm.version;

import java.util.Collection;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1654:主/分中心已存在历史版本文件，再次异步迁移历史版本文件
 * @author wuyan
 * @createDate 2018.06.05
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
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
    private boolean runSuccess = false;
    private String fileName = "fileVersion1654";
    private byte[] filedata = new byte[ 500 ];
    private byte[] updatedata = new byte[ 10 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        sessionA = ScmSessionUtils.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = ScmFileUtils.createFileByStream( wsA, fileName, filedata );
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
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
            }
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
        ScmFactory.File.asyncTransfer( wsA, fileId, majorVersion, 0,
                rootSite.getSiteName() );

        // wait task finished
        int sitenums = 2;
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, majorVersion,
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