package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @description SCM-5030:异步迁移历史版本文件
 * @author wuyan
 * @createDate 2020.08.01
 * @version v1.0
 */
public class ScmFile5030 extends TestScmBase {
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmWorkspace wsA = null;
    private ScmId fileId = null;
    private boolean runSuccess = false;
    private ScmBucket scmBucket = null;
    private String bucketName = "bucket5030";
    private String fileName = "fileVersion5030";
    private byte[] filedata = new byte[ 1024 * 1024 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, sessionM );
        S3Utils.clearBucket( sessionM, s3WorkSpaces, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( wsM, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, fileName, filedata );
        S3Utils.createFile( scmBucket, fileName, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        asyncTransferVersionFile( historyVersion );

        // 历史版本迁移到分站点，检查存在主站点和分站点上
        SiteWrapper[] expHisSiteList = { rootSite, branSite };
        VersionUtils.checkSite( wsM, fileId, historyVersion, expHisSiteList );
        checkFile( sessionA, historyVersion, filedata );

        // 当前版本只存在主站点上
        SiteWrapper[] expCurSiteList = { rootSite };
        VersionUtils.checkSite( wsM, fileId, currentVersion, expCurSiteList );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( sessionA, bucketName );
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

    private void asyncTransferVersionFile( int majorVersion ) throws Exception {
        ScmFactory.File.asyncTransfer( wsM, fileId, majorVersion, 0,
                branSite.getSiteName() );

        // wait task finished
        int sitenums = 2;
        VersionUtils.waitAsyncTaskFinished( wsM, fileId, majorVersion,
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