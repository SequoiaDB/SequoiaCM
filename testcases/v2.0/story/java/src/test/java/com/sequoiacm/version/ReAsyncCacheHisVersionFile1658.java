package com.sequoiacm.version;

import java.util.Collection;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1658:主/分中心已存在历史版本文件，再次异步缓存历史版本文件
 * @author wuyan
 * @createDate 2018.06.05
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class ReAsyncCacheHisVersionFile1658 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private boolean runSuccess = false;
    private String fileName = "file1658";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = VersionUtils.createFileByStream( wsM, fileName, filedata );
        VersionUtils.updateContentByStream( wsM, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int historyVersion = 1;
        // asyncCache history file
        AsyncCacheHisVersionFile( historyVersion );
        Collection< ScmFileLocation > firstSiteInfo = getSiteInfo(
                historyVersion );
        // asyncCache history file once again
        AsyncCacheHisVersionFile( historyVersion );
        Collection< ScmFileLocation > secondSiteInfo = getSiteInfo(
                historyVersion );

        // check the siteinfo is the same
        Assert.assertEquals( firstSiteInfo.toString(),
                secondSiteInfo.toString(),
                "fisrt get siteList:" + firstSiteInfo.toString()
                        + " 2nd get siteList:" + secondSiteInfo.toString() );
        // check the history file data
        VersionUtils.CheckFileContentByStream( wsA, fileName, historyVersion,
                filedata );
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

    private void AsyncCacheHisVersionFile( int majorVersion ) throws Exception {
        // the first asyncCache history version file
        ScmFactory.File.asyncCache( wsA, fileId, majorVersion, 0 );
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, majorVersion,
                expHisSiteList.length );
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