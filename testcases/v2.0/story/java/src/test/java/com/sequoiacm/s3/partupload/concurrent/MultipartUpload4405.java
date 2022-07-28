package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
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

/**
 * @description SCM-4405:相同key相同uploadId并发终止分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4405 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "/aa/object4405";
    private String bucketName = "bucket4405";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String[] filePaths = new String[ 5 ];
    private int[] fileSizes = { 1024 * 1024 * 50, 1024 * 1024 * 29,
            1024 * 1024 * 30, 1024 * 1024 * 10, 1024 * 1024 * 40 };
    private int successNum = 0;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSizes[ i ] + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSizes[ i ] );
            filePaths[ i ] = filePath;
        }

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void abortMultipartUpload() throws Exception {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        ThreadExecutor threadExec = new ThreadExecutor();
        int[] partSizes = { 1024 * 1024 * 5, 1024 * 1024 * 6, 1024 * 1024 * 6,
                1024 * 1024 * 5, 1024 * 1024 * 10 };
        for ( int i = 0; i < filePaths.length; i++ ) {
            String filePath = filePaths[ i ];
            int partSize = partSizes[ i ];
            threadExec.addWorker(
                    new AbortMultipartUpload( uploadId, filePath, partSize ) );
        }
        threadExec.run();

        // check the upload file
        checkResult();
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
            s3Client.shutdown();
        }
    }

    private void checkResult() {
        // only one AbortMultipartUpload success
        int expSuccessNum = 1;
        Assert.assertEquals( successNum, expSuccessNum );
        // get key is not exist.
        try {
            s3Client.getObject( bucketName, keyName );
            Assert.fail( "get not exist key must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchKey" );
        }
    }

    private class AbortMultipartUpload extends ResultStore {
        private String filePath;
        private String uploadId;
        private int partSize;
        private File file = null;
        private AmazonS3 s3Client1 = S3Utils.buildS3Client();

        private AbortMultipartUpload( String uploadId, String filePath,
                int partSize ) throws Exception {
            this.uploadId = uploadId;
            this.filePath = filePath;
            this.partSize = partSize;
        }

        @ExecuteOrder(step = 1)
        private void partUpload() {
            file = new File( filePath );
            PartUploadUtils.partUpload( s3Client1, bucketName, keyName,
                    uploadId, file, partSize );
        }

        @ExecuteOrder(step = 2)
        private void completeMultipartUpload() {
            try {
                AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                        bucketName, keyName, uploadId );
                s3Client.abortMultipartUpload( request );
                successNum++;
            } catch ( AmazonS3Exception e ) {
                int errCode = e.getStatusCode();
                // 404:NoSuchUpload
                if ( errCode != 404 ) {
                    throw e;
                }
            } finally {
                if ( s3Client1 != null ) {
                    s3Client1.shutdown();
                }
            }
        }
    }
}
