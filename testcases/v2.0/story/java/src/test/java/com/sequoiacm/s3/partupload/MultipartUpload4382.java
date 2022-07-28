package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.sequoiacm.testcommon.TestScmBase;
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
 * @description SCM-4382:带前缀prefix查询桶分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4382 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4382";
    private String[] keyNames = { "atest0_4382.png", "dir1/a/test1_4382.png",
            "dir1/dir2/a/test2_4382.png", "dirtest1/test3_4382.png",
            "dir4/test4_4382.png" };
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void uploadParts() {
        MultiValueMap< String, String > uploadIds = new LinkedMultiValueMap<>();
        for ( String keyName : keyNames ) {
            String uploadId = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, keyName );
            uploadIds.add( keyName, uploadId );
        }
        // test a: matching prefix
        String prefixA = "dir";
        listPartUploadsMatchedPrefix( uploadIds, prefixA );
        // test b: mis matched prefix
        String prefixB = "/dir";
        listPartUploadsMisMatchedPrefix( prefixB );
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

    private void listPartUploadsMatchedPrefix(
            MultiValueMap< String, String > uploadIds, String prefix ) {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withPrefix( prefix );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        // remove the object not match prefix
        uploadIds.remove( keyNames[ 0 ] );

        List< String > expCommonPrefixes = new ArrayList<>();
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, uploadIds );
    }

    private void listPartUploadsMisMatchedPrefix( String prefix ) {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withPrefix( prefix );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );

        List< String > expCommonPrefixes = new ArrayList<>();
        MultiValueMap< String, String > expUploadIds = new LinkedMultiValueMap<>();
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploadIds );
    }

}
