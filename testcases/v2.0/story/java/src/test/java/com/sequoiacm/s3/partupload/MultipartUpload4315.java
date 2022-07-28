package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-4315:上传多个分段，其中所有分段长度都不相等
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4315 extends TestScmBase {
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );
    private AmazonS3 s3Client;
    private File localPath;
    private String key = "/aa/bb/obj4315";
    private String bucketName = "bucket4315";

    @DataProvider(name = "partSizeProvider")
    private Object[][] generateFileSize() {
        // parameter : partSize1, partSize2, ......
        return new Object[][] {
                // test point a: partSize increasing
                new Object[] { 5 * 1024 * 1024, 6 * 1024 * 1024,
                        10 * 1024 * 1024, 21 * 1024 * 1024 + 1 },
                // test point b: partSize decreasing
                new Object[] { 21 * 1024 * 1024, 10 * 1024 * 1024,
                        6 * 1024 * 1024, 5 * 1024 * 1024 + 2 },
                // test point c: partSize is irregular
                new Object[] { 21 * 1024 * 1024, 5 * 1024 * 1024,
                        10 * 1024 * 1024, 6 * 1024 * 1024 + 3 } };
    }

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "partSizeProvider")
    public void test( long partSize1, long partSize2, long partSize3,
            long partSize4 ) throws Exception {
        // init file
        long fileSize = partSize1 + partSize2 + partSize3 + partSize4;
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );

        // upload part
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                key );
        long[] partSizes = { partSize1, partSize2, partSize3, partSize4 };
        List< PartETag > partETags = this.partUpload( uploadId, partSizes,
                filePath );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, key,
                uploadId, partETags );

        // check results
        File downloadPath = new File(
                localPath + File.separator + "downloadFile_" + fileSize );
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, downloadPath,
                bucketName, key );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );

        actSuccessTests.getAndIncrement();
        // remove the file when the param success
        TestTools.LocalFile.removeFile( filePath );
        TestTools.LocalFile.removeFile( downloadPath );
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( actSuccessTests.get() == generateFileSize().length ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private List< PartETag > partUpload( String uploadId, long[] partSizes,
            String filePath ) {
        File file = new File( filePath );
        int fileOffset = 0;
        List< PartETag > partETags = new ArrayList<>();
        for ( int i = 0; i < partSizes.length; i++ ) {
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( fileOffset )
                    .withPartNumber( i + 1 ).withPartSize( partSizes[ i ] )
                    .withBucketName( bucketName ).withKey( key )
                    .withUploadId( uploadId );
            UploadPartResult partResult = s3Client.uploadPart( partRequest );

            partETags.add( partResult.getPartETag() );
            fileOffset += partSizes[ i ];
        }
        return partETags;
    }
}