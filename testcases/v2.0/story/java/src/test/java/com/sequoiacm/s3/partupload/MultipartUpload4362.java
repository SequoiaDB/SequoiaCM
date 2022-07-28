package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.util.List;

/**
 * @description SCM-4362:相同key不同uploadId上传多个分段，终止分段上传后再次分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4362 extends TestScmBase {
    private boolean runSuccess = false;
    private final String keyName = "/aa/object4362";
    private final String bucketName = "bucket4362";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private String filePath3 = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        int fileSize1 = 1024 * 1024 * 15;
        filePath1 = localPath + File.separator + "localFile_" + fileSize1
                + ".txt";
        int fileSize2 = 1024 * 1024 * 10;
        filePath2 = localPath + File.separator + "localFile_" + fileSize2
                + ".txt";
        int fileSize3 = 1024 * 1024 * 20;
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
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void abortMultipartUpload() throws Exception {
        File file1 = new File( filePath1 );
        File file2 = new File( filePath2 );
        File file3 = new File( filePath3 );
        String uploadId1 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        PartUploadUtils.partUpload( s3Client, bucketName, keyName, uploadId1,
                file1 );
        String uploadId2 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        PartUploadUtils.partUpload( s3Client, bucketName, keyName, uploadId2,
                file2 );
        String uploadId3 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        List< PartETag > partEtags = PartUploadUtils.partUpload( s3Client,
                bucketName, keyName, uploadId3, file3 );

        AbortMultipartUploadRequest request1 = new AbortMultipartUploadRequest(
                bucketName, keyName, uploadId1 );
        s3Client.abortMultipartUpload( request1 );
        PartUploadUtils.checkAbortMultipartUploadResult( s3Client, bucketName,
                keyName, uploadId1 );

        AbortMultipartUploadRequest request2 = new AbortMultipartUploadRequest(
                bucketName, keyName, uploadId2 );
        s3Client.abortMultipartUpload( request2 );
        PartUploadUtils.checkAbortMultipartUploadResult( s3Client, bucketName,
                keyName, uploadId2 );

        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId3, partEtags );

        // down file check the file content
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath3 ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                s3Client.deleteObject( bucketName, keyName );
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }
}
