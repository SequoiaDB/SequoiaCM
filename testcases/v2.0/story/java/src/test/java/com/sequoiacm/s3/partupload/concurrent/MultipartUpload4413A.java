package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;

/**
 * @description SCM-4413:上传分段过程中终止分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4413A extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4413a";
    private String keyName = "key4413a";
    private AmazonS3 s3Client = null;
    private long fileSize = 100 * 1024;
    private long partSize = 5 * 1024;
    private File localPath = null;
    private File file = null;
    private String uploadId;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testUpload() throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        ThreadExecutor es = new ThreadExecutor( 300000 );
        int filePosition = 0;
        for ( int i = 1; filePosition < fileSize; i++ ) {
            es.addWorker( new ThreadUploadPart18774( filePosition, i ) );
            filePosition += partSize;
        }
        es.addWorker( new ThreadAbortMultipartUpload4413() );
        es.run();

        ListPartsRequest request = new ListPartsRequest( bucketName, keyName,
                uploadId );
        try {
            s3Client.listParts( request );
            Assert.fail( "listParts should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchUpload" );
        }

        Assert.assertFalse( s3Client.doesObjectExist( bucketName, keyName ) );
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

    class ThreadUploadPart18774 {
        private AmazonS3 s3Client = S3Utils.buildS3Client();
        private long filePosition;
        private int partNumber;

        public ThreadUploadPart18774( long filePosition, int partNumber )
                throws Exception {
            this.filePosition = filePosition;
            this.partNumber = partNumber;
        }

        @ExecuteOrder(step = 1, desc = "分段上传对象")
        public void UploadPart() {
            try {
                UploadPartRequest partRequest = new UploadPartRequest()
                        .withFile( file ).withFileOffset( filePosition )
                        .withPartNumber( partNumber ).withPartSize( partSize )
                        .withBucketName( bucketName ).withKey( keyName )
                        .withUploadId( uploadId );
                s3Client.uploadPart( partRequest );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "NoSuchUpload" );
            } finally {
                s3Client.shutdown();
            }
        }
    }

    class ThreadAbortMultipartUpload4413 {
        private AmazonS3 s3Client = S3Utils.buildS3Client();

        ThreadAbortMultipartUpload4413() throws Exception {
        }

        @ExecuteOrder(step = 1, desc = "终止分段上传")
        public void UploadPart() {
            try {
                AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                        bucketName, keyName, uploadId );
                s3Client.abortMultipartUpload( request );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}
