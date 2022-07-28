package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
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
import java.io.IOException;
import java.util.List;

/**
 * @description SCM-4348:开启分段检测，不开启版本控制，相同key不同uploadId多次分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4348 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private File localPath;
    private String filePath1;
    private String filePath2;
    private File file1;
    private File file2;
    private int fileSize = 60 * 1024 * 1024;
    private int partSize = 6 * 1024 * 1024;
    private String key = "/aa/bb/obj4348";
    private String bucketName = "bucket4348";

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        String uploadId1 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                key );
        List< PartETag > partETags1 = PartUploadUtils.partUpload( s3Client,
                bucketName, key, uploadId1, file1, partSize );
        PartUploadUtils.listPartsAndCheckPartNumbers( s3Client, bucketName, key,
                partETags1, uploadId1 );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, key,
                uploadId1, partETags1 );

        String uploadId2 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                key );
        List< PartETag > partETags2 = PartUploadUtils.partUpload( s3Client,
                bucketName, key, uploadId2, file2, partSize );
        PartUploadUtils.listPartsAndCheckPartNumbers( s3Client, bucketName, key,
                partETags2, uploadId2 );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, key,
                uploadId2, partETags2 );

        // check results
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath2 ) );

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
            s3Client.shutdown();
        }
    }

    private void initFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        String filePathBase = localPath + File.separator + "localFile_"
                + fileSize;
        filePath1 = filePathBase + "_1.txt";
        filePath2 = filePathBase + "_2.txt";
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );
        file1 = new File( filePath1 );
        file2 = new File( filePath2 );
    }
}