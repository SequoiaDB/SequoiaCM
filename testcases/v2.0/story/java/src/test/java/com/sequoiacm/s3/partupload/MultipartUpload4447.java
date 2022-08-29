package com.sequoiacm.s3.partupload;

import java.io.File;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @description SCM-4447:指定有效的源文件key，复制分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4447 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String targetBucketName = "targetbucket4447";
    private String sourceBucketName = "sourcebucket4447";
    private String targetKey = "/aa/bb/targetobj4447";
    private String sourceKey = "/aa/bb/sourceobj4447";
    private int fileSize = 1024 * 1024 * 30;
    private long sourceObjectSize;
    private File localPath = null;
    private String filePath = null;
    private String uploadId = null;

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

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 目标key和源key相同
        uploadPartCopy( sourceBucketName, sourceKey, targetBucketName,
                sourceKey );
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

    public void uploadPartCopy( String sourceBucketName, String sourceKey,
            String targetBucketName, String targetKey ) throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, targetBucketName,
                targetKey );
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
}