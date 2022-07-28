package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
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
 * @description SCM-4391:带prefix、keyMarker、uploadIdMarker和delimiter匹配查询桶分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4391 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4391";
    private final String[] keyNames = { "dir1/a4391", "dir1/dir2/test4391",
            "dir1a/test4391", "dir1b/4391", "test4391" };
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testListMultipartUploads() {
        List< String > uploadIds1 = new ArrayList<>();
        List< String > uploadIds2 = new ArrayList<>();
        List< String > uploadIds3 = new ArrayList<>();
        String uploadId;
        for ( String keyName : keyNames ) {
            uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                    keyName );
            uploadIds1.add( uploadId );
            uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                    keyName );
            uploadIds2.add( uploadId );
            uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                    keyName );
            uploadIds3.add( uploadId );
        }
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName );
        request.setPrefix( "dir" );
        // keyMarKer:"dir1/dir2/test18752"
        request.setKeyMarker( keyNames[ 1 ] );
        request.setUploadIdMarker( uploadIds2.get( 1 ) );
        request.setDelimiter( "/" );
        MultipartUploadListing partUploadList = s3Client
                .listMultipartUploads( request );
        List< String > expCommonPrefixes = new ArrayList<>();
        expCommonPrefixes.add( "dir1a/" );
        expCommonPrefixes.add( "dir1b/" );
        MultiValueMap< String, String > expUploads = new LinkedMultiValueMap<>();
        PartUploadUtils.checkListMultipartUploadsResults( partUploadList,
                expCommonPrefixes, expUploads );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
