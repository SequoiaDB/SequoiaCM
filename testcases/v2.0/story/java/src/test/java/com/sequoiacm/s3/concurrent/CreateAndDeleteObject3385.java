package com.sequoiacm.s3.concurrent;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.Md5Utils;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3385:并发增加和删除相同对象
 *
 * @author wangkexin
 * @Date 2019.01.03
 * @version 1.00
 */
public class CreateAndDeleteObject3385 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3385";
    private String keyNameBase = "aa/bb/cc/dd/key3385_";
    private int objectNum = 20;
    private String content = "testContent3385";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName,keyNameBase + 0,content );
    }

    @Test
    public void testCreateAndDeleteObject() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for(int i = 1; i < objectNum; i++){
            threadExec.addWorker( new CreateObject( keyNameBase + i ) );
        }
        threadExec.addWorker( new DeleteObject( keyNameBase + 0 ) );
        threadExec.run();

        // check result
        for (int i = 1; i < objectNum; i++) {
            S3Object s3Object = s3Client.getObject( bucketName, keyNameBase + i );
            Assert.assertEquals( s3Object.getKey(), keyNameBase + i );
            Assert.assertEquals(
                    Md5Utils.md5AsBase64( s3Object.getObjectContent() ),
                    Md5Utils.md5AsBase64( content.getBytes() ) );
        }
        Assert.assertFalse(
                s3Client.doesObjectExist( bucketName, keyNameBase + 0 ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                for ( int i = 1; i < objectNum; i++ ) {
                    s3Client.deleteObject( bucketName, keyNameBase + i );
                }
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
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
                s3Client.putObject( bucketName, keyName, content );
            }finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class DeleteObject {
        private String keyName;

        public DeleteObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = null;
            try {
                s3Client = S3Utils.buildS3Client();
                s3Client.deleteObject( bucketName, keyName );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
