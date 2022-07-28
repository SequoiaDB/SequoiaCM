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
 * @description SCM-4336:上传多个分段存在partNum为1、连续分段号且分段长度不一致
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4336 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "/aa/object4336";
    private String bucketName = "bucket4336";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024 * 36;
    private List< PartETag > partEtags = Collections
            .synchronizedList( new ArrayList< PartETag >() );

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
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void uploadParts() throws Exception {
        File file = new File( filePath );
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );

        ThreadExecutor threadExec = new ThreadExecutor();
        int partNums = 5;
        int[] partSizes = { 1024 * 1024 * 6, 1024 * 1024 * 8, 1024 * 1024 * 5,
                1024 * 1024 * 10, 1024 * 1024 * 7 };
        int[] offSets = { 0, 1024 * 1024 * 6, 1024 * 1024 * 14,
                1024 * 1024 * 19, 1024 * 1024 * 29 };
        for ( int i = 0; i < partNums; i++ ) {
            int partNum = i + 1;
            int partSize = partSizes[ i ];
            int offSet = offSets[ i ];
            threadExec.addWorker( new PartUpload( partNum, partSize, offSet,
                    file, uploadId ) );
        }
        threadExec.run();

        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );

        // check the upload file
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
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

    private class PartUpload {
        private int partNum;
        private int partSize;
        private int filePosition;
        private File file;
        private String uploadId;
        private AmazonS3 s3Client1 = S3Utils.buildS3Client();

        private PartUpload( int partNum, int partSize, int filePosition,
                File file, String uploadId ) throws Exception {
            this.partNum = partNum;
            this.partSize = partSize;
            this.filePosition = filePosition;
            this.file = file;
            this.uploadId = uploadId;
        }

        @ExecuteOrder(step = 1)
        private void partUpload() {
            try {
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
}
