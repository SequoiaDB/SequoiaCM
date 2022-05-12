package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3575:指定ifMatch和ifNoneMatch条件匹配源对象复制
 * @author fanyu
 * @Date 2019.09.19
 * @version 1.00
 */
public class CopyObject3575 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3575";
    private String srcKeyName = "src/bb/object3575";
    private String destKeyName = "dest/object3575";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String hisVersionContent = "testContent";

    @BeforeClass
    private void setUp() throws Exception {
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
        s3Client.putObject( bucketName, srcKeyName, hisVersionContent );
        s3Client.putObject( bucketName, srcKeyName, new File( filePath ) );
    }

    @Test
    public void testCopyObject() throws Exception {
        String curVersionEtag = TestTools.getMD5( filePath );
        String hisVersionETag = TestTools
                .getMD5( hisVersionContent.getBytes() );
        CopyObjectRequest request = new CopyObjectRequest( bucketName,
                srcKeyName, bucketName, destKeyName );
        request.withMatchingETagConstraint( curVersionEtag )
                .withNonmatchingETagConstraint( hisVersionETag );
        s3Client.copyObject( request );

        // check the content of destObject
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
