package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @description SCM-4374:带partnumberMarker和nextPartnumberMarker查询分段列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4374 extends TestScmBase {
    private boolean runSuccess = false;
    private int partNumber = 6;
    private String bucketName = "bucket4374";
    private String keyName = "key4374";
    private AmazonS3 s3Client = null;
    private long fileSize = partNumber * PartUploadUtils.partLimitMinSize;
    private File localPath = null;
    private File file = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testListParts() {
        List< Integer > partNumbers = new ArrayList<>(
                Arrays.asList( 1, 3, 5, 6, 7, 8 ) );
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        partUpload( uploadId, partNumbers );

        int maxParts = 3;
        int partNumberMarker = 1;
        List< Integer > actPartNumbersList = new ArrayList<>();
        ListPartsRequest request = new ListPartsRequest( bucketName, keyName,
                uploadId );
        request.setMaxParts( maxParts );
        request.setPartNumberMarker( partNumberMarker );
        PartListing listResult = s3Client.listParts( request );
        List< PartSummary > listParts = listResult.getParts();
        for ( PartSummary parts : listParts ) {
            int partNumber = parts.getPartNumber();
            actPartNumbersList.add( partNumber );
        }

        // 获取返回的nextPartNumberMarker，再次查询
        int nextPartNumberMarker = listResult.getNextPartNumberMarker();
        request.setPartNumberMarker( nextPartNumberMarker );
        listResult = s3Client.listParts( request );
        listParts = listResult.getParts();
        for ( PartSummary parts : listParts ) {
            int partNumber = parts.getPartNumber();
            actPartNumbersList.add( partNumber );
        }
        // 检查结果
        List< Integer > expPartNumbers = new ArrayList<>( partNumbers );
        expPartNumbers.remove( 0 );
        Assert.assertEquals( actPartNumbersList, expPartNumbers );
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
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void partUpload( String uploadId, List< Integer > partNumbers ) {
        List< PartETag > partEtags = new ArrayList<>();
        int filePosition = 0;
        long partSize = PartUploadUtils.partLimitMinSize;
        for ( int i = 0; filePosition < fileSize; i++ ) {
            long eachPartSize = Math.min( partSize, fileSize - filePosition );
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( filePosition )
                    .withPartNumber( partNumbers.get( i ) )
                    .withPartSize( eachPartSize ).withBucketName( bucketName )
                    .withKey( keyName ).withUploadId( uploadId );
            UploadPartResult uploadPartResult = s3Client
                    .uploadPart( partRequest );
            partEtags.add( uploadPartResult.getPartETag() );
            filePosition += partSize;
        }
    }
}
