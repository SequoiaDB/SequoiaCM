package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4814 :: 开启版本控制，并发指定不同版本删除相同对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4814 extends TestScmBase {
    private String bucketName = "bucket4814";
    private boolean runSuccess = false;
    private String key = "aa%maa%bb*中文/object4814";
    private String content = "content4814";
    private AmazonS3 s3Client = null;
    private int threadNum = 50;
    private int versionNum = 3;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        for ( int i = 0; i < versionNum; i++ ) {
            s3Client.putObject( bucketName, key, content );
        }
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < threadNum; i++ ) {
            te.addWorker( new DeleteObjectThread( ( i + 1 ) + ".0" ) );
        }
        te.run();

        checkDeleteObjectResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void checkDeleteObjectResult() throws Exception {
        // no version object exist
        boolean isExistObject = s3Client.doesObjectExist( bucketName, key );
        Assert.assertFalse( isExistObject, "the object should not be exist!" );

        // the deleted object does not exist.
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(
                bucketName, key, "1.0" );
        try {
            s3Client.getObjectMetadata( request );
            Assert.fail( "head object must be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getStatusCode(), 404 );
        }

        VersionListing versionListing = s3Client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
        Assert.assertEquals( versionListing.getVersionSummaries().size(), 0 );
    }

    private class DeleteObjectThread extends ResultStore {
        String versionId;

        DeleteObjectThread( String versionId ) {
            this.versionId = versionId;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteVersion( bucketName, key, versionId );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
