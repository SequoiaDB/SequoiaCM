package com.sequoiacm.s3.bucketQuota;

import java.io.File;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-6017:开启限额，上传对象触发水位切换 SCM-6018:开启限额，上传对象和删除对象大小和数量一致
 *              SCM-6019:开启限额，删除对象触发水位切换
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6017_6018_6019 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object6017";
    private String bucketName = "bucket6017";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectNum = 20;
    private int maxObjectSize = 20;
    private int objectNum = 20;

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
    }

    @Test
    public void test() throws Exception {
        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
            if ( i == objectNum - 3 ) {
                // 用例中需等待缓存更新，周期为10s，等待2个周期
                Thread.sleep( 20000 );
                ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota
                        .getBucketQuota( session, bucketName );
                BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName,
                        maxObjectNum, maxObjectSize * fileSize, objectNum - 2,
                        ( objectNum - 2 ) * fileSize );
            } else if ( i == objectNum - 2 ) {
                // 用例中需等待缓存更新，周期为10s，等待2个周期
                Thread.sleep( 20000 );
                ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota
                        .getBucketQuota( session, bucketName );
                BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName,
                        maxObjectNum, maxObjectSize * fileSize, objectNum - 1,
                        ( objectNum - 1 ) * fileSize );
            }
        }

        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.deleteObject( bucketName, keyName + i );
            // 用例中需等待缓存更新，周期为10s，等待2个周期
            Thread.sleep( 20000 );
            ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota
                    .getBucketQuota( session, bucketName );
            BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName,
                    maxObjectNum, maxObjectSize * fileSize, objectNum - i - 1,
                    ( objectNum - i - 1 ) * fileSize );
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
}
