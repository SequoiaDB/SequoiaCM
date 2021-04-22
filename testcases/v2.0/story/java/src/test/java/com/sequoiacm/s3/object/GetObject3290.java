package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Descreption SCM-3290:不带versionId获取对象（标准模式）
 * @Author YiPan
 * @Date 2021/3/10
 */
public class GetObject3290 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3290";
    private String objectName = "object3290";
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 100;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localfile" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws IOException {
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectName, new File( filePath ) );
        // 获取预期etag
        String exp = TestTools.getMD5( filePath );
        // 获取实际etag
        S3Object object = s3Client.getObject( bucketName, objectName );
        ObjectMetadata objectMetadata = object.getObjectMetadata();
        String act = objectMetadata.getETag();
        // etag比较
        Assert.assertEquals( exp, act );
        // 获取id比较
        String versionId = objectMetadata.getVersionId();
        Assert.assertEquals( versionId, "null" );
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
}
