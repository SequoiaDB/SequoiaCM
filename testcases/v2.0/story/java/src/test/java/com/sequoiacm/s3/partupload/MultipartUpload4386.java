package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4386:带prefix、keyMarker和uploadIdMarker查询桶分段上传列表，不匹配其中一个条件
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4386 extends TestScmBase {
    private int runSuccessNum = 0;
    private AmazonS3 s3Client;
    private final String bucketName = "bucket4386";
    private File localPath;
    private File file;
    private final long fileSize = 2 * 1024 * 1024;
    private final String[] keys = { "/aa/bb/test4386_1", "/aa/bb/test4386_2",
            "test4386_3", "test4386_4" };
    private final List< String > uploadIds = new ArrayList<>();
    private final MultiValueMap< String, String > expUploads = new LinkedMultiValueMap<>();
    private final List< String > expCommonPrefixes = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );

        // uploadPart
        for ( String key : keys ) {
            String uploadId = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, key );
            int maxPartNumber = 2;
            PartUploadUtils.partUpload( s3Client, bucketName, key, uploadId,
                    file, fileSize / maxPartNumber );
            uploadIds.add( uploadId );
        }
    }

    @Test
    public void test_NotSatisfyPrefix() {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withPrefix( "notExist" ).withKeyMarker( keys[ 0 ] )
                        .withUploadIdMarker( uploadIds.get( 0 ) );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploads );
        runSuccessNum++;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_NotSatisfyKeyMarker() {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withPrefix( "/aa" ).withKeyMarker( "notExist" )
                        .withUploadIdMarker( uploadIds.get( 0 ) );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploads );
        runSuccessNum++;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_NotSatisfyUploadIdMarker() {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withPrefix( "test" ).withKeyMarker( keys[ 2 ] )
                        .withUploadIdMarker( "9999999999999" );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        expUploads.clear();
        expUploads.add( keys[ 3 ], uploadIds.get( 3 ) );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploads );
        runSuccessNum++;
    }

    @AfterClass
    private void tearDown() {
        try {
            int expRunSuccessNum = 3;
            if ( runSuccessNum == expRunSuccessNum ) {
                s3Client.deleteBucket( bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void initFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );
    }
}