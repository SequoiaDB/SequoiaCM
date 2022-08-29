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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @description SCM-4396:带prefix、keyMarker、uploadIdMarker和delimiter匹配查询桶分段上传列表，再次查询匹配条件不同
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4396 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4396";
    private String baseKeyName = "object4396.png";
    private int objectNum = 1050;
    private int defaultEachListMaxNum = 1000;
    private String delimiter1 = "/";
    private String delimiter2 = "test";
    private AmazonS3 s3Client = null;
    private List< String > commonPrefixes1 = new ArrayList<>();
    private List< String > commonPrefixes2 = new ArrayList<>();
    private MultiValueMap< String, String > uploads = new LinkedMultiValueMap<>();
    private MultiValueMap< String, String > uploads2 = new LinkedMultiValueMap<>();

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        initPartUpload();
    }

    @Test(groups = { GroupTags.base })
    public void uploadParts() {
        // first query,specify keyMarker and uploadIdMarker
        int keySerial = 5;
        Object[] keyMarkers = uploads.keySet().toArray();
        Arrays.sort( keyMarkers );
        String keyMarker = keyMarkers[ keySerial ].toString();
        String uploadIdMarker = uploads.get( keyMarker ).get( 0 );
        String prefix1 = "dir";

        // list multipartUploads and check list info.return the num is 1000
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withDelimiter( delimiter1 ).withPrefix( prefix1 )
                        .withKeyMarker( keyMarker )
                        .withUploadIdMarker( uploadIdMarker );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        MultiValueMap< String, String > uploads1 = new LinkedMultiValueMap<>();
        List< String > expCommonPrefixes1 = commonPrefixes1.subList(
                keySerial + 1, defaultEachListMaxNum + keySerial + 1 );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes1, uploads1 );

        // second query,reSet delimiter and prefix.
        String continuationKeyMarker = result.getNextKeyMarker();
        String continuationUploadIdMarker = result.getNextUploadIdMarker();
        String prefix2 = "dir_";
        request = new ListMultipartUploadsRequest( bucketName )
                .withDelimiter( delimiter2 ).withPrefix( prefix2 )
                .withKeyMarker( continuationKeyMarker )
                .withUploadIdMarker( continuationUploadIdMarker );
        MultipartUploadListing result2 = s3Client
                .listMultipartUploads( request );
        List< String > expCommonPrefixes2 = commonPrefixes2.subList(
                defaultEachListMaxNum + keySerial + 1, commonPrefixes2.size() );
        PartUploadUtils.checkListMultipartUploadsResults( result2,
                expCommonPrefixes2, uploads2 );
        Assert.assertFalse( result2.isTruncated(),
                "the list should be query	finsh!" );
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

    private void initPartUpload() {
        for ( int i = 0; i < objectNum - 1; i++ ) {
            String subKeyName = "dir_" + i + delimiter2 + delimiter1
                    + baseKeyName;
            String uploadId1 = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, subKeyName );
            String uploadId2 = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, subKeyName );
            commonPrefixes1.add( "dir_" + i + delimiter2 + delimiter1 );
            commonPrefixes2.add( "dir_" + i + delimiter2 );
            uploads.add( subKeyName, uploadId1 );
            uploads.add( subKeyName, uploadId2 );
        }
        // add the object is misMatch delimiter1 and delimieter2
        String subKeyName = "dir_998_" + baseKeyName;
        String uploadId1 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                subKeyName );
        String uploadId2 = PartUploadUtils.initPartUpload( s3Client, bucketName,
                subKeyName );
        uploads2.add( subKeyName, uploadId1 );
        uploads2.add( subKeyName, uploadId2 );
        Collections.sort( commonPrefixes1 );
        Collections.sort( commonPrefixes2 );
    }
}
