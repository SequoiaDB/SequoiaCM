package com.sequoiacm.s3.object.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.Md5Utils;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3380:并发增加对象
 * @author fanyu
 * @Date 2018.12.18
 * @version 1.00
 */
public class CreateObject3380 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3380";
    private String keyNameBase = "key3380";
    private List< String > keyNameList = new ArrayList<>();
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private AmazonS3 s3Client = null;

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
        s3Client.createBucket( bucketName );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        // 前缀相同
        for ( int i = 0; i < 30; i++ ) {
            threadExec.addWorker(
                    new CreateObject( "aa/bb/cc" + keyNameBase + i ) );
            keyNameList.add( "aa/bb/cc" + keyNameBase + i );
        }

        // 前缀不同
        for ( int i = 0; i < 10; i++ ) {
            threadExec.addWorker(
                    new CreateObject( "aa" + i + "/" + keyNameBase + i ) );
            keyNameList.add( "aa" + i + "/" + keyNameBase + i );
        }
        threadExec.run();
        checkCreateObjectResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
                for ( String keyName : keyNameList ) {
                    s3Client.deleteObject( bucketName, keyName );
                }
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkCreateObjectResult() throws Exception {
        for ( String keyName : keyNameList ) {
            S3Object s3Object = s3Client.getObject( bucketName, keyName );
            Assert.assertEquals( s3Object.getKey(), keyName );
            Assert.assertEquals(
                    Md5Utils.md5AsBase64( s3Object.getObjectContent() ),
                    Md5Utils.md5AsBase64( new File( filePath ) ) );
        }
    }

    private class CreateObject {
        private String keyName;

        private CreateObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void createObject() {
            AmazonS3 s3Client = null;
            try {
                s3Client = S3Utils.buildS3Client();
                s3Client.putObject( bucketName, keyName, new File( filePath ) );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
