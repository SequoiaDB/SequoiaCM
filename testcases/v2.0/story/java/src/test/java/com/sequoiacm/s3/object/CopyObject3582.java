package com.sequoiacm.s3.object;

import java.io.File;
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
 * @Description SCM-3582:指定ifMatch和ifUnModifiedSince条件复制对象，
 *              源对象不匹配ifUnModifiedSince
 * @author fanyu
 * @Date 2019.09.19
 * @version 1.00
 */
public class CopyObject3582 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3582";
    private String srcKeyName = "src/bb/object3582";
    private String destKeyName = "dest/object3582";
    private AmazonS3 s3Client = null;
    private String curVersionContent = "currentVersionContent3582!";
    private File localPath = null;
    private long lastModifiedTime = 0;

    @BeforeClass
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyName, "testcontent1" );
        s3Client.putObject( bucketName, srcKeyName, curVersionContent );
        GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(
                bucketName, srcKeyName );
        ObjectMetadata objMetadata = s3Client
                .getObjectMetadata( metadataRequest );
        Date lastModifiedDate = objMetadata.getLastModified();
        lastModifiedTime = lastModifiedDate.getTime();
    }

    @Test
    public void testCopyObject() throws Exception {
        // set date 2 minutes early at the lastModified time
        long timestamp = lastModifiedTime - 2 * 60 * 1000l;
        Date date = new Date( timestamp );

        // copy object
        String curVersionEtag = TestTools
                .getMD5( curVersionContent.getBytes() );
        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyName, bucketName, destKeyName );
        request.withUnmodifiedSinceConstraint( date )
                .withMatchingETagConstraint( curVersionEtag );
        s3Client.copyObject( request );

        // check the result
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, destKeyName );
        Assert.assertEquals( downfileMd5, curVersionEtag );

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
