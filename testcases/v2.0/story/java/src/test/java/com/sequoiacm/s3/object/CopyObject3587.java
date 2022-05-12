package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3587:
 *              指定ifNoneMatch/ifMatch/ifModifiedSince/ifNoneModifiedSince条件获取对象
 * @author fanyu
 * @Date 2019.09.20
 * @version 1.00
 */
public class CopyObject3587 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3587";
    private String srcKeyNameA = "srcA/bb/object3587";
    private String srcKeyNameB = "srcB/bb/object3587";
    private String destKeyName = "dest/object3587";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String hisVersionContent0 = "testHisVersionContent0";
    private String keyBContent = "testContent1_3587";
    private String curVersionContent = "testcurVersionContent3587";
    private long lastModifiedTime = 0;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyNameA, hisVersionContent0 );
        s3Client.putObject( bucketName, srcKeyNameB, keyBContent );
        s3Client.putObject( bucketName, srcKeyNameA, curVersionContent );
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                bucketName, srcKeyNameA );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        Date lastModifiedDate = objMetadata.getLastModified();
        lastModifiedTime = lastModifiedDate.getTime();
    }

    @Test
    public void testCopyObject() throws Exception {
        String curVersionETag = TestTools
                .getMD5( curVersionContent.getBytes() );
        String keyBETag = TestTools.getMD5( keyBContent.getBytes() );

        // set date 3 minutes early at the current time
        long beforeTimestamp = lastModifiedTime - 3 * 60 * 1000l;
        Date beforeDate = new Date( beforeTimestamp );

        // set date 1 minutes later than current time
        long afterTimestamp = lastModifiedTime + 1 * 60 * 1000l;
        Date afterDate = new Date( afterTimestamp );

        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyNameA, bucketName, destKeyName );
        request.withMatchingETagConstraint( curVersionETag )
                .withNonmatchingETagConstraint( keyBETag )
                .withModifiedSinceConstraint( beforeDate )
                .withUnmodifiedSinceConstraint( afterDate );
        s3Client.copyObject( request );

        // check the content of destObject
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, destKeyName );
        Assert.assertEquals( downfileMd5, curVersionETag );

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
}
