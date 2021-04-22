package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3566:桶内复制对象，源对象名和目标对象名相同
 * @author fanyu
 * @Date 2019.09.18
 * @version 1.00
 */
public class CopyObject3566 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "bb/object3566";
    private String bucketName = "bucket3566";

    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 12;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

        Map< String, String > srcMeta = new HashMap<>();
        srcMeta.put( "tag1", "srcobject123" );
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setUserMetadata( srcMeta );
        PutObjectRequest request = new PutObjectRequest( bucketName, keyName,
                new File( filePath ) );
        request.withMetadata( metaData );
        s3Client.putObject( request );

    }

    @Test
    public void testCopyObject() throws Exception {
        // test a: no set metadataDirective
        copyObjectWithMetaFail( "" );

        // test b:setMetadataDirective is COPY
        copyObjectWithMetaFail( "COPY" );

        // test c:setMetadataDirective is REPLACE
        copyObjectWithMetaSuccess( "REPLACE" );

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

    private void copyObjectWithMetaFail( String metadataDirective ) {
        try {
            CopyObjectRequest request = new CopyObjectRequest( bucketName,
                    keyName, bucketName, keyName );
            if ( !metadataDirective.equals( "" ) ) {
                request.setMetadataDirective( metadataDirective );
            }
            s3Client.copyObject( request );
            Assert.fail( "copyObject must be fail ! metadataDirective:"
                    + metadataDirective );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidRequest",
                    e.getStatusCode() + e.getErrorMessage() );
        }
    }

    private void copyObjectWithMetaSuccess( String metadataDirective )
            throws IOException {
        Map< String, String > objectMeta = new HashMap<>();
        objectMeta.put( "tag1", "testa" );
        objectMeta.put( "tag2", "testa2" );
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setUserMetadata( objectMeta );

        CopyObjectRequest request = new CopyObjectRequest( bucketName, keyName,
                bucketName, keyName );
        request.setMetadataDirective( metadataDirective );
        request.withNewObjectMetadata( metaData );
        s3Client.copyObject( request );

        // check the attributeInfo of get object
        GetObjectMetadataRequest mRequest = new GetObjectMetadataRequest(
                bucketName, keyName );
        ObjectMetadata result = s3Client.getObjectMetadata( mRequest );

        Assert.assertEquals( result.getETag(), TestTools.getMD5( filePath ) );
        Assert.assertEquals( result.getContentLength(), fileSize );

        Map< String, String > actMeta = result.getUserMetadata();
        Assert.assertEquals( actMeta.size(), objectMeta.size(),
                "expMeta is : " + objectMeta.toString() + "actMeta is : "
                        + actMeta.toString() );
        for ( Map.Entry< String, String > entry : objectMeta.entrySet() ) {
            Object key = entry.getKey();
            Assert.assertEquals( actMeta.get( key ), objectMeta.get( key ),
                    "actMeta = " + actMeta.toString() + ",expMeta = "
                            + objectMeta.toString() );
        }
    }
}
