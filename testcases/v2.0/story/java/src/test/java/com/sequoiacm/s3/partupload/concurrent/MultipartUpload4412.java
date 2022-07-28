package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4412:相同对象不同uploadId并发上传分段和完成分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4412 extends TestScmBase {
    private boolean runSuccess = false;
    private File localPath;
    private String filePath;
    private String expFilePath;
    private File file;
    private int fileSize = 25 * 1024 * 1024;

    private AmazonS3 s3Client;
    private String bucketName = "bucket4412";
    private String key = "obj4412";
    private List< String > uploadIds = new ArrayList<>();
    private int uploadPartTimes = 5;
    private int firstUploadPartTimes = 2;
    private int partSize = fileSize / uploadPartTimes;
    private List< PartETag > partETags = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        // init part upload
        for ( int i = 0; i < uploadPartTimes; i++ ) {
            String uploadId = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, key );
            uploadIds.add( uploadId );
        }

        // upload multi part
        for ( int i = 0; i < firstUploadPartTimes; i++ ) {
            int fileOffset = partSize * i;
            int partNumber = i + 1;
            this.uploadFirstPart( uploadIds.get( 0 ), fileOffset, partNumber,
                    partSize );
        }

        // upload multi part again, and complete upload
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = firstUploadPartTimes; i < uploadPartTimes
                - firstUploadPartTimes; i++ ) {
            int fileOffset = partSize * i;
            int partNumber = i + 1;
            threadExec.addWorker(
                    new ThreadUploadOtherParts( uploadIds.get( i + 1 ),
                            fileOffset, partNumber, partSize ) );
        }
        threadExec.addWorker( new ThreadCompleteUploadPart( uploadIds.get( 0 ),
                partETags.subList( 0, firstUploadPartTimes ) ) );
        threadExec.run();

        // check complete upload part
        int len = firstUploadPartTimes * partSize;
        TestTools.LocalFile.readFile( filePath, 0, len, expFilePath );
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( expFilePath ) );

        // check not complete upload part
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        List< String > expCommonPrefixes = new ArrayList<>();
        MultiValueMap< String, String > expUploads = new LinkedMultiValueMap< String, String >();
        for ( int i = 1; i < uploadIds.size(); i++ ) {
            expUploads.add( key, uploadIds.get( i ) );
        }
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploads );

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

    private void uploadFirstPart( String uploadId, int fileOffset,
            int partNumber, int partSize ) {
        UploadPartRequest partRequest = new UploadPartRequest().withFile( file )
                .withFileOffset( fileOffset ).withPartNumber( partNumber )
                .withPartSize( partSize ).withBucketName( bucketName )
                .withKey( key ).withUploadId( uploadId );
        UploadPartResult partResult = s3Client.uploadPart( partRequest );
        partETags.add( partResult.getPartETag() );
    }

    private void initFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = this.createFile( fileSize );
        expFilePath = this.createFile( 0 );
        file = new File( filePath );
    }

    private String createFile( int fileSize ) throws IOException {
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        return filePath;
    }

    private class ThreadUploadOtherParts {
        private String uploadId;
        private int fileOffset;
        private int partNumber;
        private int partSize;

        public ThreadUploadOtherParts( String uploadId, int fileOffset,
                int partNumber, int partSize ) {
            this.uploadId = uploadId;
            this.fileOffset = fileOffset;
            this.partNumber = partNumber;
            this.partSize = partSize;
        }

        @ExecuteOrder(step = 1)
        private void uploadPart() throws Exception {
            AmazonS3 s3 = null;
            try {
                s3 = S3Utils.buildS3Client();
                UploadPartRequest partRequest = new UploadPartRequest()
                        .withFile( file ).withFileOffset( fileOffset )
                        .withPartNumber( partNumber ).withPartSize( partSize )
                        .withBucketName( bucketName ).withKey( key )
                        .withUploadId( uploadId );
                UploadPartResult partResult = s3.uploadPart( partRequest );
                partETags.add( partResult.getPartETag() );
            } finally {
                if ( s3 != null ) {
                    s3.shutdown();
                }
            }
        }
    }

    private class ThreadCompleteUploadPart {
        private String uploadId;
        private List< PartETag > eTags;

        public ThreadCompleteUploadPart( String uploadId,
                List< PartETag > eTags ) {
            this.uploadId = uploadId;
            this.eTags = eTags;
        }

        @ExecuteOrder(step = 1)
        private void uploadPart() throws Exception {
            AmazonS3 s3 = null;
            try {
                s3 = S3Utils.buildS3Client();
                PartUploadUtils.completeMultipartUpload( s3, bucketName, key,
                        uploadId, eTags );
            } finally {
                if ( s3 != null ) {
                    s3.shutdown();
                }
            }
        }
    }
}