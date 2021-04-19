package com.sequoiacm.s3.object;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3376: create object
 * @author wuyan
 * @Date 2018.11.6
 * @version 1.00
 */
public class DeleteObject3376To3379 extends TestScmBase {
    private String bucketName = "bucket3376";
    private String[] keyNames = {"aa/maa/bb/object3376","aa/maa/cc/object3376",
            "bb/object3376","cc/object3376" };
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;

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
        s3Client.createBucket( bucketName);
        for ( String keyName : keyNames ) {
           s3Client.putObject( bucketName, keyName, new File( filePath ) );
        }
    }

    @Test
    public void testCreateObject() throws Exception {
        for ( String keyName : keyNames ) {
            s3Client.deleteObject( bucketName, keyName );
        }

        // 检查结果
        for ( String keyName : keyNames ) {
            Assert.assertFalse( s3Client.doesObjectExist( bucketName, keyName ) );
        }

        // 重复删除对象
        s3Client.deleteObject( bucketName,keyNames[0] );

        // 桶不存在，删除对象
        try {
            s3Client.deleteObject( bucketName + "10", keyNames[ 0 ] );
            Assert.fail( "exp failed but act success!!!" );
        }catch ( AmazonS3Exception e ){
            if(e.getStatusCode() != 404){
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() {
        try {
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
