package com.sequoiacm.s3.partupload;

import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4450:指定源文件匹配的MD5值，复制分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4450 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String targetBucketName = "targetbucket4450";
    private String sourceBucketName = "sourcebucket4450";
    private String targetKey = "/aa/bb/targetobj4450";
    private String sourceKey = "/aa/bb/sourceobj4450";
    private long fileSize = 1024 * 1024 * 5;
    private long sourceObjectSize;
    private File localPath = null;
    private String filePath = null;
    private String uploadId = null;

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

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 存在等于指定md5的文件
        uploadPartCopy( sourceBucketName, sourceKey, targetBucketName,
                targetKey, TestTools.getMD5( filePath ) );

        S3Utils.deleteAllObjects( s3Client, sourceBucketName );
        uploadId = PartUploadUtils.initPartUpload( s3Client, targetBucketName,
                targetKey );

        // 不存在等于指定md5的文件
        try {
            partUploadCopy( sourceBucketName, sourceKey, targetBucketName,
                    targetKey, uploadId, sourceObjectSize,
                    TestTools.getMD5( filePath ) );
            Assert.fail( "upload copy part must be fail !" );
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

    public void uploadPartCopy( String sourceBucketName, String sourceKey,
            String targetBucketName, String targetKey,
            String matchingETagConstraint ) throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, targetBucketName,
                targetKey );
        List< PartETag > partEtags = partUploadCopy( sourceBucketName,
                sourceKey, targetBucketName, targetKey, uploadId,
                sourceObjectSize, matchingETagConstraint );
        PartUploadUtils.completeMultipartUpload( s3Client, targetBucketName,
                targetKey, uploadId, partEtags );

        // down file check the file content
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                targetBucketName, targetKey );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    public List< PartETag > partUploadCopy( String sourceBucketName,
            String sourceKey, String targetBucketName, String targetKey,
            String uploadId, long sourceObjectSize,
            String matchingETagConstraint ) {
        List< PartETag > partEtags = new ArrayList<>();

        long filepositon = 0L;
        CopyPartRequest request = new CopyPartRequest().withUploadId( uploadId )
                .withPartNumber( 1 ).withSourceBucketName( sourceBucketName )
                .withSourceKey( sourceKey )
                .withDestinationBucketName( targetBucketName )
                .withDestinationKey( targetKey ).withFirstByte( filepositon )
                .withLastByte( sourceObjectSize )
                .withMatchingETagConstraint( matchingETagConstraint );
        CopyPartResult copyResult = s3Client.copyPart( request );
        partEtags.add( copyResult.getPartETag() );

        return partEtags;
    }
}