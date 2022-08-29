package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @description SCM-4357:上传分段后，终止分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4357 extends TestScmBase {
    private boolean runSuccess = false;
    private final String keyName = "/aa/maa/bb/object4357";
    private final String bucketName = "bucket4357";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        int fileSize = 1024 * 1024 * 27;
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { GroupTags.base })
    public void abortMultipartUpload() throws Exception {
        File file = new File( filePath );
        // test a: upload parts is different length
        String uploadIdA = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        int[] partSizes = { 1024 * 1024 * 6, 1024 * 1024 * 5, 1024 * 1024 * 6,
                1024 * 1024 * 10 };
        partUpload( uploadIdA, file, partSizes );
        AbortMultipartUploadRequest requestA = new AbortMultipartUploadRequest(
                bucketName, keyName, uploadIdA );
        s3Client.abortMultipartUpload( requestA );
        PartUploadUtils.checkAbortMultipartUploadResult( s3Client, bucketName,
                keyName, uploadIdA );

        // test b: upload parts is the same length
        String uploadIdB = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        PartUploadUtils.partUpload( s3Client, bucketName, keyName, uploadIdB,
                file );
        AbortMultipartUploadRequest requestB = new AbortMultipartUploadRequest(
                bucketName, keyName, uploadIdB );
        s3Client.abortMultipartUpload( requestB );
        PartUploadUtils.checkAbortMultipartUploadResult( s3Client, bucketName,
                keyName, uploadIdB );

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

    private void partUpload( String uploadId, File file, int[] partSizes ) {
        int filePosition = 0;
        for ( int i = 0; i < partSizes.length; i++ ) {
            int partNumber = i + 1;
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
