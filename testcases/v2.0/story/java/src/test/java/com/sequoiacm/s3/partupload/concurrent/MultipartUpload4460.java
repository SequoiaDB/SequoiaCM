package com.sequoiacm.s3.partupload.concurrent;

import java.io.File;
import java.util.List;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiadb.threadexecutor.ResultStore;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PartETag;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-4460:复制分段上传过程中源文件被删除
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4460 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String targetBucketName = "targetbucket4460";
    private String sourceBucketName = "sourcebucket4460";
    private String targetKey = "/aa/bb/targetobj4460";
    private String sourceKey = "/aa/bb/sourceobj4460";
    private int fileSize = 1024 * 1024 * 30;
    private long sourceObjectSize;
    private String uploadId;
    private File localPath = null;
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

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, targetBucketName );
        S3Utils.clearBucket( s3Client, sourceBucketName );
        s3Client.createBucket( targetBucketName );
        s3Client.createBucket( sourceBucketName );

        s3Client.putObject( sourceBucketName, sourceKey, new File( filePath ) );
        sourceObjectSize = s3Client.getObject( sourceBucketName, sourceKey )
                .getObjectMetadata().getContentLength();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, targetBucketName,
                targetKey );

        ThreadExecutor es = new ThreadExecutor();
        ThreadUploadPartCopy uploadPartCopyThread = new ThreadUploadPartCopy(
                sourceBucketName, sourceKey, targetBucketName, targetKey );
        es.addWorker( uploadPartCopyThread );
        es.addWorker( new ThreadDeleteFile() );
        es.run();

        if ( uploadPartCopyThread.getRetCode() != 404
                && uploadPartCopyThread.getRetCode() != 0 ) {
            Assert.fail( "statuscode error,act statuscode ="
                    + uploadPartCopyThread.getRetCode() );
        }

        try {
            s3Client.getObject( sourceBucketName, sourceKey );
            Assert.fail( "getObject must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchKey",
                    "---statuscode=" + e.getStatusCode() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, targetBucketName );
                S3Utils.clearBucket( s3Client, sourceBucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    public void uploadPartCopy( AmazonS3 s3Client, String sourceBucketName,
            String sourceKey, String targetBucketName, String targetKey )
            throws Exception {
        List< PartETag > partEtags = PartUploadUtils.partUploadCopy( s3Client,
                sourceBucketName, sourceKey, targetBucketName, targetKey,
                uploadId, sourceObjectSize );
        PartUploadUtils.completeMultipartUpload( s3Client, targetBucketName,
                targetKey, uploadId, partEtags );

        // down file check the file content
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                targetBucketName, targetKey );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    public class ThreadUploadPartCopy extends ResultStore {
        private String sourceBucketName;
        private String sourceKey;
        private String targetBucketName;
        private String targetKey;

        public ThreadUploadPartCopy( String sourceBucketName, String sourceKey,
                String targetBucketName, String targetKey ) {
            this.sourceBucketName = sourceBucketName;
            this.sourceKey = sourceKey;
            this.targetBucketName = targetBucketName;
            this.targetKey = targetKey;
        }

        @ExecuteOrder(step = 1)
        public void putObject() throws Exception {
            AmazonS3 s3 = null;
            try {
                s3 = S3Utils.buildS3Client();
                uploadPartCopy( s3, sourceBucketName, sourceKey,
                        targetBucketName, targetKey );
            } catch ( AmazonS3Exception e ) {
                saveResult( e.getStatusCode(), e );
            } finally {
                if ( s3 != null ) {
                    s3.shutdown();
                }
            }
        }
    }

    public class ThreadDeleteFile {
        @ExecuteOrder(step = 1)
        public void putObject() throws Exception {
            AmazonS3 s3 = null;
            try {
                s3 = S3Utils.buildS3Client();
                s3.deleteObject( sourceBucketName, sourceKey );
            } finally {
                if ( s3 != null ) {
                    s3.shutdown();
                }
            }
        }
    }
}