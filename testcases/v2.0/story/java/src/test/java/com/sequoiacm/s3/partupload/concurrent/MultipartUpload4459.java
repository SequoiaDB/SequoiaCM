package com.sequoiacm.s3.partupload.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
 * @description SCM-4459:复制分段上传和查询分段上传并发
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4459 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String targetBucketName = "targetbucket4459";
    private String sourceBucketName = "sourcebucket4459";
    private String targetKey = "/aa/bb/targetobj4459";
    private String sourceKey = "/aa/bb/sourceobj4459";
    private int fileSize = 1024 * 1024 * 30;
    private long sourceObjectSize;
    private String uploadId;
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
        S3Utils.clearBucket( s3Client, sourceBucketName );
        s3Client.createBucket( targetBucketName );
        s3Client.createBucket( sourceBucketName );

        s3Client.putObject( sourceBucketName, sourceKey, new File( filePath ) );
        sourceObjectSize = s3Client.getObject( sourceBucketName, sourceKey )
                .getObjectMetadata().getContentLength();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, targetBucketName,
                targetKey );

        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadUploadPartCopy( sourceBucketName, sourceKey,
                targetBucketName, targetKey ) );
        es.addWorker( new ThreadListMultipartUploads( targetBucketName ) );
        es.run();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, targetBucketName );
                S3Utils.clearBucket( s3Client, sourceBucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    public void uploadPartCopy( AmazonS3 s3Client, String sourceBucketName,
            String sourceKey, String targetBucketName, String targetKey )
            throws Exception {
        List< PartETag > partEtags = PartUploadUtils.partUploadCopy( s3Client,
                sourceBucketName, sourceKey, targetBucketName, targetKey,
                uploadId, sourceObjectSize );
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

    public class ThreadListMultipartUploads {
        private String targetBucketName;

        public ThreadListMultipartUploads( String targetBucketName ) {
            this.targetBucketName = targetBucketName;
        }

        @ExecuteOrder(step = 1)
        public void putObject() throws Exception {
            AmazonS3 s3 = null;
            try {
                s3 = S3Utils.buildS3Client();
                ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                        targetBucketName );
                MultipartUploadListing result = s3
                        .listMultipartUploads( request );

                MultiValueMap< String, String > expUpload = new LinkedMultiValueMap<>();
                expUpload.add( targetKey, uploadId );
                List< String > expCommonPrefixes = new ArrayList<>();
                PartUploadUtils.checkListMultipartUploadsResults( result,
                        expCommonPrefixes, expUpload );
            } finally {
                if ( s3 != null ) {
                    s3.shutdown();
                }
            }
        }
    }
}