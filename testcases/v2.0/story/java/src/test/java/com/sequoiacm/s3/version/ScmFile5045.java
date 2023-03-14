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
 * @description SCM-5045 : 开启版本控制，异步迁移文件
 * @author wuyan
 * @createDate 2020.08.01
 * @version v1.0
 */
public class ScmFile5045 extends TestScmBase {
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmWorkspace wsA = null;
    private ScmId fileId = null;
    private boolean runSuccess = false;
    private ScmBucket scmBucket = null;
    private String bucketName = "bucket5045";
    private String fileName = "fileVersion5045";
    private byte[] filedata = new byte[ 1024 * 10 ];
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
        scmBucket = ScmFactory.Bucket.createBucket( wsM, bucketName );
        scmBucket.enableVersionControl();
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filedata );
        ScmFileUtils.createFile( scmBucket, fileName, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        asyncTransferVersionFile( currentVersion );

        // 当前版本迁移到分站点，检查存在主站点和分站点上
        SiteWrapper[] expCurSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsM, fileId, currentVersion, expCurSiteList );
        checkFile( sessionA, currentVersion, updatedata );

        // 检查历史版本未迁移
        SiteWrapper[] expHisSiteList = { rootSite };
        VersionUtils.checkSite( wsM, fileId, historyVersion, expHisSiteList );

        // 再次创建文件执行异步缓存
        int newCurrentVersion = 3;
        ScmFileUtils.createFile( scmBucket, fileName, updatedata1 );
        asyncCacheVersionFile( newCurrentVersion );
        VersionUtils.checkSite( wsM, fileId, newCurrentVersion,
                expCurSiteList );
        checkFile( sessionA, newCurrentVersion, updatedata1 );

        // 检查历史版本V1仍在分中心
        VersionUtils.checkSite( wsM, fileId, historyVersion, expHisSiteList );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( sessionM, bucketName );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void asyncTransferVersionFile( int majorVersion ) throws Exception {
        ScmFactory.File.asyncTransfer( wsM, fileId, branSite.getSiteName() );

        // wait task finished
        int sitenums = 2;
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, majorVersion,
                sitenums );
    }

    private void asyncCacheVersionFile( int majorVersion ) throws Exception {
        ScmFactory.File.asyncCache( wsA, fileId );
        int sitenums = 2;
        ScmTaskUtils.waitAsyncTaskFinished( wsM, fileId, majorVersion,
                sitenums );
    }

    private void checkFile( ScmSession session, int version, byte[] filedata )
            throws Exception {
        ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                bucketName );
        ScmFile file = scmBucket.getFile( fileName );
        Assert.assertEquals( file.getMajorVersion(), version );
        Assert.assertEquals( file.getFileId(), fileId );
        S3Utils.checkFileContent( file, filedata );
    }

}