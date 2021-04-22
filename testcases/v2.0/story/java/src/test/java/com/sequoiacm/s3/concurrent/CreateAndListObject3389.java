package com.sequoiacm.s3.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @Description SCM-3389:并发增加和获取对象列表
 * @author fanyu
 * @Date 2019.1.8
 * @version 1.00
 */
public class CreateAndListObject3389 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket3389";
    private String keyName = "aa/bb/object3389";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private int objectNums = 10;
    private List< String > keyList = new ArrayList<>();
    private List< String > queryKeyList1 = new ArrayList<>();
    private List< String > queryKeyList2 = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void testCreateBucket() throws Exception {
        ThreadExecutor threadExec = new ThreadExecutor();
        threadExec.addWorker( new ListObjectThread() );
        threadExec.addWorker( new ListObjectV1Thread() );
        for ( int i = 0; i < objectNums; i++ ) {
            String key = keyName + "_" + i;
            keyList.add( key );
            threadExec.addWorker( new PutObjectThread( key ) );
        }
        threadExec.run();
        // check the query keys by listObjectv1 and listObjectv2, than check the
        // all keys
        listObjectResult( queryKeyList1 );
        listObjectResult( queryKeyList2 );
        listObjectsAndCheckResult( keyList );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void listObjectsAndCheckResult( List< String > keyList )
            throws IOException {
        List< String > queryKeyList = new ArrayList<>();
        ListObjectsV2Result result = s3Client.listObjectsV2( bucketName );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        Assert.assertEquals( objects.size(), objectNums );
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            queryKeyList.add( key );
        }

        // check the keyName
        Collections.sort( keyList );
        Collections.sort( queryKeyList );
        Assert.assertEquals( queryKeyList, keyList );
    }

    private void listObjectResult( List< String > queryKeyList ) {
        for ( String key : queryKeyList ) {
            if ( !keyList.contains( key ) ) {
                Assert.fail( "list key error!,the key is " + key
                        + "\nqueryList:" + queryKeyList.toString() );
            }
        }

    }

    private class PutObjectThread {
        private String keyName;

        public PutObjectThread( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.putObject( bucketName, keyName, new File( filePath ) );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListObjectThread {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                ListObjectsV2Result result = s3Client
                        .listObjectsV2( bucketName );
                List< S3ObjectSummary > objects = result.getObjectSummaries();
                for ( S3ObjectSummary os : objects ) {
                    String key = os.getKey();
                    queryKeyList2.add( key );
                }
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class ListObjectV1Thread {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {

                ObjectListing result = s3Client.listObjects( bucketName );
                List< S3ObjectSummary > objects = result.getObjectSummaries();
                for ( S3ObjectSummary os : objects ) {
                    String key = os.getKey();
                    queryKeyList1.add( key );
                }
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
