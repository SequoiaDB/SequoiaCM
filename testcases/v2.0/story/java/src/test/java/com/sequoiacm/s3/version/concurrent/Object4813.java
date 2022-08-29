package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
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
 * @descreption SCM-4813 :: 开启版本控制，并发删除相同对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4813 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4813";
    private String keyName = "key4813";
    private String content = "testContent4813";
    private String deleteVersionId = "2.0";
    private int initVersionNum = 3;
    private int expandVersionNum = 100;
    private int threadNum = expandVersionNum;
    private List< String > expEtag = new ArrayList<>();
    private List< String > expVersionId = new ArrayList<>();
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        // put three versions of the object
        for ( int i = 0; i < initVersionNum; i++ ) {
            String currentContent = content + S3Utils.getRandomString( i );
            PutObjectResult result = s3Client.putObject( bucketName, keyName,
                    currentContent );
            expVersionId.add( result.getVersionId() );
            expEtag.add( TestTools.getMD5( currentContent.getBytes() ) );
        }

        // version id : 0-102
        for ( int i = 3; i < 3 + expandVersionNum; i++ ) {
            expVersionId.add( ( i + 1 ) + ".0");
        }

        expEtag.remove( Integer.parseInt( deleteVersionId.split("\\.")[0] ) - 1 );
        expVersionId.remove( deleteVersionId );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // test a : Delete the same object without specifying version
        ThreadExecutor teA = new ThreadExecutor();
        for (int i = 0; i < threadNum; i++) {
            teA.addWorker(new DeleteObjectThread());
        }
        teA.run();

        // test b : Delete the same object with the specified version
        ThreadExecutor teB = new ThreadExecutor();
        for (int i = 0; i < threadNum; i++) {
            teB.addWorker(new DeleteObjectWithVersionThread(deleteVersionId));
        }
        teB.run();

        // check
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, keyName ) );
        checkVersionResult();
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

    private void checkVersionResult() {
        List< String > actEtg = new ArrayList<>();
        List< String > actVersionId = new ArrayList<>();
        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( req );
        List<S3VersionSummary> objectVersionList = versionList
                .getVersionSummaries();
        Assert.assertEquals( objectVersionList.size(), initVersionNum + expandVersionNum - 1,
                "the number of results returned is incorrect!" );
        for ( int i = 0; i < objectVersionList.size(); i++ ) {
            if ( i < 2 ) {
                Assert.assertFalse(
                        objectVersionList.get( i ).isDeleteMarker() );
                actEtg.add( objectVersionList.get( i ).getETag() );
            } else {
                Assert.assertTrue(
                        objectVersionList.get( i ).isDeleteMarker() );
                Assert.assertEquals( objectVersionList.get( i ).getETag(),
                        null );
            }
            actVersionId.add( objectVersionList.get( i ).getVersionId() );
        }
        Collections.sort( actVersionId );
        Collections.sort( expVersionId );
        Assert.assertEquals( actVersionId, expVersionId );

        Collections.reverse( expEtag );
        Assert.assertEquals( actEtg, expEtag );
    }

    private class DeleteObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteObject( bucketName, keyName );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class DeleteObjectWithVersionThread extends ResultStore {
        String versionId;

        public DeleteObjectWithVersionThread( String versionId ) {
            this.versionId = versionId;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteVersion( bucketName, keyName, versionId );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
