package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3574:复制对象指定ifModifiedSince条件
 * @author wuyan
 * @Date 2019.09.19
 * @version 1.00
 */
public class CopyObject3574 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3574";
    private String srcKeyName = "src/bb/object3574";
    private String destKeyName = "dest/object3574";
    private AmazonS3 s3Client = null;
    private int fileSize1 = 1024 * 2;
    private int fileSize2 = 1;
    private File localPath = null;
    private String hisVersionFilePath = null;
    private String curVersionFilePath = null;
    private long lastModifiedTime = 0;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        hisVersionFilePath = localPath + File.separator + "localFile_"
                + fileSize1 + ".txt";
        curVersionFilePath = localPath + File.separator + "localFile_"
                + fileSize1 + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( hisVersionFilePath, fileSize1 );
        TestTools.LocalFile.createFile( curVersionFilePath, fileSize2 );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyName,
                new File( hisVersionFilePath ) );
        s3Client.putObject( bucketName, srcKeyName,
                new File( curVersionFilePath ) );
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                bucketName, srcKeyName );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        Date lastModifiedDate = objMetadata.getLastModified();
        lastModifiedTime = lastModifiedDate.getTime();
    }

    @Test
    public void testCopyObject() throws Exception {
        // test c:the currentVersion of sourceObject has not been modified after
        // the date
        copyObjectWithModifiedSinceC();

        // set date an hour early at the lastModified time
        long timestamp = lastModifiedTime - 60 * 60 * 1000l;
        Date date = new Date( timestamp );
        // test b: the currentVersion of sourceObject has been modified after
        // the date
        copyObjectWithModifiedSinceB( date );

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

    private void copyObjectWithModifiedSinceB( Date date ) throws Exception {
        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyName, bucketName, destKeyName );
        request.withModifiedSinceConstraint( date );
        s3Client.copyObject( request );

        // check the content of destObject
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, destKeyName );
        Assert.assertEquals( downfileMd5,
                TestTools.getMD5( curVersionFilePath ) );
    }

    private void copyObjectWithModifiedSinceC() throws Exception {
        // set date 10 minutes later than lastModified time
        long timestamp = lastModifiedTime + 10 * 60 * 1000l;
        Date date = new Date( timestamp );

        try {
            CopyObjectRequest request = new CopyObjectRequest( bucketName,
                    srcKeyName, bucketName, destKeyName );
            request.withModifiedSinceConstraint( date );
            s3Client.copyObject( request );
            Assert.fail( "copyObject must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getStatusCode(), 304,
                    e.getErrorCode() + e.getErrorMessage() + "\ndate:" + date );
        }
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, destKeyName ),
                "the destObject does not exist!" );
    }
}
