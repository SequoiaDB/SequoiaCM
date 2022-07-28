package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PartETag;
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
 * @description SCM-4341:指定partlist中分段数和已上传分段数不同
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4341 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4341";
    private String keyName = "key4341";
    private AmazonS3 s3Client = null;
    private long fileSize = 24 * 1024 * 1024;
    private File localPath = null;
    private File file = null;
    private File file2 = null;
    private String filePath = null;
    private String filePath2 = null;
    private List< AmazonS3 > clientList = Collections
            .synchronizedList( new ArrayList< AmazonS3 >() );

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );
        file = new File( filePath );
        file2 = new File( filePath2 );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testUpload() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadUploadPart4341( 6 * 1024 * 1024, file ) );
        es.addWorker( new ThreadUploadPart4341( 6 * 1024 * 1024, file2 ) );
        es.addWorker( new ThreadUploadPart4341( 5 * 1024 * 1024, file ) );
        es.run();
        // 未开启版本控制，对象内容为最后一次完成分段上传的对象内容，因只上传了两种内容file和file2，故实际对象内容应为2者中的一种
        String expMd5 = TestTools.getMD5( filePath );
        String expMd5_2 = TestTools.getMD5( filePath2 );
        String downloadMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        if ( !downloadMd5.equals( expMd5 )
                && !downloadMd5.equals( expMd5_2 ) ) {
            Assert.fail( "actMd5 = " + downloadMd5 + "expMd5=[" + expMd5 + ", "
                    + expMd5_2 + "]" );
        }
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
            for ( AmazonS3 client : clientList ) {
                if ( client != null ) {
                    client.shutdown();
                }
            }
        }
    }

    class ThreadUploadPart4341 {
        private AmazonS3 inner_s3Client;
        private long partSize;
        private File file;
        private String uploadId;
        private List< PartETag > partEtags = new ArrayList<>();

        public ThreadUploadPart4341( long partSize, File file )
                throws Exception {
            inner_s3Client = S3Utils.buildS3Client();
            this.partSize = partSize;
            this.file = file;
            clientList.add( inner_s3Client );
        }

        @ExecuteOrder(step = 1, desc = "初始化分段上传")
        public void initPartUpload() {
            uploadId = PartUploadUtils.initPartUpload( inner_s3Client,
                    bucketName, keyName );
        }

        @ExecuteOrder(step = 2, desc = "分段上传对象")
        public void UploadPart() {
            partEtags = PartUploadUtils.partUpload( inner_s3Client, bucketName,
                    keyName, uploadId, file, partSize );
        }

        @ExecuteOrder(step = 3, desc = "完成分段上传")
        public void CompleteMultipartUpload() {
            PartUploadUtils.completeMultipartUpload( inner_s3Client, bucketName,
                    keyName, uploadId, partEtags );
        }
    }
}
