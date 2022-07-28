package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @description SCM-4358:上传多个相同分段，终止分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4358 extends TestScmBase {
    private boolean runSuccess = false;
    private final String keyName = "/aa/object4358";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String filePath = null;
    private final String bucketName = "bucket4358";

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        int fileSize = 1024 * 1024 * 38;
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void abortMultipartUpload() throws Exception {
        File file = new File( filePath );
        // test a: upload parts is different length
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        int[] partSizes = { 1024 * 1024 * 6, 1024 * 1024 * 5, 1024 * 1024 * 6,
                1024 * 1024 * 8, 1024 * 1024 * 6, 1024 * 1024 * 7 };
        int[] partNumbers = { 1, 1, 3, 4, 4, 5 };
        partUpload( uploadId, file, partSizes, partNumbers );
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                bucketName, keyName, uploadId );
        s3Client.abortMultipartUpload( request );

        PartUploadUtils.checkAbortMultipartUploadResult( s3Client, bucketName,
                keyName, uploadId );
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

    private void partUpload( String uploadId, File file, int[] partSizes,
            int[] partNumbers ) {
        int filePosition = 0;
        for ( int i = 0; i < partSizes.length; i++ ) {
            int partNumber = partNumbers[ i ];
            long eachPartSize = partSizes[ i ];
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( filePosition )
                    .withPartNumber( partNumber ).withPartSize( eachPartSize )
                    .withBucketName( bucketName ).withKey( keyName )
                    .withUploadId( uploadId );
            s3Client.uploadPart( partRequest );
            filePosition += eachPartSize;
        }
    }
}
