package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @descreption SCM-4810 :: 开启版本控制，并发更新不同对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4810 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4810";
    private String content = "content4810";
    private String keyName = "key4810";
    private int keyNums = 10;
    private int versionNums = 2;
    private List< String > expETags = new ArrayList<>();
    private List< String > KeyNames = new ArrayList<>();
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        for ( int i = 0; i < keyNums; i++ ) {
            String tmpContent = content + "_" + i + "0";
            s3Client.putObject( bucketName, keyName + "_" + i, tmpContent );
            expETags.add( TestTools.getMD5( tmpContent.getBytes() ) );
        }
    }

    @Test
    public void test() throws Exception {
        List< UpdateObjectThread > updateObjects = new ArrayList<>();
        for ( int i = 0; i < keyNums; i++ ) {
            String currContent = content + "_" + i + "1";
            updateObjects.add(
                    new UpdateObjectThread( keyName + "_" + i, currContent ) );
            expETags.add( TestTools.getMD5( currContent.getBytes() ) );
        }

        ThreadExecutor te = new ThreadExecutor();
        for ( UpdateObjectThread updateObject : updateObjects ) {
            te.addWorker( updateObject );
        }
        te.run();

        checkCreateObjectResult();
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

    private void checkCreateObjectResult() {
        List< String > actETags = new ArrayList<>();
        int count = 0;
        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( req );
        List< S3VersionSummary > objectVersionList = versionList
                .getVersionSummaries();
        Assert.assertEquals( objectVersionList.size(), keyNums * versionNums );
        for ( S3VersionSummary obj : objectVersionList ) {
            Assert.assertEquals( obj.getBucketName(), bucketName,
                    "bucketName is wrong!" );
            Assert.assertEquals( obj.getKey(),
                    keyName + "_" + count / versionNums, "keyName is wrong!" );
            Assert.assertEquals( obj.getVersionId(),
                    ( versionNums - count % versionNums ) + ".0",
                    "versionId is wrong!" );
            actETags.add( obj.getETag() );
            count++;
        }
        Collections.sort( expETags );
        Collections.sort( actETags );
        Assert.assertEquals( actETags, expETags,
                "etag is wrong! , the act etag is :" + actETags
                        + ", exp etag is : " + expETags.toString() );
    }

    private class UpdateObjectThread extends ResultStore {
        String keyName;
        String content;

        public UpdateObjectThread( String key, String content ) {
            this.keyName = key;
            this.content = content;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3ClientT = S3Utils.buildS3Client();
            try {
                s3ClientT.putObject( bucketName, keyName, content );
            } finally {
                if ( s3ClientT != null ) {
                    s3ClientT.shutdown();
                }
            }
        }
    }
}
