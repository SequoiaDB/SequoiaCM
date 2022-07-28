package com.sequoiacm.bigfile;

import com.amazonaws.services.s3.AmazonS3;
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
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4352:不开启版本控制，对象较大，分段上传更新对象
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4352 extends TestScmBase {
    private static final long M = 1024 * 1024L;
    private boolean runSuccess = false;
    private String bucketName = "bucket4352";
    private String keyName = "key4352";
    private AmazonS3 s3Client = null;
    private long oldFileSize = 5 * 1024 * 1024 * 1024L;
    private long newFileSize = 4 * 1024 * 1024 * 1024L;
    private File localPath = null;
    private File oldfile = null;
    private File newfile = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + oldFileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, oldFileSize );
        oldfile = new File( filePath );

        filePath = localPath + File.separator + "localFile_" + newFileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, newFileSize );
        newfile = new File( filePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    // 用例执行时间过长，ci上暂时屏蔽
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    public void testUpload() throws Exception {
        // put object
        long[] partSizes = { 100 * M, 100 * M, 150 * M, 200 * M, 50 * M, 50 * M,
                100 * M, 500 * M, 500 * M, 200 * M, 500 * M, 300 * M, 600 * M,
                100 * M, 300 * M, 1050 * M, 320 * M };
        putObject( partSizes, oldfile );

        long[] partSizes2 = { 100 * M, 500 * M, 200 * M, 150 * M, 300 * M,
                500 * M, 200 * M, 50 * M, 600 * M, 100 * M, 1000 * M, 100 * M,
                296 * M };
        putObject( partSizes2, newfile );

        // check
        String expMd5 = TestTools.getMD5( filePath );
        String downloadMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downloadMd5, expMd5 );
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

    private void putObject( long[] partSizes, File file ) {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        List< PartETag > partEtags = partUpload( uploadId, partSizes, file );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );
    }

    private List< PartETag > partUpload( String uploadId, long partSizes[],
            File file ) {
        List< PartETag > partEtags = new ArrayList<>();
        long filePosition = 0;
        for ( int i = 1; i < partSizes.length + 1; i++ ) {
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( filePosition )
                    .withPartNumber( i ).withPartSize( partSizes[ i - 1 ] )
                    .withBucketName( bucketName ).withKey( keyName )
                    .withUploadId( uploadId );
            UploadPartResult uploadPartResult = s3Client
                    .uploadPart( partRequest );
            partEtags.add( uploadPartResult.getPartETag() );
            filePosition += partSizes[ i - 1 ];
        }
        return partEtags;
    }
}