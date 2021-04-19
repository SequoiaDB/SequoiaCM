package com.sequoiacm.s3.object;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.exception.BaseException;


/**
 * @Description: SCM-3560:PutObjectRequest接口参数校验
 *
 * @author wangkexin
 * @Date 2019.01.07
 * @version 1.00
 */
public class Param_TestPutObjectRequest3560 extends TestScmBase {
    private String bucketName = "bucket3560";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );

    @DataProvider(name = "legalKeyNameProvider")
    public Object[][] generateKeyName() {
            String ascii = new String();
        for ( int i = 1; i < 32; i++ ) {
            ascii += ( char ) i;
        }
        for ( int i = 127; i < 256; i++ ) {
            ascii += ( char ) i;
        }
        return new Object[][] {
                // test a : 范围内取值
                new Object[] { "dir1/test.txt" },
                // test b : 长度边界值
                new Object[] { TestTools.getRandomString( 1 ) },
                new Object[] { TestTools.getRandomString( 900 ) },
                // test c : 包含特殊字符
                new Object[] { "!-_.'()" },
                // test d : 包含 数字字符[0-9a-zA-Z]
                new Object[] {
                        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" },
                // test e : 包含需要特殊处理的字符
                new Object[] { "&@,$=+;"},
                // test f : 包含不建议使用的字符
                new Object[] { "^`{}][#%~" },
                // test g : 包含中文字符
                new Object[] { "测试对象名" }, };
    }

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(dataProvider = "legalKeyNameProvider",enabled = false)
    public void testLegalKeyName( String keyName ) throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        PutObjectResult result = s3Client.putObject( new PutObjectRequest(
                bucketName, keyName, new File( filePath ) ) );
        String actMd5 = result.getETag();
        String expMd5 = TestTools.getMD5( filePath );
        Assert.assertEquals( actMd5, expMd5,
                "md5 is wrong! the key name is : " + keyName );
        TestTools.LocalFile.removeFile( localPath );
        actSuccessTests.getAndIncrement();
    }

    @Test
    public void testIllegalKeyName() throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        // test a : 对象名为空串，null，901个字节
//        try {
//            s3Client.putObject( new PutObjectRequest( bucketName, "",
//                    new File( filePath ) ) );
//            Assert.fail( "when key name is '',it should fail" );
//        } catch ( AmazonS3Exception e ) {
//            e.printStackTrace();
//            Assert.assertEquals( e.getErrorCode(), "MalformedXML" );
//        }

        // 对象名中包含不支持的参数
        String[] chars = {/*"aa/bb/object18558_8符*ezT2xd",*/"\\"/*, ":","*","?","\"",">","<","|"*/};
        for(String str : chars) {
            try {
                s3Client.putObject( new PutObjectRequest( bucketName, str, new File( filePath ) ) );
                Assert.fail( "when key name is null,it should fail" );
            } catch ( Exception e ) {
                System.out.println("-------str = " + str);
                e.printStackTrace();
                Assert.assertEquals( e.getMessage(),
                        " Invalid Key" );
            }
        }

        // 对象名以/开头
        try {
            s3Client.putObject( new PutObjectRequest( bucketName, "/a", new File( filePath ) ) );
            Assert.fail( "when key name is null,it should fail" );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.assertEquals( e.getMessage(),
                    " Invalid Key" );
        }

        // 对象名以/结尾
        try {
            s3Client.putObject( new PutObjectRequest( bucketName, "ac/", new File( filePath ) ) );
            Assert.fail( "when key name is null,it should fail" );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.assertEquals( e.getMessage(),
                    " Invalid Key" );
        }

        try {
            s3Client.putObject( new PutObjectRequest( bucketName, null,
                    new File( filePath ) ) );
            Assert.fail( "when key name is null,it should fail" );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The key parameter must be specified when uploading an object" );
        }

        try {
            s3Client.putObject( new PutObjectRequest( bucketName,
                    TestTools.getRandomString( 901 ),
                    new File( filePath ) ) );
            Assert.fail( "when key name is 901 characters,it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorMessage(), "Your key is too long." );
        }

        // test b : 桶名为null
        try {
            s3Client.putObject( new PutObjectRequest( null, "/dir/test16478",
                    new File( filePath ) ) );
            Assert.fail( "when bucket name is null,it should fail" );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The bucket name parameter must be specified when uploading an object" );
        }

        // test c : file 为不存在的路径、文件名不存在
        String nonexistentFilePath = localPath + File.separator
                + "nonexistentFilePath.txt";
        try {
            s3Client.putObject( new PutObjectRequest( bucketName,
                    "dir/test16478", new File( nonexistentFilePath ) ) );
            Assert.fail( "when file path does not exist,it should fail" );
        } catch ( SdkClientException e ) {
            e.printStackTrace();
            Assert.assertTrue(
                    e.getMessage().contains( "Unable to calculate MD5 hash" ),
                    e.getMessage() );
        }
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( actSuccessTests.get() == ( generateKeyName().length + 1 ) ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
            Assert.fail( "clean up failed:" + e.getMessage() );
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
