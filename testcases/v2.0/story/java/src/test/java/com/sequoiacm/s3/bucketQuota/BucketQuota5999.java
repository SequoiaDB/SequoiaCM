package com.sequoiacm.s3.bucketQuota;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.client.element.quota.ScmUpdateBucketQuotaConfig;
import com.sequoiacm.testcommon.TestTools;
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
import com.sequoiacm.testcommon.scmutils.S3Utils;

import java.io.File;

/**
 * @description SCM-5999:限额同步与限额管理操作组合验证
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota5999 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5999";
    private String keyName = "object5999";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectNum = 10;
    private int maxObjectSize = 10;
    private int objectNum = 10;

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
        ScmEnableBucketQuotaConfig quotaConfig = ScmEnableBucketQuotaConfig
                .newBuilder( bucketName ).setMaxObjects( maxObjectNum )
                .setMaxSize( maxObjectSize + "m" ).build();
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
    }

    @Test
    public void test() throws Exception {
        // 限额同步后立即执行限额同步
        test1();

        // 限额同步后立即获取桶限额信息
        test2();

        // 限额同步后立即更新限额
        test3();

        // 限额同步后立即取消限额同步
        test4();

        // 限额同步后立即禁用限额
        test5();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            s3Client.shutdown();
        }
    }

    public void checkQuotaInfo( ScmBucketQuotaInfo quotaInfo, int maxObjectNum,
            int maxObjectSize, ScmQuotaSyncStatus syncStatus ) {
        Assert.assertEquals( quotaInfo.getBucketName(), bucketName );
        Assert.assertEquals( quotaInfo.getUsedSizeBytes(), 0 );
        Assert.assertEquals( quotaInfo.getMaxObjects(), maxObjectNum );
        Assert.assertEquals( quotaInfo.getUsedSizeBytes(),
                maxObjectSize * fileSize );
        Assert.assertEquals( quotaInfo.getUsedObjects(), 0 );
        Assert.assertEquals( quotaInfo.getSyncStatus(), syncStatus );
        Assert.assertTrue( quotaInfo.isEnable() );
    }

    public void test1() throws ScmException, InterruptedException {
        ScmFactory.Quota.syncBucketQuota( session, bucketName );

        ScmQuotaSyncStatus syncStatus = ScmFactory.Quota
                .getBucketQuota( session, bucketName ).getSyncStatus();
        if ( syncStatus.equals( ScmQuotaSyncStatus.SYNCING ) ) {
            try {
                ScmFactory.Quota.syncBucketQuota( session, bucketName );
                Assert.fail( "预期失败，实际成功！" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                        .getErrorCode() ) {
                    throw e;
                }
            }
        }
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
    }

    public void test2() throws ScmException, InterruptedException {
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
        ScmFactory.Quota.syncBucketQuota( session, bucketName );

        ScmQuotaSyncStatus syncStatus = ScmFactory.Quota
                .getBucketQuota( session, bucketName ).getSyncStatus();
        if ( syncStatus.equals( ScmQuotaSyncStatus.SYNCING ) ) {
            ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota
                    .getBucketQuota( session, bucketName );
            checkQuotaInfo( quotaInfo, maxObjectNum, maxObjectSize,
                    ScmQuotaSyncStatus.SYNCING );
        }
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
    }

    public void test3() throws ScmException, InterruptedException {
        ScmFactory.Quota.syncBucketQuota( session, bucketName );
        ScmQuotaSyncStatus syncStatus = ScmFactory.Quota
                .getBucketQuota( session, bucketName ).getSyncStatus();
        if ( syncStatus.equals( ScmQuotaSyncStatus.SYNCING ) ) {
            ScmUpdateBucketQuotaConfig updateQuotaConfig = ScmUpdateBucketQuotaConfig
                    .newBuilder( bucketName ).setMaxObjects( maxObjectNum )
                    .setMaxSize( maxObjectSize + "m" ).build();
            ScmFactory.Quota.updateBucketQuota( session, updateQuotaConfig );
            ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota
                    .getBucketQuota( session, bucketName );
            checkQuotaInfo( quotaInfo, maxObjectNum, maxObjectSize,
                    ScmQuotaSyncStatus.SYNCING );
            checkPutObjectSuccessQuota( maxObjectNum, maxObjectSize );
            checkPutObjectFailedQuota( maxObjectNum, maxObjectSize );
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
        }
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
    }

    public void test5() throws ScmException, InterruptedException {
        ScmFactory.Quota.syncBucketQuota( session, bucketName );
        ScmQuotaSyncStatus syncStatus = ScmFactory.Quota
                .getBucketQuota( session, bucketName ).getSyncStatus();
        if ( syncStatus.equals( ScmQuotaSyncStatus.SYNCING ) ) {
            ScmFactory.Quota.disableBucketQuota( session, bucketName );
            BucketQuotaUtils.checkSyncStatus( session, bucketName,
                    ScmQuotaSyncStatus.CANCELED );
            boolean isEnable = ScmFactory.Quota
                    .getBucketQuota( session, bucketName ).isEnable();
            Assert.assertFalse( isEnable );
        }
        BucketQuotaUtils.checkSyncStatus( session, bucketName,
                ScmQuotaSyncStatus.COMPLETED );
    }

    public void checkPutObjectSuccessQuota( int maxObjectNum,
            int maxObjectSize ) throws ScmException {
        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
        }
        ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );
        if ( maxObjectSize == -1 ) {
            BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName,
                    maxObjectNum, maxObjectSize, objectNum,
                    objectNum * fileSize );
        } else {
            BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName,
                    maxObjectNum, maxObjectSize * fileSize, objectNum,
                    objectNum * fileSize );
        }

    }

    public void checkPutObjectFailedQuota( int maxObjectNum, int maxObjectSize )
            throws ScmException {
        try {
            s3Client.putObject( bucketName, keyName + objectNum,
                    new File( filePath ) );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getMessage().contains( "The bucket quota exceeded" ) ) {
                throw e;
            }
        }

        ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );

        if ( maxObjectSize == -1 ) {
            BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName,
                    maxObjectNum, maxObjectSize, objectNum,
                    objectNum * fileSize );
        } else {
            BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName,
                    maxObjectNum, maxObjectSize * fileSize, objectNum,
                    objectNum * fileSize );
        }

        S3Utils.deleteAllObjects( s3Client, bucketName );
    }
}
