package com.sequoiacm.s3.partupload.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description SCM-4403:开启版本控制，相同key并发上传分段
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4403 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "/aa/object4403";
    private String bucketName = "bucket4403";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String[] filePaths = new String[ 5 ];
    private int[] fileSizes = { 1024 * 1024 * 50, 1024 * 1024 * 29,
            1024 * 1024 * 30, 1024 * 1024 * 10, 1024 * 1024 * 40 };

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileSizes.length; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSizes[ i ] + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSizes[ i ] );
            filePaths[ i ] = filePath;
        }

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test(groups = { GroupTags.base })
    public void uploadParts() throws Exception {

        ThreadExecutor threadExec = new ThreadExecutor();
        int[] partSizes = { 1024 * 1024 * 5, 1024 * 1024 * 6, 1024 * 1024 * 6,
                1024 * 1024 * 5, 1024 * 1024 * 10 };
        for ( int i = 0; i < filePaths.length; i++ ) {
            File file = new File( filePaths[ i ] );
            int partSize = partSizes[ i ];
            threadExec.addWorker( new PartUpload( file, partSize ) );
        }
        threadExec.run();

        // check the upload file
        listObjectsAndCheckResults();
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

    private void listObjectsAndCheckResults() throws Exception {
        MultiValueMap< Long, String > expFileSizeAndMd5 = new LinkedMultiValueMap< Long, String >();
        for ( int i = 0; i < filePaths.length; i++ ) {
            String fileMd5 = TestTools.getMD5( filePaths[ i ] );
            long fileSize = fileSizes[ i ];
            expFileSizeAndMd5.add( fileSize, fileMd5 );
        }
        // listObjectVersion, then get object to check filemd5
        VersionListing vsList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        List< S3VersionSummary > vsSummaryList = vsList.getVersionSummaries();
        List< String > versionList = new ArrayList<>();
        List< String > expVersionList = new ArrayList<>();
        double count = 1.0;
        for ( S3VersionSummary versionSummary : vsSummaryList ) {
            String keyName = versionSummary.getKey();
            String versionId = versionSummary.getVersionId();
            long size = versionSummary.getSize();
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, keyName, versionId );
            String expFileMd5 = expFileSizeAndMd5.get( size ).get( 0 );
            Assert.assertEquals( downfileMd5, expFileMd5,
                    "the object version is :" + versionId + "  object Size is:"
                            + size );
            expVersionList.add( count + "" );
            versionList.add( versionId );
            count++;
        }

        // check the versionId
        Collections.sort( expVersionList );
        Collections.sort( versionList );
        Assert.assertEquals( versionList, expVersionList );
        // the object has 5 versions.

        Assert.assertEquals( ( int ) count - 1, filePaths.length );
    }

    private class PartUpload {
        private File file;
        private String uploadId;
        private int partSize;
        private List< PartETag > partEtags;
        private AmazonS3 s3Client1 = S3Utils.buildS3Client();

        private PartUpload( File file, int partSize ) throws Exception {
            this.file = file;
            this.partSize = partSize;
        }

        @ExecuteOrder(step = 1)
        private void initPartUpload() {
            uploadId = PartUploadUtils.initPartUpload( s3Client1, bucketName,
                    keyName );
        }

        @ExecuteOrder(step = 2)
        private void partUpload() {
            partEtags = PartUploadUtils.partUpload( s3Client1, bucketName,
                    keyName, uploadId, file, partSize );
        }

        @ExecuteOrder(step = 3)
        private void completeMultipartUpload() {
            try {
                PartUploadUtils.completeMultipartUpload( s3Client1, bucketName,
                        keyName, uploadId, partEtags );
            } finally {
                if ( s3Client1 != null ) {
                    s3Client1.shutdown();
                }
            }
        }
    }
}
