package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3334:headObject请求指定response属性
 * @author fanyu
 * @Date 2019.09.25
 * @version 1.00
 */
public class HeadObject3334 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "bb/object3334.jps";
    private String bucketName = "bucket3334";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    private Date httpExpiresDate = null;

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

        s3Client.putObject( bucketName, keyName,
                "testobjectForGetHttpExpiresDate" );
        // set the httpExpiresData
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                bucketName, keyName );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        Date lastModifiedDate = objMetadata.getLastModified();
        long lastModifiedTime = lastModifiedDate.getTime();
        // set date 30 min later than lastModified time
        long timestamp = lastModifiedTime + 30 * 60 * 1000l;
        httpExpiresDate = new Date( timestamp );
    }

    @Test
    public void testHeadObject() throws Exception {
        Map< String, String > expUserMeta = new HashMap<>();
        expUserMeta.put( "tag1", "testa" );
        expUserMeta.put( "tag2", "testa2" );
        PutObjectRequest request = new PutObjectRequest( bucketName, keyName,
                new File( filePath ) );
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentDisposition( "this is object!" );
        metaData.setContentType( "jps" );
        metaData.setCacheControl( "RFC2616" );
        metaData.setContentEncoding( "tar" );
        metaData.setContentLanguage( "zh" );
        metaData.setHttpExpiresDate( httpExpiresDate );
        metaData.setUserMetadata( expUserMeta );
        request.withMetadata( metaData );
        s3Client.putObject( request );
        // head object and check the metaData
        checkObjectAttributeInfo( bucketName, keyName, expUserMeta );
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
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkObjectAttributeInfo( String bucketName, String keyName,
            Map< String, String > expMeta ) throws IOException {
        // check the attributeInfo of get object
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                bucketName, keyName );
        ObjectMetadata result = s3Client.getObjectMetadata( request );
        Assert.assertEquals( result.getETag(), TestTools.getMD5( filePath ) );
        Assert.assertEquals( result.getContentLength(), fileSize );
        Assert.assertEquals( result.getContentDisposition(),
                "this is object!" );
        Assert.assertEquals( result.getCacheControl(), "RFC2616" );
        Assert.assertEquals( result.getContentEncoding(), "tar" );
        Assert.assertEquals( result.getContentLanguage(), "zh" );
        Assert.assertEquals( result.getContentType(), "jps" );
        Assert.assertEquals( result.getHttpExpiresDate(), httpExpiresDate );

        Map< String, String > actMeta = result.getUserMetadata();
        Assert.assertEquals( actMeta.size(), expMeta.size(), "expMeta is : "
                + expMeta.toString() + "actMeta is : " + actMeta.toString() );
        for ( Map.Entry< String, String > entry : expMeta.entrySet() ) {
            Object key = entry.getKey();
            Assert.assertEquals( actMeta.get( key ), expMeta.get( key ),
                    "actMeta = " + actMeta.toString() + ",expMeta = "
                            + expMeta.toString() );
        }
    }
}
