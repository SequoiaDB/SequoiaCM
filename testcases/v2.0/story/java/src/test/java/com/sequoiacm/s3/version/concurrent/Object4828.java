package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @descreption SCM-4828 :: 禁用版本控制，并发增加和获取对象列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4828 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4828";
    private String keyName = "aa/bb/object4828";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private int objectNums = 10;
    private List< String > keyList = new ArrayList<>();

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
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.SUSPENDED );
    }

    @Test
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < objectNums; i++ ) {
            String key = keyName + "_" + i;
            keyList.add( key );
            te.addWorker( new PutObjectThread( key ) );
        }
        te.addWorker(new ListObjectThread());
        te.run();

        listObjectsAndCheckResult( keyList );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
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

    private void listObjectsAndCheckResult( List< String > keyList ) {
        List< String > queryKeyList = new ArrayList<>();
        ListObjectsV2Result result = s3Client.listObjectsV2( bucketName );
        List<S3ObjectSummary> objects = result.getObjectSummaries();
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

    private class PutObjectThread extends ResultStore {
        private String keyName;

        public PutObjectThread( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
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

    private class ListObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.listObjectsV2( bucketName );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
