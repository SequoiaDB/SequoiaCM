package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4381:带分隔符delimiter查询分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4381 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4381";
    private String[] keyNames = { "atest0_4381.png", "dir1/a/test1_4381.png",
            "dir1/dir2/a/test2_4381.png", "/a/test3_4381.png",
            "dir4/test4_4381.png" };
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void uploadParts() {
        MultiValueMap< String, String > uploadIds = new LinkedMultiValueMap<>();
        for ( String keyName : keyNames ) {
            String uploadId = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, keyName );
            uploadIds.add( keyName, uploadId );
        }

        // test a: matching delimiter
        String delimiterA = "/";
        listPartUploadsMatchedDelimiter( uploadIds, delimiterA );
        // test b: mis matched delimiter
        String delimiterB = "/testb";
        listPartUploadsMisMatchedDelimiter( uploadIds, delimiterB );
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

    private void listPartUploadsMatchedDelimiter(
            MultiValueMap< String, String > uploadIds, String delimiter ) {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withDelimiter( delimiter );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > expUpload = new LinkedMultiValueMap<>();
        expUpload.add( keyNames[ 0 ], uploadIds.getFirst( keyNames[ 0 ] ) );
        List< String > expCommonPrefixes = new ArrayList<>();
        expCommonPrefixes.add( "dir1/" );
        expCommonPrefixes.add( "/" );
        expCommonPrefixes.add( "dir4/" );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUpload );
    }

    private void listPartUploadsMisMatchedDelimiter(
            MultiValueMap< String, String > uploadIds, String delimiter ) {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withDelimiter( delimiter );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );

        List< String > expCommonPrefixes = new ArrayList<>();
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, uploadIds );
    }

}
