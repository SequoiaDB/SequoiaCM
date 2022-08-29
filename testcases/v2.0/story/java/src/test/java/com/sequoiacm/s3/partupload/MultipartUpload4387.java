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
 * @description SCM-4387:带prefix、keyMarker和uploadIdMarker匹配查询桶分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4387 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private final String bucketName = "bucket4387";
    private File localPath;
    private File file;
    private final long fileSize = 2 * 1024 * 1024;
    private final String[] keys = { "atest4387_0", "/dir1/test4387_1",
            "/dir1/dir2/test4387_2", "/dira/test4387_3", "test4387_4" };
    private final List< String > uploadIdsOld = new ArrayList<>();
    private final List< String > uploadIdsNew = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test
    public void test() throws Exception {
        // initPartUpload
        for ( String key : keys ) {
            String uploadId = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, key );
            uploadIdsOld.add( uploadId );
        }
        // initPartUpload again
        for ( String key : keys ) {
            String uploadId = PartUploadUtils.initPartUpload( s3Client,
                    bucketName, key );
            uploadIdsNew.add( uploadId );
        }

        // uploadPart, multi part
        for ( int i = 0; i < 2; i++ ) {
            int maxPartNumber = 2;
            PartUploadUtils.partUpload( s3Client, bucketName, keys[ i ],
                    uploadIdsNew.get( i ), file, fileSize / maxPartNumber );
        }
        // uploadPart, only one part
        for ( int i = 2; i < keys.length; i++ ) {
            PartUploadUtils.partUpload( s3Client, bucketName, keys[ i ],
                    uploadIdsNew.get( i ), file, fileSize );
        }

        // list
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                bucketName ).withPrefix( "/dir" ).withKeyMarker( keys[ 2 ] )
                        .withUploadIdMarker( uploadIdsOld.get( 2 ) );
        MultipartUploadListing result = s3Client
                .listMultipartUploads( request );

        // check results
        List< String > expCommonPrefixes = new ArrayList<>();
        MultiValueMap< String, String > expUploads = new LinkedMultiValueMap<>();
        expUploads.add( keys[ 2 ], uploadIdsNew.get( 2 ) );
        expUploads.add( keys[ 1 ], uploadIdsOld.get( 1 ) );
        expUploads.add( keys[ 1 ], uploadIdsNew.get( 1 ) );
        expUploads.add( keys[ 3 ], uploadIdsOld.get( 3 ) );
        expUploads.add( keys[ 3 ], uploadIdsNew.get( 3 ) );
        PartUploadUtils.checkListMultipartUploadsResults( result,
                expCommonPrefixes, expUploads );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
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