package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4622:开启版本控制，增加一个对象
 * @author wuyan
 * @Date 2022.07.05
 * @version 1.00
 */
public class Object4622 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4622";
    private String keyName = "object4622";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
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
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        BucketVersioningConfiguration configuration = new BucketVersioningConfiguration()
                .withStatus( "Enabled" );
        SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest = new SetBucketVersioningConfigurationRequest(
                bucketName, configuration );
        s3Client.setBucketVersioningConfiguration(
                setBucketVersioningConfigurationRequest );
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        PutObjectResult result = s3Client.putObject( bucketName, keyName,
                new File( filePath ) );
        checkPutObjectResult( bucketName, keyName, result );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                s3Client.deleteVersion( bucketName, keyName, "1.0" );
                s3Client.deleteBucket( bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkPutObjectResult( String bucketName, String keyName,
            PutObjectResult objAttrInfo ) throws Exception {
        String expMd5 = TestTools.getMD5( filePath );
        // 检查对象元数据信息
        checkObjectAttributeInfo( objAttrInfo, expMd5 );
        // 检查对象内容
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    private void checkObjectAttributeInfo( PutObjectResult objAttrInfo,
            String expMd5 ) {
        Assert.assertEquals( objAttrInfo.getETag(), expMd5 );
        String isModify = null;
        Assert.assertEquals( objAttrInfo.getExpirationTimeRuleId(), isModify );
        Assert.assertEquals( objAttrInfo.getVersionId(), "1.0" );

        // 获取对象属性信息
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                bucketName, keyName );
        ObjectMetadata result = s3Client.getObjectMetadata( request );
        Assert.assertEquals( result.getVersionId(), "1.0" );
        Assert.assertEquals( result.getETag(), expMd5 );
        Assert.assertEquals( result.getContentLength(), fileSize );
    }
}
