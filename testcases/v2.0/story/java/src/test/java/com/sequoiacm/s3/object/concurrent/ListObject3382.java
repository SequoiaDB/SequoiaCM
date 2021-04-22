package com.sequoiacm.s3.object.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description: SCM-3382:并发不同条件查询对象列表,覆盖listObjectV1和listObjectV2
 * @author fanyu
 * @Date 2019.01.03
 * @version 1.00
 */
public class ListObject3382 extends TestScmBase {
    private String bucketName = "bucket3382";
    private String keyName = "dir/dir";
    private String prefix = "dir";
    private String delimiter = "/";
    private List< String > expresultList1 = new ArrayList< String >();
    private List< String > expresultList2 = new ArrayList< String >();
    private List< String > expresultList3 = new ArrayList< String >();
    private int objectTotalNum = 100;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );

        // put multiple objects
        for ( int i = 0; i < objectTotalNum; i++ ) {
            String currentKeyName = keyName + i + "/3382";
            s3Client.putObject( bucketName, currentKeyName, "object_file3382" );
            expresultList1.add( currentKeyName );
            expresultList2.add( currentKeyName );
        }

        // put another objects that do not match prefix
        s3Client.putObject( bucketName, "testa3382", "object_file3382" );
        s3Client.putObject( bucketName, "testb3382", "object_file3382" );
        expresultList1.add( "testa3382" );
        expresultList1.add( "testb3382" );

        Collections.sort( expresultList1 );
        Collections.sort( expresultList2 );
        expresultList3.add( "dir/" );
    }

    @Test
    public void testGetObjectList() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new ListObject() );
        threadExec.addWorker( new ListObjectV1() );
        threadExec.addWorker( new ListObjectWithPerfix() );
        threadExec.addWorker( new ListObjectWithPerfixAndDelimiter() );
        threadExec.addWorker( new ListObjectV1WithPerfixAndDelimiter() );
        threadExec.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                deleteObjectsAndBucket();
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void deleteObjectsAndBucket() {
        for ( int i = 0; i < expresultList1.size(); i++ ) {
            s3Client.deleteObject( bucketName, expresultList1.get( i ) );
        }
        s3Client.deleteBucket( bucketName );
    }

    private class ListObject {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                List< S3ObjectSummary > contentsResult = new ArrayList<>();
                ListObjectsV2Request req = new ListObjectsV2Request()
                        .withBucketName( bucketName );
                ListObjectsV2Result result;

                do {
                    result = s3Client.listObjectsV2( req );
                    contentsResult.addAll( result.getObjectSummaries() );
                    String nextContinuationToken = result
                            .getNextContinuationToken();
                    req.setContinuationToken( nextContinuationToken );
                } while ( result.isTruncated() );

                S3Utils.checkListObjectsV2KeyName( contentsResult,
                        expresultList1 );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListObjectV1 {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                List< S3ObjectSummary > contentsResult = new ArrayList<>();
                ListObjectsRequest req = new ListObjectsRequest()
                        .withBucketName( bucketName );
                ObjectListing result;

                do {
                    result = s3Client.listObjects( req );
                    contentsResult.addAll( result.getObjectSummaries() );
                    String marker = result.getNextMarker();
                    req.setMarker( marker );
                } while ( result.isTruncated() );

                S3Utils.checkListObjectsV2KeyName( contentsResult,
                        expresultList1 );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListObjectWithPerfix {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                List< S3ObjectSummary > contentsResult = new ArrayList<>();
                ListObjectsV2Request req = new ListObjectsV2Request()
                        .withBucketName( bucketName ).withPrefix( prefix );
                ListObjectsV2Result result;

                do {
                    result = s3Client.listObjectsV2( req );
                    contentsResult.addAll( result.getObjectSummaries() );
                    String nextContinuationToken = result
                            .getNextContinuationToken();
                    req.setContinuationToken( nextContinuationToken );
                } while ( result.isTruncated() );
                S3Utils.checkListObjectsV2KeyName( contentsResult,
                        expresultList2 );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListObjectWithPerfixAndDelimiter {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                List< String > commprefixesResult = new ArrayList<>();
                ListObjectsV2Request req = new ListObjectsV2Request()
                        .withBucketName( bucketName ).withPrefix( prefix )
                        .withDelimiter( delimiter );
                ListObjectsV2Result result;

                do {
                    result = s3Client.listObjectsV2( req );
                    commprefixesResult.addAll( result.getCommonPrefixes() );
                    String nextContinuationToken = result
                            .getNextContinuationToken();
                    req.setContinuationToken( nextContinuationToken );
                } while ( result.isTruncated() );

                S3Utils.checkListObjectsV2Commprefixes( commprefixesResult,
                        expresultList3 );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListObjectV1WithPerfixAndDelimiter {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                List< String > commprefixesResult = new ArrayList<>();
                ListObjectsRequest req = new ListObjectsRequest()
                        .withBucketName( bucketName ).withPrefix( prefix )
                        .withDelimiter( delimiter );
                ObjectListing result;
                do {
                    result = s3Client.listObjects( req );
                    commprefixesResult.addAll( result.getCommonPrefixes() );
                    String nextMarker = result.getNextMarker();
                    req.setMarker( nextMarker );
                } while ( result.isTruncated() );

                S3Utils.checkListObjectsV2Commprefixes( commprefixesResult,
                        expresultList3 );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
