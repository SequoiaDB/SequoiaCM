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
 * @description SCM-4389:带prefix、delimiter查询桶分段上传列表，不匹配delimiter或prefix
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4389 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4389";
    private String baseKeyName = "object4389.png";
    private int objectNum = 20;
    private String prefix = "dir";
    private String delimiter = "/";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test
    public void uploadParts() {
        MultiValueMap< String, String > expUpload = initPartUpload();
        // list multipartUploads and check list info.
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withDelimiter( delimiter ).withPrefix( prefix );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
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
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private MultiValueMap< String, String > initPartUpload() {
        MultiValueMap< String, String > expUpload = new LinkedMultiValueMap<>();
        for ( int i = 0; i < objectNum; i++ ) {
            if ( i % 10 == 0 ) {
                // keyName misMatch prefix and matching delimiter
                String subKeyName = "test" + i + delimiter + baseKeyName;
                PartUploadUtils.initPartUpload( s3Client, bucketName,
                        subKeyName );
                PartUploadUtils.initPartUpload( s3Client, bucketName,
                        subKeyName );
            } else {
                // keyName matching prefix and mismatch delimter
                String subKeyName = prefix + i + "_" + baseKeyName;
                String uploadId1 = PartUploadUtils.initPartUpload( s3Client,
                        bucketName, subKeyName );
                String uploadId2 = PartUploadUtils.initPartUpload( s3Client,
                        bucketName, subKeyName );
                expUpload.add( subKeyName, uploadId1 );
                expUpload.add( subKeyName, uploadId2 );
            }
        }
        return expUpload;
    }

}
