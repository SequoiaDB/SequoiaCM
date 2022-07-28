package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
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
import java.util.Map;

/**
 * @description SCM-4394:带delimiter和maxkeys查询桶分段上传列表，只匹配其中一个条件
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4394 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4394";
    private final String[] keyNames = { "dir1/a4394", "dir1/dir2/test4394",
            "dir1a/test4394", "dir1b/4394", "dir1c_test4394", "test4394" };
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testListMultipartUploads() {
        MultiValueMap< String, String > expUploads = new LinkedMultiValueMap<>();
        List< String > uploadIds = new ArrayList<>();
        for ( String keyName : keyNames ) {
            String uploadId = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, keyName );
            uploadIds.add( uploadId );
            expUploads.add( keyName, uploadId );
        }

        // 不匹配delimiter条件
        List< String > expCommonPrefixes = new ArrayList<>();
        listAndCheckResult( "%", 3, 3, expCommonPrefixes, expUploads );

        // 不匹配maxUploads条件
        expCommonPrefixes.add( "dir1/" );
        expCommonPrefixes.add( "dir1a/" );
        expCommonPrefixes.add( "dir1b/" );
        MultiValueMap< String, String > temPxpUploads = new LinkedMultiValueMap<>();
        temPxpUploads.put( keyNames[ 4 ], expUploads.get( keyNames[ 4 ] ) );
        temPxpUploads.put( keyNames[ 5 ], expUploads.get( keyNames[ 5 ] ) );
        listAndCheckResult( "/", 10,
                expCommonPrefixes.size() + temPxpUploads.size(),
                expCommonPrefixes, temPxpUploads );
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

    private void listAndCheckResult( String delimiter, int maxUploads,
            int expReturnedUploadNum, List< String > expCommonPrefixes,
            MultiValueMap< String, String > expUploads ) {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName );
        request.setDelimiter( delimiter );
        request.setMaxUploads( maxUploads );
        MultipartUploadListing partUploadList;
        List< String > actCommonPrefixes = new ArrayList<>();
        MultiValueMap< String, String > actUploads = new LinkedMultiValueMap<>();
        do {
            int returnedUploadNum = 0;
            partUploadList = s3Client.listMultipartUploads( request );
            List< String > commonPrefixes = partUploadList.getCommonPrefixes();
            returnedUploadNum += commonPrefixes.size();
            actCommonPrefixes.addAll( commonPrefixes );
            List< MultipartUpload > multipartUploads = partUploadList
                    .getMultipartUploads();
            for ( MultipartUpload multipartUpload : multipartUploads ) {
                String temKeyName = multipartUpload.getKey();
                String temUploadId = multipartUpload.getUploadId();
                actUploads.add( temKeyName, temUploadId );
            }
            returnedUploadNum += multipartUploads.size();

            String nextKeyMarKer = partUploadList.getNextKeyMarker();
            request.setKeyMarker( nextKeyMarKer );
            String nextUploadIdMarker = partUploadList.getNextUploadIdMarker();
            request.setUploadIdMarker( nextUploadIdMarker );
            Assert.assertEquals( returnedUploadNum, expReturnedUploadNum,
                    "commonprefixes : " + actCommonPrefixes.toString()
                            + " uploads:" + actUploads.toString() );
        } while ( partUploadList.isTruncated() );
        checkResult( expCommonPrefixes, actCommonPrefixes, expUploads,
                actUploads );
    }

    private void checkResult( List< String > expCommonPrefixes,
            List< String > actCommonPrefixes,
            MultiValueMap< String, String > expUploads,
            MultiValueMap< String, String > actUploads ) {
        Assert.assertEquals( actCommonPrefixes, expCommonPrefixes,
                "actCommonPrefixes = " + actCommonPrefixes.toString()
                        + ",expCommonPrefixes = "
                        + expCommonPrefixes.toString() );
        Assert.assertEquals( actUploads.size(), expUploads.size(),
                "actMap = " + actUploads.toString() + ",expUpload = "
                        + expUploads.toString() );
        for ( Map.Entry< String, List< String > > entry : expUploads
                .entrySet() ) {
            Assert.assertEquals( actUploads.get( entry.getKey() ),
                    expUploads.get( entry.getKey() ),
                    "actMap = " + actUploads.toString() + ",expMap = "
                            + expUploads.toString() );
        }
    }
}
