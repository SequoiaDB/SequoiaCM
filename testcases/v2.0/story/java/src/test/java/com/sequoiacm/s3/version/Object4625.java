package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
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
import java.util.Date;

/**
 * @Description SCM-4625 :: 更新桶状态为开启（disable->enable），更新同名文件
 * @author wuyan
 * @Date 2022.07.05
 * @version 1.00
 */
public class Object4625 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "aa/bb/object4625";
    private String bucketName = "bucket4625";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 5;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

    }

    @Test
    public void testCreateObject() throws Exception {
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
        checkUpdateObjectReslut( bucketName );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket(s3Client, bucketName);
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkUpdateObjectReslut( String bucketName ) throws Exception {
        // get the new object content is the update content
        S3Object object = s3Client.getObject( bucketName, keyName );
        Date updateDate = object.getObjectMetadata().getLastModified();
        String newVersionId = object.getObjectMetadata().getVersionId();

        // 获取对象历史版本为null-marker标记
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                "null" );
        S3Object oldObject = s3Client.getObject( request );
        Date createDate = oldObject.getObjectMetadata().getLastModified();

        String updateVersionId = "2.0";
        Assert.assertEquals( newVersionId, updateVersionId );

        // 检查对象修改时间范围
        if ( updateDate.getTime() < createDate.getTime() ) {
            Assert.fail(
                    "updateDate must be grater than createDate! updateDate:"
                            + updateDate.getTime() + "\t createDate:"
                            + createDate.getTime() );
        }

        checkObjectContent( bucketName );
    }

    private void checkObjectContent( String bucketName ) throws Exception {
        String createVersionId = "null";
        String updateVersionId = "2.0";

        // 获取历史版本检查内容
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, createVersionId );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );

        // 获取当前版本检查内容
        String updateMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, updateVersionId );
        Assert.assertEquals( updateMd5, TestTools.getMD5( updatePath ) );
    }
}
