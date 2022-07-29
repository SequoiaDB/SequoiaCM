package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
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
 * @Description SCM-4623 :: 禁用版本控制，增加一个对象
 * @author wuyan
 * @Date 2022.07.05
 * @version 1.00
 */
public class Object4623 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object4623";
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
    }

    @Test
    public void testCreateObject() throws Exception {
        PutObjectResult result = s3Client.putObject(
                TestScmBase.susVerBucketName, keyName, new File( filePath ) );
        checkPutObjectResult( TestScmBase.susVerBucketName, keyName, result );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
               // s3Client.deleteVersion( TestScmBase.susVerBucketName, keyName,
              //          "1.0" );
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
            String expMd5 ) throws IOException {
        Assert.assertEquals( objAttrInfo.getETag(), expMd5 );
        Assert.assertEquals( objAttrInfo.getVersionId(), "null" );

        // 获取对象属性信息
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                TestScmBase.susVerBucketName, keyName );
        ObjectMetadata result = s3Client.getObjectMetadata( request );
        Assert.assertEquals( result.getVersionId(), "null" );
        Assert.assertEquals( result.getETag(), expMd5 );
        Assert.assertEquals( result.getContentLength(), fileSize );
    }
}
