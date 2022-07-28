package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4395:带prefix、keyMarker、uploadIdMarker和delimiter匹配查询桶分段上传列表（多次查询）
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4395A extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4395";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void listMultipartUploads() {
        String keyNameA = "a/test0_4395";
        String uploadIdA1 = PartUploadUtils.initPartUpload( s3Client,
                bucketName, keyNameA );
        String uploadIdA2 = PartUploadUtils.initPartUpload( s3Client,
                bucketName, keyNameA );
        String keyNameB = "a/test1_4395";
        String uploadIdB1 = PartUploadUtils.initPartUpload( s3Client,
                bucketName, keyNameB );
        String uploadIdB2 = PartUploadUtils.initPartUpload( s3Client,
                bucketName, keyNameB );
        String keyNameC = "dir1/atest2_4395.png";
        String uploadIdC1 = PartUploadUtils.initPartUpload( s3Client,
                bucketName, keyNameC );
        String uploadIdC2 = PartUploadUtils.initPartUpload( s3Client,
                bucketName, keyNameC );
        int maxUploads = 3;
        // first query
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withMaxUploads( maxUploads );
        MultipartUploadListing result1 = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > expUpload1 = new LinkedMultiValueMap<>();
        expUpload1.add( keyNameA, uploadIdA1 );
        expUpload1.add( keyNameA, uploadIdA2 );
        expUpload1.add( keyNameB, uploadIdB1 );
        List< String > expCommonPrefixes = new ArrayList<>();
        PartUploadUtils.checkListMultipartUploadsResults( result1,
                expCommonPrefixes, expUpload1 );

        // abortMultipartUpload by the nextKeyMarker(eg:keyNameB:uploadIdB1)
        String nextKeyMarker = result1.getNextKeyMarker();
        String nextUploadId = result1.getNextUploadIdMarker();
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                bucketName, nextKeyMarker, nextUploadId );
        s3Client.abortMultipartUpload( abortRequest );

        // second query
        request = new ListMultipartUploadsRequest( bucketName )
                .withKeyMarker( nextKeyMarker )
                .withUploadIdMarker( nextUploadId );
        MultipartUploadListing result2 = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > expUpload2 = new LinkedMultiValueMap<>();
        expUpload2.add( keyNameB, uploadIdB2 );
        expUpload2.add( keyNameC, uploadIdC1 );
        expUpload2.add( keyNameC, uploadIdC2 );
        PartUploadUtils.checkListMultipartUploadsResults( result2,
                expCommonPrefixes, expUpload2 );
        Assert.assertFalse( result2.isTruncated(),
                "the list query should be finsh!" );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }
}
