package com.sequoiacm.s3.object.concurrent;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3383:并发获取同一对象
 * @author fanyu
 * @Date 2019.01.03
 * @version 1.00
 */
public class GetSameObject3383 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3383";
    private String keyName = "key3383";
    private String content = "testContent3383";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, content );
    }

    @Test
    public void testGetObject() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < 30; i++ ) {
            threadExec.addWorker( new GetObject() );
        }
        threadExec.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                s3Client.deleteObject( bucketName, keyName );
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkGetObjectResult( ObjectMetadata metadata )
            throws IOException {
        Assert.assertEquals( metadata.getETag(),
                TestTools.getMD5( content.getBytes() ), "md5 is wrong!" );
        Assert.assertEquals( metadata.getVersionId(), "null" );
        Assert.assertEquals( metadata.getContentLength(), content.length() );
    }

    private class GetObject {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                S3Object object = s3Client.getObject( bucketName, keyName );
                ObjectMetadata metadata = object.getObjectMetadata();
                checkGetObjectResult( metadata );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
