package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.util.List;

/**
 * @description SCM-4349:开启分段检测，开启版本控制，相同key不同uploadId多次分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4349 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4349";
    private String keyName = "key4349";
    private AmazonS3 s3Client = null;
    private long fileSize = 100 * 1024 * 1024;
    private File localPath = null;
    private File oldFile = null;
    private File newFile = null;
    private String oldfilePath = null;
    private String newfilePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        oldfilePath = localPath + File.separator + "localFile_" + fileSize
                + "old.txt";
        newfilePath = localPath + File.separator + "localFile_" + fileSize
                + "new.txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( oldfilePath, fileSize );
        TestTools.LocalFile.createFile( newfilePath, fileSize );
        oldFile = new File( oldfilePath );
        newFile = new File( newfilePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        Bucket bucket = s3Client
                .createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testUpload() throws Exception {
        String uploadId1 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        // 分10个分段上传
        List< PartETag > partEtags = PartUploadUtils.partUpload( s3Client,
                bucketName, keyName, uploadId1, oldFile, 5 * 1024 * 1024 );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId1, partEtags );

        // 再次指定相同key初始化分段上传对象
        String uploadId2 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        // 分20个分段上传
        partEtags = PartUploadUtils.partUpload( s3Client, bucketName, keyName,
                uploadId2, newFile );

        // 查询分段列表
        checkPartList( 20, uploadId2 );

        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId2, partEtags );
        checkUploadResult();
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

    private void checkPartList( int partNum, String uploadId ) {
        ListPartsRequest request = new ListPartsRequest( bucketName, keyName,
                uploadId );
        PartListing result = s3Client.listParts( request );
        List< PartSummary > parts = result.getParts();
        Assert.assertEquals( parts.size(), partNum );
        for ( int i = 0; i < parts.size(); i++ ) {
            int partNumber = parts.get( i ).getPartNumber();
            Assert.assertEquals( partNumber, i + 1 );
            long partSize = parts.get( i ).getSize();
            Assert.assertEquals( partSize, PartUploadUtils.partLimitMinSize,
                    "partnumber = " + partNumber );
        }
    }

    private void checkUploadResult() throws Exception {
        String actMd5 = S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName, "2.0" );
        String expMd5 = TestTools.getMD5( newfilePath );
        Assert.assertEquals( actMd5, expMd5, "version id = 2.0" );

        actMd5 = S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName, "1.0" );
        expMd5 = TestTools.getMD5( oldfilePath );
        Assert.assertEquals( actMd5, expMd5, "version id = 1.0" );

        // 验证bucketName、key、fileSize、Version
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                "2.0" );
        S3Object object = s3Client.getObject( request );
        Assert.assertEquals( object.getBucketName(), bucketName,
                "version id = 2.0" );
        Assert.assertEquals( object.getKey(), keyName, "version id = 2.0" );
        Assert.assertEquals( object.getObjectMetadata().getContentLength(),
                fileSize, "version id = 2.0" );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                "2.0", "version id = 2.0" );

        request = new GetObjectRequest( bucketName, keyName, "1.0" );
        object = s3Client.getObject( request );
        Assert.assertEquals( object.getBucketName(), bucketName,
                "version id = 1.0" );
        Assert.assertEquals( object.getKey(), keyName, "version id = 1.0" );
        Assert.assertEquals( object.getObjectMetadata().getContentLength(),
                fileSize, "version id = 1.0" );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                "1.0", "version id = 1.0" );
    }
}
