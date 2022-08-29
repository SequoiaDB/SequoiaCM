package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-4317:上传多个分段，其中中间部分分段长度不一致
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4317 extends TestScmBase {
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );
    private String keyName = "/aa/object4317";
    private String bucketName = "bucket4317";
    private AmazonS3 s3Client = null;
    private File localPath = null;

    @DataProvider(name = "fileSizeProvider")
    public Object[][] generateFileSize() {
        return new Object[][] {
                // the parameter :
                // fileSize,eachPartSize,secondPartSize,thirdPartSize,fourthPartSize,fifthPartSize
                // test a: the length of the middle parts is inconsistent
                new Object[] { 1024 * 1024 * 45, 1024 * 1024 * 8,
                        1024 * 1024 * 5, 1024 * 1024 * 9, 1024 * 1024 * 9,
                        1024 * 1024 * 6 },
                // test b: the length of the middle parts grows from small to
                // large
                new Object[] { 1024 * 1024 * 40, 1024 * 1024 * 7,
                        1024 * 1024 * 5, 1024 * 1024 * 6, 1024 * 1024 * 7,
                        1024 * 1024 * 8 },
                // test c: the length of the middle parts grows from large to
                // small
                new Object[] { 1024 * 1024 * 40, 1024 * 1024 * 5,
                        1024 * 1024 * 9, 1024 * 1024 * 8, 1024 * 1024 * 7,
                        1024 * 1024 * 6 }, };
    }

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { GroupTags.base }, dataProvider = "fileSizeProvider")
    public void uploadParts( int fileSize, long eachPartSize,
            long secondPartSize, long thirdPartSize, long fourthPartSize,
            long fifthPartSize ) throws Exception {
        String filePath = createFile( fileSize );
        File file = new File( filePath );
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        List< PartETag > partEtags = partUpload( uploadId, file, eachPartSize,
                secondPartSize, thirdPartSize, fourthPartSize, fifthPartSize );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );

        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
        actSuccessTests.getAndIncrement();
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

    private String createFile( int fileSize ) throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        return filePath;
    }

    private List< PartETag > partUpload( String uploadId, File file,
            long partSize, long secondPartSize, long thirdPartSize,
            long fourthPartSize, long fifthPartSize ) {
        List< PartETag > partEtags = new ArrayList<>();
        int filePosition = 0;
        long fileSize = file.length();
        for ( int i = 1; filePosition < fileSize; i++ ) {
            long eachPartSize;
            if ( i == 2 ) {
                eachPartSize = secondPartSize;
            } else if ( i == 3 ) {
                eachPartSize = thirdPartSize;
            } else if ( i == 4 ) {
                eachPartSize = fourthPartSize;
            } else if ( i == 5 ) {
                eachPartSize = fifthPartSize;
            } else {
                eachPartSize = partSize;
            }
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( filePosition )
                    .withPartNumber( i ).withPartSize( eachPartSize )
                    .withBucketName( bucketName ).withKey( keyName )
                    .withUploadId( uploadId );
            UploadPartResult uploadPartResult = s3Client
                    .uploadPart( partRequest );
            partEtags.add( uploadPartResult.getPartETag() );
            filePosition += eachPartSize;
        }
        return partEtags;
    }
}
