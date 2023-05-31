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
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-6014:暂停版本控制后删除对象（不存在null版本），检查桶容量限额
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6014 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object6014";
    private String bucketName = "bucket6014";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectNum = 20;
    private int maxObjectSize = 20;
    private int objectNum = 20;
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
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );

        quotaConfig = ScmEnableBucketQuotaConfig.newBuilder( bucketName )
                .setMaxObjects( maxObjectNum ).setMaxSize( maxObjectSize + "m" )
                .build();
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
    }

    @Test
    public void test() throws Exception {
        // 高水位
        test1();
        // 低水位
        test2();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
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

    public void test1() throws ScmException, InterruptedException {
        preHighWaterLevel();
        // 删除指定版本
        s3Client.deleteObject( bucketName, keyName + 0 );

        // 用例中需等待缓存更新，周期为10s，等待2个周期
        Thread.sleep( 20000 );

        ScmBucketQuotaInfo bucketQuotaInfo = ScmFactory.Quota
                .getBucketQuota( session, bucketName );
        BucketQuotaUtils.checkQuotaInfo( bucketQuotaInfo, bucketName,
                maxObjectNum, maxObjectSize * fileSize, objectNum - 1,
                ( objectNum - 1 ) * fileSize );

        S3Utils.clearBucket( s3Client, bucketName );
    }

    public void test2() throws ScmException, InterruptedException {
        preLowWaterLevel();
        s3Client.deleteObject( bucketName, keyName + 0 );

        // 用例中需等待缓存更新，周期为10s，等待2个周期
        Thread.sleep( 20000 );

        ScmBucketQuotaInfo bucketQuotaInfo = ScmFactory.Quota
                .getBucketQuota( session, bucketName );
        BucketQuotaUtils.checkQuotaInfo( bucketQuotaInfo, bucketName,
                maxObjectNum, maxObjectSize * fileSize, objectNum - 2,
                ( objectNum - 2 ) * fileSize );
    }

    public void preHighWaterLevel() {
        for ( int i = 0; i < objectNum - 1; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
        }
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
    }

    public void preLowWaterLevel() throws ScmException {
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
        for ( int i = 0; i < objectNum - 2; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
        }
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
    }
}
