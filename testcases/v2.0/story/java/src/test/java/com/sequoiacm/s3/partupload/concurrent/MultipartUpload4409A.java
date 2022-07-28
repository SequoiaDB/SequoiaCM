package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
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
 * @description SCM-4409:并发查询分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4409A extends TestScmBase {
    private boolean runSuccess = false;
    private int ThreadNum = 20;
    private AmazonS3 s3Client;
    private String bucketName = "bucket4409a";
    private File localPath;
    private String filePath;
    private File file;
    private long fileSize = 5 * 1024 * 1024;
    private int maxPartNumber = 10;
    private String[] keys = { "atest4409a_0", "/dir1/test4409a_1",
            "/dir1/dir2/test4409a_2", "/dira/test4409a_3", "test4409a_4" };
    private List< String > uploadIdsOld = new ArrayList<>();
    private List< String > uploadIdsNew = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        this.initAndUploadPart();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        // list and check results
        ThreadExecutor threadExec = new ThreadExecutor();
        for ( int i = 0; i < ThreadNum; i++ ) {
            threadExec.addWorker( new ThreadList() );
        }
        threadExec.run();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void initAndUploadPart() {
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
        for ( int i = 0; i < keys.length; i++ ) {
            PartUploadUtils.partUpload( s3Client, bucketName, keys[ i ],
                    uploadIdsNew.get( i ), file, fileSize / maxPartNumber );
        }
    }

    private void initFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );
    }

    private class ThreadList {
        private AmazonS3 s3;
        private MultipartUploadListing result;

        @ExecuteOrder(step = 1, desc = "connect s3")
        private void connectS3() throws Exception {
            s3 = S3Utils.buildS3Client();
        }

        @ExecuteOrder(step = 2, desc = "list")
        private void list() {
            ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                    bucketName ).withPrefix( "/dir" ).withKeyMarker( keys[ 2 ] )
                            .withUploadIdMarker( uploadIdsOld.get( 2 ) );
            result = s3.listMultipartUploads( request );
        }

        @ExecuteOrder(step = 3, desc = "check results")
        private void checkResults() {
            List< String > expCommonPrefixes = new ArrayList<>();
            MultiValueMap< String, String > expUploads = new LinkedMultiValueMap< String, String >();
            expUploads.add( keys[ 2 ], uploadIdsNew.get( 2 ) );
            expUploads.add( keys[ 1 ], uploadIdsOld.get( 1 ) );
            expUploads.add( keys[ 1 ], uploadIdsNew.get( 1 ) );
            expUploads.add( keys[ 3 ], uploadIdsOld.get( 3 ) );
            expUploads.add( keys[ 3 ], uploadIdsNew.get( 3 ) );
            PartUploadUtils.checkListMultipartUploadsResults( result,
                    expCommonPrefixes, expUploads );
        }

        @ExecuteOrder(step = 4, desc = "shutdown s3")
        private void shutdownS3() {
            s3.shutdown();
        }
    }
}