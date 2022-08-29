package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4811 :: 开启版本控制，并发获取相同对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4811 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4811";
    private String keyName = "key4811";
    private String content = "content4811";
    private int versionNum = 3;
    private List< String > etagList = new ArrayList<>();
    private String[] acessKeys = null;
    private AmazonS3 s3Client = null;
    private File localPath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        // put three versions of the object
        for ( int i = 0; i < versionNum; i++ ) {
            String currentContent = content + S3Utils.getRandomString( i );
            s3Client.putObject( bucketName, keyName, currentContent );
            etagList.add( TestTools.getMD5( currentContent.getBytes() ) );
        }
    }

    @Test
    public void test() throws Exception {
        int threadNum = 10;
        // test a : Getting object without specified versions
        ThreadExecutor teA = new ThreadExecutor();
        for ( int i = 0; i < threadNum; i++ ) {
            teA.addWorker( new GetObjectThread() );
        }
        teA.run();

        // test b : Getting object with the same versionId
        ThreadExecutor teB = new ThreadExecutor( );
        for ( int i = 0; i < threadNum; i++ ) {
            teB.addWorker( new GetSameVersionThread() );
        }
        teB.run();

        // test c : Getting object with the different versionId
        ThreadExecutor teC = new ThreadExecutor( );
        for ( int i = 0; i < threadNum; i++ ) {
            teC.addWorker(
                    new GetDiffVersionThread( ( i % versionNum + 1 ) + ".0" ) );
        }
        teC.run();

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

    private class GetObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            AmazonS3 s3ClientT = S3Utils.buildS3Client();
            try {
                S3Object object = s3ClientT.getObject( bucketName, keyName );
                S3ObjectInputStream s3is = object.getObjectContent();
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                S3Utils.inputStream2File( s3is, downloadPath );
                s3is.close();
                String getObjectMd5 = TestTools.getMD5( downloadPath );
                Assert.assertEquals( getObjectMd5, etagList.get( 2 ),
                        "md5 is wrong!" );
                ObjectMetadata metadata = object.getObjectMetadata();
                Assert.assertEquals( metadata.getVersionId(), "3.0" );
            } finally {
                if ( s3ClientT != null ) {
                    s3ClientT.shutdown();
                }
            }
        }
    }

    private class GetSameVersionThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3ClientT = S3Utils.buildS3Client();
            try {
                S3Object object = s3ClientT.getObject(
                        new GetObjectRequest( bucketName, keyName, "2.0" ) );
                S3ObjectInputStream s3is = object.getObjectContent();
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                S3Utils.inputStream2File( s3is, downloadPath );
                s3is.close();
                String getObjectMd5 = TestTools.getMD5( downloadPath );
                Assert.assertEquals( getObjectMd5, etagList.get( 1 ),
                        "md5 is wrong!" );
                ObjectMetadata metadata = object.getObjectMetadata();
                Assert.assertEquals( metadata.getVersionId(), "2.0" );
            } finally {
                if ( s3ClientT != null ) {
                    s3ClientT.shutdown();
                }
            }
        }
    }

    private class GetDiffVersionThread extends ResultStore {
        String versionId;

        public GetDiffVersionThread( String versionid ) {
            this.versionId = versionid;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3ClientT = S3Utils.buildS3Client();
            try {
                S3Object object = s3ClientT.getObject( new GetObjectRequest(
                        bucketName, keyName, versionId ) );
                S3ObjectInputStream s3is = object.getObjectContent();
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                S3Utils.inputStream2File( s3is, downloadPath );
                s3is.close();
                String getObjectMd5 = TestTools.getMD5( downloadPath );
                Assert.assertEquals( getObjectMd5,
                        etagList.get( Integer.parseInt( versionId.split("\\.")[0] ) - 1 ),
                        "md5 is wrong!" );
                ObjectMetadata metadata = object.getObjectMetadata();
                Assert.assertEquals( metadata.getVersionId(), versionId );
            } finally {
                if ( s3ClientT != null ) {
                    s3ClientT.shutdown();
                }
            }
        }
    }
}
