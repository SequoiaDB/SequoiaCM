package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
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
import java.util.List;

/**
 * @descreption SCM-4824 :: 禁用版本控制，并发获取不同版本同一对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4824 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4824";
    private String keyName = "key4824";
    private int fileSizeV1 = 1024 * 1024 * 2;
    private int fileSizeV2 = 1024 * 3;
    private int fileSizeV3 = 1024 * 3;
    private File localPath = null;
    private String filePathV1 = null;
    private String filePathV2 = null;
    private String filePathV3 = null;
    private List< String > etagList = new ArrayList<>();
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePathV1 = localPath + File.separator + "localFile_" + fileSizeV1
                + ".txt";
        filePathV2 = localPath + File.separator + "localFile_" + fileSizeV2
                + ".txt";
        filePathV3 = localPath + File.separator + "localFile_" + fileSizeV3
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePathV1, fileSizeV1 );
        TestTools.LocalFile.createFile( filePathV2, fileSizeV2 );
        TestTools.LocalFile.createFile( filePathV3, fileSizeV3 );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        // put three versions
        s3Client.putObject( bucketName, keyName, new File( filePathV1 ) );
        etagList.add( S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName ) );

        s3Client.putObject( bucketName, keyName, new File( filePathV2 ) );
        etagList.add( S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName ) );

        s3Client.putObject( bucketName, keyName, new File( filePathV3 ) );
        etagList.add( S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName ) );

        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
    }

    @Test
    public void test() throws Exception {
        // Getting different version of objects
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < 3; i++ ) {
            te.addWorker( new GetDifferentObjectThread( ( i + 1 ) + ".0" ) );
        }
        te.run();

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

    private class GetDifferentObjectThread extends ResultStore {
        String versionId;

        public GetDifferentObjectThread( String versionId ) {
            this.versionId = versionId;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                S3Object object = s3Client.getObject( new GetObjectRequest(
                        bucketName, keyName, versionId ) );
                S3ObjectInputStream s3is = object.getObjectContent();
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                S3Utils.inputStream2File( s3is, downloadPath );
                s3is.close();
                String getObjectMd5 = TestTools.getMD5( downloadPath );
                Assert.assertEquals( getObjectMd5, etagList.get(
                        Integer.parseInt( versionId.split( "\\." )[ 0 ] ) - 1 ),
                        "md5 is wrong!" );
                Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                        versionId );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
