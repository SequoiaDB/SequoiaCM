package com.sequoiacm.s3.object.concurrent;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3381:对象已存在，并发更新相同对象
 * @author fanyu
 * @Date 2018.12.18
 * @version 1.00
 */
public class UpdateObject3381 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3381";
    private String keyName = "key3381";;
    private String oldContent = "testContentold3381";
    private String newContent = "testContentnew3381";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, oldContent );
    }

    @Test
    public void testUpdateObject() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < 50; i++ ) {
            threadExec.addWorker( new UpdateObject() );
        }
        threadExec.run();
        checkUpdateObjectResult( s3Client );
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

    private void checkUpdateObjectResult( AmazonS3 s3Client )
            throws IOException {
        ListObjectsV2Result listObjectsV2Result = s3Client
                .listObjectsV2( bucketName );
        Assert.assertEquals( listObjectsV2Result.getObjectSummaries().size(),
                1 );
        Assert.assertEquals( listObjectsV2Result.getObjectSummaries().get( 0 )
                .getBucketName(), bucketName, "bucketName is wrong!" );
        Assert.assertEquals(
                listObjectsV2Result.getObjectSummaries().get( 0 ).getKey(),
                keyName, "keyName is wrong!" );
        Assert.assertEquals(
                listObjectsV2Result.getObjectSummaries().get( 0 ).getETag(),
                TestTools.getMD5( newContent.getBytes() ),
                "content is wrong!" );

    }

    private class UpdateObject {

        @ExecuteOrder(step = 1)
        private void updateObject() throws Exception {
            AmazonS3 s3Client = null;
            try {
                s3Client = S3Utils.buildS3Client();
                s3Client.putObject( bucketName, keyName, newContent );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
