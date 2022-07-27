package com.sequoiacm.s3.object;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.Md5Utils;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description: SCM-3288:增加对象，携带md5值
 * @author fanyu
 * @Date 2018.11.13
 * @version 1.00
 */
public class CreateObject3288 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3288";
    private String keyName = "aa/bb/object3288";
    private AmazonS3 s3Client = null;
    private String beforeMd5 = null;
    private String wrongMd5 = null;
    private int fileSize = 1024 * 10;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        beforeMd5 = Md5Utils.md5AsBase64( new File( filePath ) );

        s3Client = S3Utils.buildS3Client();
        // create bucket
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test
    public void testPutObject() throws Exception {
        // put object with correct md5 value.
        File f = new File( filePath );
        InputStream input = new FileInputStream( f );
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentMD5( beforeMd5 );
        metadata.setContentLength( fileSize );
        s3Client.putObject( bucketName, keyName, input, metadata );
        input.close();
        checkPutObjectResult();

        // put object with wrong md5 value.
        wrongMd5 = TestTools.getMD5( filePath );
        metadata.setContentMD5( wrongMd5 );
        metadata.setContentLength( fileSize );
        try ( InputStream input2 = new FileInputStream( f )) {
            s3Client.putObject( bucketName, keyName, input2, metadata );
            Assert.fail( "exp fail but found success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "BadDigest" );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.deleteAllObjects( s3Client, bucketName );
                s3Client.deleteBucket( bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void checkPutObjectResult() throws Exception {
        // down file
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }
}
