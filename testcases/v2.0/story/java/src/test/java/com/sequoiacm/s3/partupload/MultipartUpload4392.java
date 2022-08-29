package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
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
 * @description SCM-4392:带prefix、keyMarker、uploadIdMarker和delimiter查询桶分段上传列表，不匹配其中一个条件
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4392 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4392";
    private final String[] keyNames = { "dir1/a4392", "dir1/dir2/test4392",
            "dir1a/test4392", "dir1b/4392", "dir1c_test4392", "test4392" };
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
    }

    @Test
    public void testListMultipartUploads() {
        List< String > uploadIds1 = new ArrayList<>();
        List< String > uploadIds2 = new ArrayList<>();
        String uploadId;
        for ( String keyName : keyNames ) {
            uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                    keyName );
            uploadIds1.add( uploadId );
            uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                    keyName );
            uploadIds2.add( uploadId );
        }

        // 不匹配delimiter
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName );
        request.setPrefix( "dir" );
        // keyMarKer:"dir1a/test18753"
        request.setKeyMarker( keyNames[ 2 ] );
        request.setUploadIdMarker( uploadIds1.get( 2 ) );
        request.setDelimiter( "%" );
        MultipartUploadListing partUploadList = s3Client
                .listMultipartUploads( request );
        List< String > expCommonPrefixes = new ArrayList<>();
        MultiValueMap< String, String > expUploads = new LinkedMultiValueMap<>();
        expUploads.add( keyNames[ 2 ], uploadIds2.get( 2 ) );
        expUploads.add( keyNames[ 3 ], uploadIds1.get( 3 ) );
        expUploads.add( keyNames[ 3 ], uploadIds2.get( 3 ) );
        expUploads.add( keyNames[ 4 ], uploadIds1.get( 4 ) );
        expUploads.add( keyNames[ 4 ], uploadIds2.get( 4 ) );
        PartUploadUtils.checkListMultipartUploadsResults( partUploadList,
                expCommonPrefixes, expUploads );

        // 不匹配prefix
        ListMultipartUploadsRequest request2 = new ListMultipartUploadsRequest(
                bucketName );
        request2.setPrefix( "prefix" );
        request2.setKeyMarker( keyNames[ 2 ] );
        request2.setUploadIdMarker( uploadIds1.get( 2 ) );
        request2.setDelimiter( "/" );
        partUploadList = s3Client.listMultipartUploads( request2 );
        expUploads = new LinkedMultiValueMap<>();
        PartUploadUtils.checkListMultipartUploadsResults( partUploadList,
                expCommonPrefixes, expUploads );

        // 不匹配uploadIdMarker
        ListMultipartUploadsRequest request3 = new ListMultipartUploadsRequest(
                bucketName );
        request3.setPrefix( "prefix" );
        request3.setKeyMarker( keyNames[ 2 ] );
        request3.setUploadIdMarker( "123456" );
        request3.setDelimiter( "/" );
        partUploadList = s3Client.listMultipartUploads( request3 );
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
