package com.sequoiacm.s3.partupload.concurrent;

import java.io.File;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PartETag;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-4458:并发复制分段上传，不同的源对象，相同的目标对象
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4458 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String targetBucketName = "targetbucket4458";
    private String sourceBucketName1 = "sourcebucket4458a";
    private String sourceBucketName2 = "sourcebucket4458b";
    private String targetKey = "/aa/bb/targetobj4458";
    private String sourceKey = "/aa/bb/sourceobj4458";
    private int fileSize = 1024 * 1024 * 20;
    private File localPath = null;
    private String filePath = null;

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
        S3Utils.clearBucket( s3Client, targetBucketName );
        S3Utils.clearBucket( s3Client, sourceBucketName1 );
        S3Utils.clearBucket( s3Client, sourceBucketName2 );
        s3Client.createBucket( targetBucketName );
        s3Client.createBucket( sourceBucketName1 );
        s3Client.createBucket( sourceBucketName2 );
        s3Client.putObject( sourceBucketName1, sourceKey, new File( filePath ) );
        s3Client.putObject( sourceBucketName2, sourceKey, new File( filePath ) );

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadUploadPartCopy( sourceBucketName1, sourceKey,
                targetBucketName, targetKey ) );
        es.addWorker( new ThreadUploadPartCopy( sourceBucketName2, sourceKey,
                targetBucketName, targetKey ) );
        es.run();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, targetBucketName );
                S3Utils.clearBucket( s3Client, sourceBucketName1 );
                S3Utils.clearBucket( s3Client, sourceBucketName2 );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    public void uploadPartCopy( AmazonS3 s3Client, String sourceBucketName,
            String sourceKey, String targetBucketName, String targetKey )
            throws Exception {
        String uploadId = PartUploadUtils.initPartUpload( s3Client,
                targetBucketName, targetKey );
        List< PartETag > partEtags = PartUploadUtils.partUploadCopy( s3Client,
                sourceBucketName, sourceKey, targetBucketName, targetKey,
                uploadId, fileSize );
        PartUploadUtils.completeMultipartUpload( s3Client, targetBucketName,
                targetKey, uploadId, partEtags );

        // down file check the file content
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                targetBucketName, targetKey );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    public class ThreadUploadPartCopy {
        private String sourceBucketName;
        private String sourceKey;
        private String targetBucketName;
        private String targetKey;

        public ThreadUploadPartCopy( String sourceBucketName, String sourceKey,
                String targetBucketName, String targetKey ) {
            this.sourceBucketName = sourceBucketName;
            this.sourceKey = sourceKey;
            this.targetBucketName = targetBucketName;
            this.targetKey = targetKey;
        }

        @ExecuteOrder(step = 1)
        public void putObject() throws Exception {
            AmazonS3 s3 = null;
            try {
                s3 = S3Utils.buildS3Client();
                uploadPartCopy( s3, sourceBucketName, sourceKey,
                        targetBucketName, targetKey );
            } finally {
                if ( s3 != null ) {
                    s3.shutdown();
                }
            }
        }
    }
}