package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;

/**
 * @Descreption SCM-3293:指定range范围，不带versionId获取对象（标准模式）
 * @Author YiPan
 * @Date 2020/3/10
 */
public class GetObject3293 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3293";
    private String objectName = "object3293";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024;

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
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectName, new File( filePath ) );
    }

    @Test
    public void test() throws IOException {
        GetObjectRequest getObjectRequest;
        S3Object object;
        String actDownloadPath = localPath + File.separator + "localfile"
                + fileSize + "act.txt";
        // test a: rang指定起始范围为0-10
        getObjectRequest = new GetObjectRequest( bucketName, objectName )
                .withRange( 0, 10 * 1024 );
        object = s3Client.getObject( getObjectRequest );
        TestTools.LocalFile.readFile( filePath, 0, 10 * 1024, actDownloadPath );

        // test b: rang指定起始范围为512-1020
        getObjectRequest = new GetObjectRequest( bucketName, objectName )
                .withRange( 512 * 1024, 1020 * 1024 );
        object = s3Client.getObject( getObjectRequest );
        TestTools.LocalFile.readFile( filePath, 512 * 1024, 508 * 1024,
                actDownloadPath );

        // test c: rang指定起始范围为1023-1024
        getObjectRequest = new GetObjectRequest( bucketName, objectName )
                .withRange( 1023 * 1024, 1024 * 1024 );
        object = s3Client.getObject( getObjectRequest );
        TestTools.LocalFile.readFile( filePath, 1023 * 1024, 1024,
                actDownloadPath );
    }

    @AfterClass
    private void tearDown() {
        try {
            S3Utils.clearBucket( s3Client, bucketName );
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            s3Client.shutdown();
        }
    }
}
