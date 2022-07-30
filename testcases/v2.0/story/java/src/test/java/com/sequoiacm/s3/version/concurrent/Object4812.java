package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @descreption SCM-4812 :: 开启版本控制，并发列取对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4812 extends TestScmBase {
    private String bucketName = "bucket4812";
    private String keyName = "/dir/dir";
    private String prefix = "/dir";
    private String delimiter = "/";
    private String content = "content4812";
    private List< String > expResultList1 = new ArrayList<>();
    private List< String > expResultList2 = new ArrayList<>();
    private List< String > expResultList3 = new ArrayList<>();
    private int objectTotalNum = 50;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        // put multiple objects
        for ( int i = 0; i < objectTotalNum; i++ ) {
            String currentKeyName = keyName + i + "/4812";
            s3Client.putObject( bucketName, currentKeyName, content );
            expResultList1.add( currentKeyName );
            expResultList2.add( currentKeyName );
        }

        // put another objects that do not match prefix
        s3Client.putObject( bucketName, "/testa4812", content );
        s3Client.putObject( bucketName, "/testb4812", content );
        expResultList1.add( "/testa4812" );
        expResultList1.add( "/testb4812" );

        Collections.sort( expResultList1 );
        Collections.sort( expResultList2 );
        expResultList3.add( "/dir/" );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ListVersionThread() );
        te.addWorker( new ListVersionWithPrefixThread() );
        te.addWorker( new ListVersionWithPrefixAndDelimiterThread() );
        te.run();

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

    private class ListVersionThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            List< String > actVersionsKeyName = new ArrayList<>();
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                ListVersionsRequest req = new ListVersionsRequest()
                        .withBucketName( bucketName );
                VersionListing versionList = s3Client.listVersions( req );
                while ( true ) {
                    List< S3VersionSummary > verList = versionList
                            .getVersionSummaries();
                    for ( S3VersionSummary s3VersionSummary : verList ) {
                        actVersionsKeyName.add( s3VersionSummary.getKey() );
                    }

                    if ( versionList.isTruncated() ) {
                        versionList = s3Client
                                .listNextBatchOfVersions( versionList );
                    } else {
                        break;
                    }
                }
                Assert.assertEqualsNoOrder( actVersionsKeyName.toArray(),
                        expResultList1.toArray(),
                        "the returned result by versions is wrong, act: "
                                + actVersionsKeyName + ", exp: "
                                + expResultList1 );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListVersionWithPrefixThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            List< String > actVersionsKeyName = new ArrayList<>();
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                ListVersionsRequest req = new ListVersionsRequest()
                        .withBucketName( bucketName ).withPrefix( prefix );
                VersionListing versionList = s3Client.listVersions( req );
                while ( true ) {
                    List< S3VersionSummary > verList = versionList
                            .getVersionSummaries();
                    for ( S3VersionSummary s3VersionSummary : verList ) {
                        actVersionsKeyName.add( s3VersionSummary.getKey() );
                    }

                    if ( versionList.isTruncated() ) {
                        versionList = s3Client
                                .listNextBatchOfVersions( versionList );
                    } else {
                        break;
                    }
                }
                Assert.assertEquals( actVersionsKeyName, expResultList2,
                        "the returned result by versions is wrong, act: "
                                + actVersionsKeyName.toString() + ", exp: "
                                + expResultList2.toString() );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListVersionWithPrefixAndDelimiterThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            List< String > actCommonPrefixes = new ArrayList< String >();
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                ListVersionsRequest req = new ListVersionsRequest()
                        .withBucketName( bucketName ).withPrefix( prefix )
                        .withDelimiter( delimiter );
                VersionListing versionList = s3Client.listVersions( req );
                while ( true ) {
                    List< String > commprefixesResult = versionList
                            .getCommonPrefixes();
                    for ( String s : commprefixesResult ) {
                        actCommonPrefixes.add( s );
                    }

                    if ( versionList.isTruncated() ) {
                        versionList = s3Client
                                .listNextBatchOfVersions( versionList );
                    } else {
                        break;
                    }
                }
                Assert.assertEquals( actCommonPrefixes, expResultList3,
                        "the returned result by commonperfixes is wrong, act: "
                                + actCommonPrefixes.toString() + ", exp: "
                                + expResultList3.toString() );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
