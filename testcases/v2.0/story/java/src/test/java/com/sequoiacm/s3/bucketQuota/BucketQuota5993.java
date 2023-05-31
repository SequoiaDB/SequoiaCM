package com.sequoiacm.s3.bucketQuota;

import java.io.File;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.common.ScmQuotaSyncStatus;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-5993:开启限额操作与限额管理操作组合验证
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota5993 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object5993";
    private String bucketName = "bucket5993";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectSize = 10;
    private int maxObjectNum = 10;
    private int objectNum = 10;
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

    @Test
    public void test() throws Exception {
        // test a : 开启桶限额后立即再次开启桶限额
        test1();

        // test b : 开启桶限额后立即获取桶限额信息
        test2();

        // test c : 开启桶限额后立即执行限额同步
        test3();

        // test d : 开启桶限额后立即执行取消限额同步
        test4();

        // test e : 开启桶限额后立即禁用限额
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

    public void checkQuota( int maxObjectNum, int maxObjectSize )
            throws ScmException {
        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
        }
        ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );
        BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName, maxObjectNum,
                maxObjectSize * fileSize, objectNum, objectNum * fileSize );

        try {
            s3Client.putObject( bucketName, keyName + objectNum,
                    new File( filePath ) );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getMessage().contains( "The bucket quota exceeded" ) ) {
                throw e;
            }
        }
        quotaInfo = ScmFactory.Quota.getBucketQuota( session, bucketName );
        BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName, maxObjectNum,
                maxObjectSize * fileSize, objectNum, objectNum * fileSize );

        S3Utils.deleteAllObjects( s3Client, bucketName );
    }

    public void test1() throws ScmException {
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
        try {
            ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }

        checkQuota( maxObjectNum, maxObjectSize );
        ScmFactory.Quota.disableBucketQuota( session, bucketName );
    }

    public void test2() throws ScmException {
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
        ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );
        BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName, maxObjectNum,
                maxObjectSize * fileSize, 0, 0 );
        ScmFactory.Quota.disableBucketQuota( session, bucketName );
    }

    public void test3() throws ScmException {
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
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
        ScmFactory.Quota.disableBucketQuota( session, bucketName );
    }

    public void test4() throws ScmException {
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
        ScmQuotaSyncStatus syncStatus = ScmFactory.Quota
                .getBucketQuota( session, bucketName ).getSyncStatus();
        if ( syncStatus.equals( ScmQuotaSyncStatus.SYNCING ) ) {
            ScmFactory.Quota.cancelSyncBucketQuota( session, bucketName );
            ScmBucketQuotaInfo bucketQuota = ScmFactory.Quota
                    .getBucketQuota( session, bucketName );
            Assert.assertEquals( bucketQuota.getSyncStatus(),
                    ScmQuotaSyncStatus.CANCELED );
        }

        ScmFactory.Quota.disableBucketQuota( session, bucketName );
    }

    public void test5() throws ScmException {
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
        ScmFactory.Quota.disableBucketQuota( session, bucketName );
    }
}
