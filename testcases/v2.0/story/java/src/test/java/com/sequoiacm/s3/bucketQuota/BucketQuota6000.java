package com.sequoiacm.s3.bucketQuota;

import java.io.File;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.BucketQuotaUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-6000:指定不同限额状态的桶取消限额同步
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6000 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket6000";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectNum = 10;
    private int maxObjectSize = 10;
    private ScmEnableBucketQuotaConfig quotaConfig = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        quotaConfig = ScmEnableBucketQuotaConfig.newBuilder( bucketName )
                .setMaxObjects( maxObjectNum ).setMaxSize( maxObjectSize + "m" )
                .build();
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 未开启限额
        test1();

        // 开启限额时自动同步未结束
        test2();

        // 开启限额时自动同步已结束
        test3();

        // 手动同步未结束
        test4();

        // 手动同步已结束
        test5();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            s3Client.shutdown();
        }
    }

    public void test1() throws ScmException {
        try {
            ScmFactory.Quota.cancelSyncBucketQuota( session, bucketName );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }
    }

    public void test2() throws ScmException, InterruptedException {
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );

        ScmQuotaSyncStatus syncStatus = ScmFactory.Quota
                .getBucketQuota( session, bucketName ).getSyncStatus();
        if ( syncStatus.equals( ScmQuotaSyncStatus.SYNCING ) ) {
            ScmFactory.Quota.cancelSyncBucketQuota( session, bucketName );
            BucketQuotaUtils.checkSyncStatus( session, bucketName,
                    ScmQuotaSyncStatus.CANCELED );
        }
    }

    public void test3() throws ScmException, InterruptedException {
        ScmFactory.Quota.disableBucketQuota( session, bucketName );
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );

        ScmFactory.Quota.syncBucketQuota( session, bucketName );
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
        try {
            ScmFactory.Quota.cancelSyncBucketQuota( session, bucketName );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
    }

    public void test4() throws ScmException, InterruptedException {
        ScmFactory.Quota.syncBucketQuota( session, bucketName );
        ScmQuotaSyncStatus syncStatus = ScmFactory.Quota
                .getBucketQuota( session, bucketName ).getSyncStatus();
        if ( syncStatus.equals( ScmQuotaSyncStatus.SYNCING ) ) {
            ScmFactory.Quota.cancelSyncBucketQuota( session, bucketName );
            BucketQuotaUtils.checkSyncStatus( session, bucketName,
                    ScmQuotaSyncStatus.CANCELED );
        } else {
            BucketQuotaUtils.checkSyncStatus( session, bucketName,
                    ScmQuotaSyncStatus.COMPLETED );
        }
    }

    public void test5() throws ScmException, InterruptedException {
        ScmFactory.Quota.syncBucketQuota( session, bucketName );
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
        try {
            ScmFactory.Quota.cancelSyncBucketQuota( session, bucketName );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
    }
}
