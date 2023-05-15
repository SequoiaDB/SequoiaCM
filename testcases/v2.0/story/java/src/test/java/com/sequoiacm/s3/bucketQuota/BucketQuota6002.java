package com.sequoiacm.s3.bucketQuota;

import java.io.File;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
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
 * @description SCM-6002:禁用版本控制，覆盖上传对象后检查桶容量限额
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6002 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object6002";
    private String bucketName = "bucket6002";
    private AmazonS3 s3Client = null;
    private int fileSize1 = 1024 * 1024;
    private int fileSize2 = 1024;
    private int fileSize3 = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private String filePath3 = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectNum = 20;
    private int maxObjectSize = 20;
    private int objectNum = 20;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize1
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_" + fileSize2
                + ".txt";
        filePath3 = localPath + File.separator + "localFile_" + fileSize3
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize1 );
        TestTools.LocalFile.createFile( filePath2, fileSize2 );
        TestTools.LocalFile.createFile( filePath3, fileSize3 );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ScmEnableBucketQuotaConfig quotaConfig = ScmEnableBucketQuotaConfig
                .newBuilder( bucketName ).setMaxObjects( maxObjectNum )
                .setMaxSize( maxObjectSize + "m" ).build();
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
        preHighWaterLevel();
        try {
            s3Client.putObject( bucketName, keyName + 0,
                    new File( filePath3 ) );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "quote" ) ) {
                throw e;
            }
        }
        ScmBucketQuotaInfo bucketQuotaInfo = ScmFactory.Quota
                .getBucketQuota( session, bucketName );
        BucketQuotaUtils.checkQuotaInfo( bucketQuotaInfo, bucketName,
                maxObjectNum, maxObjectSize * fileSize1, objectNum - 1,
                ( objectNum - 1 ) * fileSize1 );

        s3Client.putObject( bucketName, keyName + 0, new File( filePath2 ) );

        bucketQuotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );
        BucketQuotaUtils.checkQuotaInfo( bucketQuotaInfo, bucketName,
                maxObjectNum, maxObjectSize * fileSize1, objectNum - 1,
                ( objectNum - 2 ) * fileSize1 + fileSize2 );

        S3Utils.deleteAllObjects( s3Client, bucketName );
    }

    public void test2() throws ScmException {
        preLowWaterLevel();
        try {
            s3Client.putObject( bucketName, keyName + 0,
                    new File( filePath3 ) );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "quote" ) ) {
                throw e;
            }
        }
        ScmBucketQuotaInfo bucketQuotaInfo = ScmFactory.Quota
                .getBucketQuota( session, bucketName );
        BucketQuotaUtils.checkQuotaInfo( bucketQuotaInfo, bucketName,
                maxObjectNum, maxObjectSize * fileSize1, objectNum - 2,
                ( objectNum - 2 ) * fileSize1 );

        s3Client.putObject( bucketName, keyName + 0, new File( filePath2 ) );
        bucketQuotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );
        BucketQuotaUtils.checkQuotaInfo( bucketQuotaInfo, bucketName,
                maxObjectNum, maxObjectSize * fileSize1, objectNum - 2,
                ( objectNum - 3 ) * fileSize1 + fileSize2 );
        S3Utils.deleteAllObjects( s3Client, bucketName );
    }

    public void preHighWaterLevel() {
        for ( int i = 0; i < objectNum - 1; i++ ) {
            s3Client.putObject( bucketName, keyName + i,
                    new File( filePath1 ) );
        }
    }

    public void preLowWaterLevel() {
        for ( int i = 0; i < objectNum - 2; i++ ) {
            s3Client.putObject( bucketName, keyName + i,
                    new File( filePath1 ) );
        }
    }
}
