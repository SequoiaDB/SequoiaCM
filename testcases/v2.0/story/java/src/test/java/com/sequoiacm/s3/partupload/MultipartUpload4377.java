package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4377:对象处于多个不同阶段，查询桶中对象分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4377 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4377";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private File file = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        int fileSize = 1024 * 1024 * 20;
        String filePath = localPath + File.separator + "localFile_" + fileSize
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
    public void listMultipartUploads() throws Exception {
        // test a: PartUpload
        String keyNameA = "/aa/object4377A";
        String uploadIdA = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameA );
        PartUploadUtils.partUpload( s3Client, bucketName, keyNameA, uploadIdA,
                file );

        // test b: initPartUpload
        String keyNameB = "/aa/object4377B";
        String uploadIdB = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameB );

        // test c: upload partial uploads
        String keyNameC = "/aa/object4377C";
        String uploadIdC = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameC );
        UploadPartRequest partRequest = new UploadPartRequest().withFile( file )
                .withFileOffset( 0 ).withPartNumber( 1 )
                .withPartSize( 1024 * 1024 * 5 ).withBucketName( bucketName )
                .withKey( keyNameC ).withUploadId( uploadIdC );
        s3Client.uploadPart( partRequest );

        // test d: completeMultipartUpload
        String keyNameD = "/aa/object4377D";
        String uploadIdD = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameD );
        List< PartETag > partEtagsD = PartUploadUtils.partUpload( s3Client,
                bucketName, keyNameD, uploadIdD, file );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyNameD,
                uploadIdD, partEtagsD );

        // test e: abortMultipartUpload
        String keyNameE = "/aa/object4377E";
        String uploadIdE = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameE );
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                bucketName, keyNameE, uploadIdE );
        s3Client.abortMultipartUpload( abortRequest );

        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > expUpload = new LinkedMultiValueMap<>();
        expUpload.add( keyNameA, uploadIdA );
        expUpload.add( keyNameB, uploadIdB );
        expUpload.add( keyNameC, uploadIdC );
        List< String > expCommonPrefixes = new ArrayList<>();
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUpload );
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
}
