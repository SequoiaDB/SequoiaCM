package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @description SCM-4417:上传分段过程中源文件被删除
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4417 extends TestScmBase {
    private boolean runSuccess = false;
    private File localPath;
    private String filePath1;
    private String filePath2;
    private String filePath3;
    private File file;
    private int fileSize = 50 * 1024 * 1024;

    private AmazonS3 s3Client;
    private String bucketName = "bucket4417";
    private String key = "obj4417";
    private int partsNum = 10;
    private String uploadId;
    private List< PartETag > partETags = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName, key );

        ThreadExecutor threadExec = new ThreadExecutor();
        ThreadUploadParts threadUploadPart = new ThreadUploadParts();
        threadExec.addWorker( threadUploadPart );
        threadExec.addWorker( new ThreadRemoveFile() );
        threadExec.run();

        // check results
        if ( partETags.size() > 0 ) {
            PartUploadUtils.completeMultipartUpload( s3Client, bucketName, key,
                    uploadId, partETags );
            String expFilePath;
            if ( threadUploadPart.getRetCode() == 0 ) {
                // all part upload success
                expFilePath = filePath2;
            } else {
                // multi part upload success;
                Assert.assertNotEquals( partETags.size(), partsNum );
                expFilePath = filePath3;
                int len = ( fileSize / partsNum ) * partETags.size();
                TestTools.LocalFile.readFile( filePath2, 0, len, filePath3 );
            }
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, key );
            Assert.assertEquals( downfileMd5, TestTools.getMD5( expFilePath ) );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                s3Client.deleteObject( bucketName, key );
                s3Client.deleteBucket( bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void initFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        filePath1 = localPath + File.separator + "localFile_" + fileSize
                + "_1.txt";
        TestTools.LocalFile.createFile( filePath1, fileSize );
        file = new File( filePath1 );

        filePath2 = localPath + File.separator + "localFile_" + fileSize
                + "_2.txt";
        TestTools.LocalFile.createFile( filePath2, 0 );
        TestTools.LocalFile.readFile( filePath1, 0, fileSize, filePath2 );

        filePath3 = localPath + File.separator + "localFile_3.txt";
        TestTools.LocalFile.createFile( filePath3, 0 );
    }

    private class ThreadUploadParts extends ResultStore {
        @ExecuteOrder(step = 1)
        private void uploadParts() throws Exception {
            AmazonS3 s3 = null;
            try {
                s3 = S3Utils.buildS3Client();
                int partSize = fileSize / partsNum;
                for ( int i = 0; i < partsNum; i++ ) {
                    UploadPartRequest partRequest = new UploadPartRequest()
                            .withBucketName( bucketName ).withKey( key )
                            .withUploadId( uploadId ).withFile( file )
                            .withFileOffset( partSize * i )
                            .withPartNumber( i + 1 ).withPartSize( partSize );
                    UploadPartResult partResult = s3.uploadPart( partRequest );
                    partETags.add( partResult.getPartETag() );
                }
            } catch ( IllegalArgumentException e ) {
                if ( e.getMessage().indexOf( "Failed to open file" ) != 0 ) {
                    throw e;
                }
                saveResult( -1, e );
            } finally {
                if ( s3 != null ) {
                    s3.shutdown();
                }
            }
        }
    }

    private class ThreadRemoveFile {
        @ExecuteOrder(step = 1)
        private void removeFile() throws InterruptedException {
            Thread.sleep( new Random().nextInt( 10 ) );
            TestTools.LocalFile.removeFile( filePath1 );
        }
    }
}