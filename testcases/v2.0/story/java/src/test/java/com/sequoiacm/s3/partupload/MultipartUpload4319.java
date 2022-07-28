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
import org.testng.annotations.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4319:上传相同分段，长度相同内容不同
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4319 extends TestScmBase {
    List< PartETag > partETags = new ArrayList<>();
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private File localPath;
    private String filePath1;
    private String filePath2;
    private String filePath3;
    private File file1;
    private File file2;
    private int fileSize = 11 * 1024 * 1024;
    private int firstPartSize = 5 * 1024 * 1024;
    private int remainPartSize = fileSize - firstPartSize;
    private String key = "/aa/bb/obj4319";
    private String bucketName = "bucket4319";

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                key );
        this.partUpload( uploadId );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, key,
                uploadId, partETags );

        // check results
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath3 ) );

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

    private void partUpload( String uploadId ) {
        File file = file1;
        long fileOffset = 0;
        int partNum = 1;
        long partSize = firstPartSize;
        for ( int i = 0; i < 3; i++ ) {
            if ( i >= 1 ) {
                fileOffset = firstPartSize;
                partNum = 2;
                partSize = remainPartSize;
                if ( i == 2 ) {
                    file = file2;
                }
            }
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( fileOffset )
                    .withPartNumber( partNum ).withPartSize( partSize )
                    .withBucketName( bucketName ).withKey( key )
                    .withUploadId( uploadId );
            UploadPartResult partResult = s3Client.uploadPart( partRequest );
            if ( i == 0 || i == 2 ) {
                partETags.add( partResult.getPartETag() );
            }
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
        filePath3 = filePathBase + "_3.txt";
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );
        file1 = new File( filePath1 );
        file2 = new File( filePath2 );

        // expect file content
        TestTools.LocalFile.createFile( filePath3, 0 );
        this.readFile( filePath1, 0, firstPartSize, filePath3 );
        this.readFile( filePath2, firstPartSize, remainPartSize, filePath3 );
    }

    private void readFile( String filePath, int off, int len,
            String downloadPath ) throws IOException {
        try ( RandomAccessFile raf = new RandomAccessFile( filePath, "rw" ) ;
                OutputStream fos = new FileOutputStream( downloadPath, true )) {
            raf.seek( off );
            int readSize;
            byte[] buf = new byte[ off + len ];
            readSize = raf.read( buf, off, len );
            fos.write( buf, off, readSize );
        }
    }
}