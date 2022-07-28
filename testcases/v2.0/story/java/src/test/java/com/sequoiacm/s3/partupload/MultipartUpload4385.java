package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
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
 * @description SCM-4385:带MaxUploads查询桶分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4385 extends TestScmBase {
    private int runSuccessNum = 0;
    private AmazonS3 s3Client;
    private final String bucketName = "bucket4385";
    private File localPath;
    private File file;
    private final long fileSize = 10 * 1024 * 1024;
    private final String[] keys = { "/aa/bb/test4385_1", "/aa/bb/test4385_2",
            "test4385_3", "test4385_4" };
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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_ltTotalRecs() {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withMaxUploads( 2 );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        expUploads.clear();
        expUploads.add( keys[ 0 ], uploadIds.get( 0 ) );
        expUploads.add( keys[ 1 ], uploadIds.get( 1 ) );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploads );
        runSuccessNum++;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_gtTotalRecs() {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withMaxUploads( 5 );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        expUploads.clear();
        expUploads.add( keys[ 0 ], uploadIds.get( 0 ) );
        expUploads.add( keys[ 1 ], uploadIds.get( 1 ) );
        expUploads.add( keys[ 2 ], uploadIds.get( 2 ) );
        expUploads.add( keys[ 3 ], uploadIds.get( 3 ) );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploads );
        runSuccessNum++;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_etTotalRecs() {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withMaxUploads( 4 );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );
        expUploads.clear();
        expUploads.add( keys[ 0 ], uploadIds.get( 0 ) );
        expUploads.add( keys[ 1 ], uploadIds.get( 1 ) );
        expUploads.add( keys[ 2 ], uploadIds.get( 2 ) );
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