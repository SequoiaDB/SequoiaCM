package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description SCM-4411:相同对象相同uploadId并发上传分段和完成分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4411 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "/aa/object4411";
    private String bucketName = "bucket4411";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024 * 100;
    private int partSize = 1024 * 1024 * 20;
    private String uploadId = null;
    private List< PartETag > partEtags = Collections
            .synchronizedList( new ArrayList< PartETag >() );
    private boolean isCompleteMultipartUploadOK = false;

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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void uploadParts() throws Exception {
        File file = new File( filePath );
        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );

        ThreadExecutor threadExec = new ThreadExecutor();
        int partNums = fileSize / partSize;
        for ( int i = 0; i < partNums; i++ ) {
            int partNum = i + 1;
            threadExec.addWorker(
                    new PartUpload( partNum, partSize, file, uploadId ) );
        }

        threadExec.addWorker( new CompletePartUpload( uploadId ) );
        threadExec.run();

        if ( isCompleteMultipartUploadOK ) {
            // get the upload object to check content by md5
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, keyName );
            Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
        } else {
            // check the upload part info
            PartUploadUtils.listPartsAndCheckPartNumbers( s3Client, bucketName,
                    keyName, partEtags, uploadId );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                        bucketName, keyName, uploadId );
                s3Client.abortMultipartUpload( request );
                s3Client.deleteObject( bucketName, keyName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private class PartUpload {
        private int partNum;
        private int partSize;
        private File file;
        private String uploadId;
        private AmazonS3 s3Client1 = S3Utils.buildS3Client();

        private PartUpload( int partNum, int partSize, File file,
                String uploadId ) throws Exception {
            this.partNum = partNum;
            this.partSize = partSize;
            this.file = file;
            this.uploadId = uploadId;
        }

        @ExecuteOrder(step = 1)
        private void partUpload() {
            try {
                int filePosition = ( partNum - 1 ) * partSize;
                UploadPartRequest partRequest = new UploadPartRequest()
                        .withFile( file ).withFileOffset( filePosition )
                        .withPartNumber( partNum ).withPartSize( partSize )
                        .withBucketName( bucketName ).withKey( keyName )
                        .withUploadId( uploadId );
                UploadPartResult uploadPartResult = s3Client1
                        .uploadPart( partRequest );
                partEtags.add( uploadPartResult.getPartETag() );
            } finally {
                if ( s3Client1 != null ) {
                    s3Client1.shutdown();
                }
            }
        }
    }

    private class CompletePartUpload {
        private String uploadId;
        private AmazonS3 s3Client2 = S3Utils.buildS3Client();

        private CompletePartUpload( String uploadId ) throws Exception {
            this.uploadId = uploadId;
        }

        @ExecuteOrder(step = 1)
        private void completeMultipartUpload() {
            try {
                PartUploadUtils.completeMultipartUpload( s3Client, bucketName,
                        keyName, uploadId, partEtags );
                isCompleteMultipartUploadOK = true;
            } catch ( AmazonS3Exception e ) {
                int errCode = e.getStatusCode();
                // 400: InvalidPart
                if ( errCode != 400 ) {
                    throw e;
                }
            } finally {
                if ( s3Client2 != null ) {
                    s3Client2.shutdown();
                }
            }
        }
    }
}
