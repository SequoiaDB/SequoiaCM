package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4626 :: 更新桶状态为禁用（enable->suspended），新增文件和删除标记对象同名
 * @author wuyan
 * @Date 2022.07.05
 * @version 1.00
 */
public class Object4626 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "aa/bb/object4626";
    private String bucketName = "bucket4626";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024;
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
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.deleteObject( bucketName, keyName );
    }

    @Test
    public void testCreateObject() throws Exception {
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        checkCreateObjectReslut();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        keyName );
                s3Client.deleteBucket( bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkCreateObjectReslut() throws Exception {
        // 获取对象版本信息
        S3Object object = s3Client.getObject( bucketName, keyName );
        String versionId = object.getObjectMetadata().getVersionId();
        Assert.assertEquals( versionId, "null" );

        // 检查对象内容
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );

        // 获取历史版本中删除标记对象失败
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                "1.0" );
        try {
            s3Client.getObject( request );
            Assert.fail( "get object with deleteMarker should be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
        }
    }
}
