package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @description SCM-4419:并发上传分段，指定partNumber不连续
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4419 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4419";
    private String keyName = "key4419";
    private AmazonS3 s3Client = null;
    private long fileSize = 220 * 1024 * 1024;
    private long partSize = 5 * 1024 * 1024;
    private File localPath = null;
    private File file = null;
    private String uploadId;
    private List< String > expEtagList = new ArrayList<>();
    private List< PartETag > partEtags = new CopyOnWriteArrayList<>();
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
        ThreadExecutor es = new ThreadExecutor();
        int filePosition = 0;

        for ( int i = 1; filePosition < fileSize; i += 2 ) {
            es.addWorker( new ThreadUploadPart4419( filePosition, i ) );
            expEtagList.add( TestTools.getLargeFilePartMD5( file, filePosition,
                    partSize ) );
            filePosition += partSize;
        }
        es.run();

        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );
        String expMd5 = TestTools.getMD5( filePath );
        String downloadMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downloadMd5, expMd5 );
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

    class ThreadUploadPart4419 {
        private AmazonS3 s3Client = S3Utils.buildS3Client();
        private long filePosition;
        private int partNumber;

        public ThreadUploadPart4419( long filePosition, int partNumber )
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
                UploadPartResult result = s3Client.uploadPart( partRequest );
                partEtags.add( result.getPartETag() );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}
