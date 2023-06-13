package com.sequoiacm.s3.bucketQuota;

import java.io.File;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.client.element.quota.ScmUpdateBucketQuotaConfig;
import com.sequoiacm.testcommon.scmutils.BucketQuotaUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-5995:指定不同限额配置更新限额
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota5995 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5995";
    private String keyName = "object5995";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectNum1 = 10;
    private int maxObjectSize1 = 10;
    private int maxObjectNum2 = 5;
    private int maxObjectSize2 = 5;
    private int objectNum1 = 10;
    private int objectNum2 = 5;

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
                .newBuilder( bucketName ).setMaxObjects( maxObjectNum2 )
                .setMaxSize( maxObjectSize2 + "m" ).build();
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
    }
    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ScmUpdateBucketQuotaConfig quotaConfig = ScmUpdateBucketQuotaConfig
                .newBuilder( bucketName ).setMaxSize( maxObjectSize1 + "m" )
                .build();
        ScmFactory.Quota.updateBucketQuota( session, quotaConfig );

        // 用例中需等待缓存更新，周期为10s，等待2个周期
        Thread.sleep( 20000 );
        ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );
        BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName, -1,
                maxObjectSize1 * fileSize, 0, 0 );
        checkPutObjectSuccessQuota( -1, maxObjectSize1, objectNum1 );
        checkPutObjectFailedQuota( -1, maxObjectSize1, objectNum1 );

        quotaConfig = ScmUpdateBucketQuotaConfig.newBuilder( bucketName )
                .setMaxObjects( maxObjectNum1 ).build();
        ScmFactory.Quota.updateBucketQuota( session, quotaConfig );

        // 用例中需等待缓存更新，周期为10s，等待2个周期
        Thread.sleep( 20000 );
        quotaInfo = ScmFactory.Quota.getBucketQuota( session, bucketName );
        BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName, maxObjectNum1,
                -1, 0, 0 );
        checkPutObjectSuccessQuota( maxObjectNum1, -1, objectNum1 );
        checkPutObjectFailedQuota( maxObjectNum1, -1, objectNum1 );

        quotaConfig = ScmUpdateBucketQuotaConfig.newBuilder( bucketName )
                .setMaxObjects( maxObjectNum2 )
                .setMaxSize( maxObjectSize2 + "m" ).build();
        ScmFactory.Quota.updateBucketQuota( session, quotaConfig );

        // 用例中需等待缓存更新，周期为10s，等待2个周期
        Thread.sleep( 20000 );
        quotaInfo = ScmFactory.Quota.getBucketQuota( session, bucketName );
        BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName, maxObjectNum2,
                maxObjectSize2 * fileSize, 0, 0 );
        checkPutObjectSuccessQuota( maxObjectNum2, maxObjectSize2, objectNum2 );
        checkPutObjectFailedQuota( maxObjectNum2, maxObjectSize2, objectNum2 );

        try {
            ScmUpdateBucketQuotaConfig.newBuilder( bucketName ).build();
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                throw e;
            }
        }

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

    public void checkPutObjectSuccessQuota( int maxObjectNum, int maxObjectSize,
            int objectNum ) throws ScmException {
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

    public void checkPutObjectFailedQuota( int maxObjectNum, int maxObjectSize,
            int objectNum ) throws ScmException {
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
