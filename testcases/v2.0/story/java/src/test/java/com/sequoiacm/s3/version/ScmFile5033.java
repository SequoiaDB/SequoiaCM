package com.sequoiacm.s3.version;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-5033:开启/暂停版本控制，异步迁移null版本文件
 * @author wuyan
 * @createDate 2020.08.01
 * @version v1.0
 */
public class ScmFile5033 extends TestScmBase {
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private boolean runSuccess = false;
    private ScmBucket scmBucket = null;
    private String bucketName = "bucket5033";
    private String fileName = "fileVersion5033";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];
    private byte[] updatedata1 = new byte[ 1024 * 300 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();

        sessionA = ScmSessionUtils.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, sessionA );
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, sessionM );
        S3Utils.clearBucket( sessionM, s3WorkSpaces, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( wsA, bucketName );
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
        scmBucket.enableVersionControl();
        ScmFileUtils.createFile( scmBucket, fileName, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = -2;
        asyncTransferCurrentVersionFile( historyVersion );

        // 检查null版本迁移到主中心
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsA, fileId, historyVersion, expHisSiteList );
        checkFile( sessionM, historyVersion, filedata );

        // 检查当前版本未迁移
        SiteWrapper[] expCurSiteList = { branSite };
        VersionUtils.checkSite( wsM, fileId, currentVersion, expCurSiteList );

        scmBucket.suspendVersionControl();
        ScmFileUtils.createFile( scmBucket, fileName, updatedata1 );
        int newCurrentVersion = -2;
        asyncTransferCurrentVersionFile( newCurrentVersion );
        // 检查最新null版本迁移到主中心
        SiteWrapper[] expNewCurSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsM, fileId, newCurrentVersion,
                expNewCurSiteList );
        checkFile( sessionM, newCurrentVersion, updatedata1 );
        // 原版本V2仍未迁移到主中心
        VersionUtils.checkSite( wsA, fileId, currentVersion, expCurSiteList );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( sessionM, bucketName );
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
        ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId, majorVersion,
                sitenums );
    }

    private void checkFile( ScmSession session, int version, byte[] filedata )
            throws Exception {
        ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                bucketName );
        ScmFile file = scmBucket.getFile( fileName, version, 0 );
        Assert.assertEquals( file.getMajorVersion(), version );
        Assert.assertEquals( file.getFileId(), fileId );
        S3Utils.checkFileContent( file, filedata );
    }

}