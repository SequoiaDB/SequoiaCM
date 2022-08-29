package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;

/**
 * @description SCM-4356:初始化分段上传后，终止分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4356 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4356";
    private AmazonS3 s3Client;
    private File localPath;
    private File file;
    private final long fileSize = 5 * 1024 * 1024;

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { GroupTags.base })
    public void test() {
        String key = "/aa/bb/obj4356";
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                key );
        int maxPartNumber = 5;
        PartUploadUtils.partUpload( s3Client, bucketName, key, uploadId, file,
                fileSize / maxPartNumber );
        s3Client.abortMultipartUpload(
                new AbortMultipartUploadRequest( bucketName, key, uploadId ) );
        PartUploadUtils.checkAbortMultipartUploadResult( s3Client, bucketName,
                key, uploadId );

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

    private void initFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );
    }
}