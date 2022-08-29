package com.sequoiacm.s3.partupload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-4452:指定文件范围，复制分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4452 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String targetBucketName = "targetbucket4452";
    private String sourceBucketName = "sourcebucket4452";
    private String targetKey = "/aa/bb/targetobj4452";
    private String sourceKey = "/aa/bb/sourceobj4452";
    private long fileSize = 1024 * 1024 * 20;
    private File localPath = null;
    private String filePath = null;
    private String uploadId = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile1_" + fileSize
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
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 指定文件范围大于的文件大小
        uploadPartCopy( sourceBucketName, sourceKey, targetBucketName,
                targetKey, fileSize * 2 );
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
            String targetBucketName, String targetKey, long LastByte )
            throws Exception {
        uploadId = PartUploadUtils.initPartUpload( s3Client, targetBucketName,
                targetKey );
        List< PartETag > partEtags = partUploadCopy( sourceBucketName,
                sourceKey, targetBucketName, targetKey, uploadId, LastByte );
        PartUploadUtils.completeMultipartUpload( s3Client, targetBucketName,
                targetKey, uploadId, partEtags );

        // down file check the file content
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                targetBucketName, targetKey );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    public List< PartETag > partUploadCopy( String sourceBucketName,
            String sourceKey, String targetBucketName, String targetKey,
            String uploadId, long sourceObjectSize ) {
        List< PartETag > partEtags = new ArrayList<>();
        long filepositon = 0;

        CopyPartRequest request = new CopyPartRequest().withUploadId( uploadId )
                .withPartNumber( 1 ).withSourceBucketName( sourceBucketName )
                .withSourceKey( sourceKey )
                .withDestinationBucketName( targetBucketName )
                .withDestinationKey( targetKey ).withFirstByte( filepositon )
                .withLastByte( sourceObjectSize );
        CopyPartResult copyResult = s3Client.copyPart( request );
        partEtags.add( copyResult.getPartETag() );
        return partEtags;
    }
}