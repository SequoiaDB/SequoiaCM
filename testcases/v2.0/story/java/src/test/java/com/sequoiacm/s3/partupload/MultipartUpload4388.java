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
 * @description SCM-4388:带prefix和delimiter匹配查询桶分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4388 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4388";
    private String baseKeyName = "object4388.png";
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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void uploadParts() {
        List< String > expCommonPrefixes = initPartUpload();
        // list multipartUploads and check list info.
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withDelimiter( delimiter ).withPrefix( prefix );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > expUpload = new LinkedMultiValueMap<>();
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

    private List< String > initPartUpload() {
        List< String > expCommonPrefixes = new ArrayList<>();
        for ( int i = 0; i < objectNum; i++ ) {
            if ( i % 10 == 0 ) {
                // keyName misMatch prefix and delimiter
                String subKeyName = "test" + i + "_" + baseKeyName;
                PartUploadUtils.initPartUpload( s3Client, bucketName,
                        subKeyName );
                PartUploadUtils.initPartUpload( s3Client, bucketName,
                        subKeyName );
            } else {
                // keyName match prefix and delimter
                String subKeyName = prefix + i + delimiter + "_" + baseKeyName;
                PartUploadUtils.initPartUpload( s3Client, bucketName,
                        subKeyName );
                PartUploadUtils.initPartUpload( s3Client, bucketName,
                        subKeyName );
                expCommonPrefixes.add( prefix + i + delimiter );
            }
        }
        return expCommonPrefixes;
    }
}
