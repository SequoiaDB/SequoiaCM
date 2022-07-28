package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
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
 * @description SCM-4398:多次查询结果在commprefix中有相同记录
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4398 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4398";
    private String keyNameA = "/aa!_test1_4398";
    private String keyNameB = "/aa/bb/test2_4398";
    private String keyNameC = "/aa/bb/test3_4398";
    private String keyNameD = "/aa?aa?test4_4398";
    private String keyNameE = "/aa/_test5_4398";
    private String keyNameF = "/aa_test6_4398";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void listMultipartUploads() {
        String uploadIdA = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameA );
        PartUploadUtils.initPartUpload( s3Client, bucketName, keyNameB );
        PartUploadUtils.initPartUpload( s3Client, bucketName, keyNameC );
        String uploadIdD = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameD );
        int maxUploads = 2;
        String prefix = "/aa";
        String delimiter = "/";
        // first query
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withMaxUploads( maxUploads )
                        .withDelimiter( delimiter ).withPrefix( prefix );
        MultipartUploadListing result1 = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > expUpload1 = new LinkedMultiValueMap<>();
        expUpload1.add( keyNameA, uploadIdA );
        List< String > expCommonPrefixes1 = new ArrayList<>();
        expCommonPrefixes1.add( "/aa/" );
        PartUploadUtils.checkListMultipartUploadsResults( result1,
                expCommonPrefixes1, expUpload1 );

        // second list, match the new putObject(
        // keyNameE:"/aa/_test5_4398",keyNameF = "/aa_test6_4398")
        PartUploadUtils.initPartUpload( s3Client, bucketName, keyNameE );
        String uploadIdF = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyNameF );
        String nextKeyMarker = result1.getNextKeyMarker();
        String nextUploadId = result1.getNextUploadIdMarker();

        // second query
        request = new ListMultipartUploadsRequest( bucketName )
                .withKeyMarker( nextKeyMarker )
                .withUploadIdMarker( nextUploadId ).withDelimiter( delimiter )
                .withPrefix( prefix );
        MultipartUploadListing result2 = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > expUpload2 = new LinkedMultiValueMap<>();
        expUpload2.add( keyNameD, uploadIdD );
        expUpload2.add( keyNameF, uploadIdF );
        List< String > expCommonPrefixes2 = new ArrayList<>();
        PartUploadUtils.checkListMultipartUploadsResults( result2,
                expCommonPrefixes2, expUpload2 );
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
