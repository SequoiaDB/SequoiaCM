package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @descreption SCM-4823 :: 禁用版本控制，并发删除和获取对象列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4823 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4823";
    private String keyName = "key4823";
    private String oldContent = "oldContent4823";
    private String newContent = "newContent4823";
    private AmazonS3 s3Client = null;
    private int threadNum = 50;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        s3Client.putObject( bucketName, keyName, oldContent );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < threadNum; i++ ) {
            te.addWorker( new UpdateObjectThread() );
        }
        te.run();

        checkUpdateObjectResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkUpdateObjectResult() {
        S3Object obj = s3Client.getObject( bucketName, keyName );
        ObjectMetadata metadata = obj.getObjectMetadata();
        Assert.assertEquals( metadata.getETag(),
                TestTools.getMD5( newContent.getBytes() ) );
        Assert.assertEquals( metadata.getVersionId(), "null" );

        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( req );
        List< S3VersionSummary > objectVersionList = versionList
                .getVersionSummaries();
        Assert.assertEquals( objectVersionList.size(), 1,
                "the number of object version is incorrect!" );
    }

    private class UpdateObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.putObject( bucketName, keyName, newContent );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
