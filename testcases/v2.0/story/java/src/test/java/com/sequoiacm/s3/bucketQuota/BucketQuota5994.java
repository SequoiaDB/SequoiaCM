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
 * @description SCM-5994:指定不同限额状态的桶更新限额
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota5994 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object5994";
    private String bucketName = "bucket5994";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectSize = 10;
    private int maxObjectNum = 10;
    private int objectNum = 10;
    private ScmUpdateBucketQuotaConfig quotaConfig = null;

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
        quotaConfig = ScmUpdateBucketQuotaConfig.newBuilder( bucketName )
                .setMaxObjects( maxObjectNum ).setMaxSize( maxObjectSize + "m" )
                .build();
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // test a : 未开启限额时更新限额
        test1();

        // test b : 开启限额时更新限额
        test2();

        // test c : 开启桶限额禁用限额后更新限额
        test3();
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
        try {
            ScmFactory.Quota.updateBucketQuota( session, quotaConfig );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }

        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
        }
        S3Utils.deleteAllObjects( s3Client, bucketName );
    }

    public void test2() throws ScmException {
        ScmEnableBucketQuotaConfig quotaConfig1 = ScmEnableBucketQuotaConfig
                .newBuilder( bucketName ).setMaxObjects( -1 ).setMaxSize( "-1" )
                .build();
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig1 );

        ScmFactory.Quota.updateBucketQuota( session, quotaConfig );

        checkQuota( maxObjectNum, maxObjectSize );
    }

    public void test3() throws ScmException {
        ScmFactory.Quota.disableBucketQuota( session, bucketName );
        try {
            ScmFactory.Quota.updateBucketQuota( session, quotaConfig );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_INTERNAL_SERVER_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }
        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
        }
    }

}
