package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @descreption SCM-4809 :: 开启版本控制，并发更新相同对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4809 extends TestScmBase {
    private boolean runSuccess = false;
    private int defaultNums = 50;
    private String bucketName = "bucket4809";
    private String content = "content4809";
    private String keyName = "key4809";
    private List< String > expETags = new ArrayList<>();
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.putObject( bucketName, keyName, content );
        expETags.add( TestTools.getMD5( content.getBytes() ) );
    }

    @Test(groups = { GroupTags.base })
    public void testUpdateObject() throws Exception {
        List< UpdateObjectThread > updateObjects = new ArrayList<>();
        for ( int i = 0; i < defaultNums; i++ ) {
            String currContent = content + "."
                    + S3Utils.getRandomString( i );
            updateObjects.add( new UpdateObjectThread( currContent ) );
            expETags.add( TestTools.getMD5( currContent.getBytes() ) );
        }

        ThreadExecutor te = new ThreadExecutor();
        for ( UpdateObjectThread updateObject : updateObjects ) {
            te.addWorker(updateObject);
        }
        te.run();

        checkCreateObjectResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket(s3Client, bucketName);
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkCreateObjectResult() {
        List< String > actETags = new ArrayList<>();
        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( req );
        List<S3VersionSummary> objectVersionList = versionList
                .getVersionSummaries();
        Assert.assertEquals( objectVersionList.size(), defaultNums + 1 );
        for ( S3VersionSummary obj : objectVersionList ) {
            Assert.assertEquals( obj.getBucketName(), bucketName,
                    "bucketName is wrong!" );
            Assert.assertEquals( obj.getKey(), keyName, "keyName is wrong!" );
            actETags.add( obj.getETag() );
        }
        Collections.sort( expETags );
        Collections.sort( actETags );
        Assert.assertEquals( actETags, expETags,
                "etag is wrong! , the act etag is :" + actETags
                        + ", exp etag is : " + expETags.toString() );
    }

    private class UpdateObjectThread extends ResultStore {
        String content;

        public UpdateObjectThread( String content ) {
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
