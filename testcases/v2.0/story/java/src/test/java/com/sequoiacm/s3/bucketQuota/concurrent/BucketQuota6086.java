package com.sequoiacm.s3.bucketQuota.concurrent;

import java.io.File;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.BucketQuotaUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-6086:获取桶限额信息与上传对象并发
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6086 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object6086";
    private String bucketName = "bucket6086";
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
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new UploadFileThread() );
        t.addWorker( new GetBucketQuotaThread() );
        t.run();

        ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota( session,
                bucketName );
        BucketQuotaUtils.checkQuotaInfo( quotaInfo, bucketName, maxObjectNum,
                maxObjectSize * fileSize, objectNum, objectNum * fileSize );
        checkObjectContent();

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

    private class GetBucketQuotaThread {
        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            try ( ScmSession session = ScmSessionUtils
                    .createSession( rootSite )) {
                ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota
                        .getBucketQuota( session, bucketName );
                Assert.assertEquals( quotaInfo.getMaxObjects(), maxObjectNum );
                Assert.assertEquals( quotaInfo.getMaxSizeBytes(),
                        maxObjectNum * fileSize );
            }
        }
    }

    private class UploadFileThread {
        @ExecuteOrder(step = 1)
        private void run() {
            for ( int i = 0; i < objectNum; i++ ) {
                s3Client.putObject( bucketName, keyName + i,
                        new File( filePath ) );
            }
        }
    }

    public void checkObjectContent() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        for ( int i = 0; i < objectNum; i++ ) {
            ScmFile file = bucket.getFile( keyName + i );
            S3Utils.checkFileContent( file, filePath, localPath );
        }
    }
}
