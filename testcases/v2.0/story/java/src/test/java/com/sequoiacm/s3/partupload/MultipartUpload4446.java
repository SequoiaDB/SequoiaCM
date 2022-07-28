package com.sequoiacm.s3.partupload;

import java.io.File;
import java.util.List;

import com.amazonaws.services.s3.model.AmazonS3Exception;
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

/**
 * @description SCM-4446:指定无效的源文件桶名，复制分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4446 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String targetBucketName = "targetbucket4446";
    private String sourceBucketName = "sourcebucket4446";
    private String targetKey = "/aa/bb/targetobj4446";
    private String sourceKey = "/aa/bb/sourceobj4446";
    private int fileSize = 1024 * 1024 * 30;
    private String uploadId = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, targetBucketName );
        S3Utils.clearBucket( s3Client, sourceBucketName );
        s3Client.createBucket( targetBucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, targetBucketName,
                targetKey );
        try {
            PartUploadUtils.partUploadCopy( s3Client, sourceBucketName,
                    sourceKey, targetBucketName, targetKey, uploadId,
                    fileSize );
            Assert.fail( "upload copy part must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket",
                    "---statuscode=" + e.getStatusCode() );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, targetBucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }
}