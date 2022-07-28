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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4407:不同key并发终止分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4413B extends TestScmBase {
    private boolean runSuccess = false;
    private String baseKeyName = "/aa/object4413b";
    private int keyNum = 20;
    private List< String > keyNames = new ArrayList<>();
    private String bucketName = "bucket4413b";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024 * 28;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void abortMultipartUpload() throws Exception {
        File file = new File( filePath );
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < keyNum; i++ ) {
            String keyName = baseKeyName + "/" + i + "_.txt";
            threadExec.addWorker( new AbortMultipartUpload( file, keyName ) );
            keyNames.add( keyName );
        }
        threadExec.run();
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
        for ( int i = 0; i < keyNum; i++ ) {
            String keyName = keyNames.get( i );
            // get key is not exist.
            try {
                s3Client.getObject( bucketName, keyName );
                Assert.fail( "get not exist key must be fail !" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "NoSuchKey" );
            }
        }

    }

    private class AbortMultipartUpload extends ResultStore {
        private String uploadId;
        private String keyName;
        private File file = null;
        private AmazonS3 s3Client1 = S3Utils.buildS3Client();

        private AbortMultipartUpload( File file, String keyName ) throws Exception {
            this.file = file;
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void initPartUpload() {
            uploadId = PartUploadUtils.initPartUpload( s3Client1, bucketName,
                    keyName );
        }

        @ExecuteOrder(step = 2)
        private void partUpload() {
            PartUploadUtils.partUpload( s3Client1, bucketName, keyName,
                    uploadId, file );
        }

        @ExecuteOrder(step = 3)
        private void completeMultipartUpload() throws IOException {
            try {
                AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                        bucketName, keyName, uploadId );
                s3Client.abortMultipartUpload( request );
            } finally {
                if ( s3Client1 != null ) {
                    s3Client1.shutdown();
                }
            }
        }
    }
}
