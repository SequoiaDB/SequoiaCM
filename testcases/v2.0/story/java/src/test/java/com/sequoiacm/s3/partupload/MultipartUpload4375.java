package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
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
import java.util.List;

/**
 * @description SCM-4375:带partnumberMarker和nextPartnumberMarker查询分段列表，其中设置nextPartnumberMarker前后匹配条件不一致
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4375 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "/aa/object4375";
    private String bucketName = "bucket4375";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024 * 50;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void listParts() throws Exception {
        File file = new File( filePath );
        String uploadId = PartUploadUtils.initPartUpload( s3Client,
                bucketName, keyName );
        List< Integer > partNumbers = partUpload( s3Client,
                bucketName, keyName, uploadId, file );

        // first listParts,set the partNumberMarker is 2
        ListPartsRequest listRequest = new ListPartsRequest( bucketName,
                keyName, uploadId );
        int maxParts = 4;
        int partNumberMarker = partNumbers.get( 1 );
        listRequest.withMaxParts( maxParts )
                .withPartNumberMarker( partNumberMarker );
        PartListing listResult = s3Client.listParts( listRequest );
        List< Integer > actPartNumbersList1 = getPartNumbers( listResult );
        List< Integer > expPartNumbers1 = partNumbers.subList( partNumberMarker,
                partNumberMarker + maxParts );
        Assert.assertEquals( actPartNumbersList1, expPartNumbers1 );

        // second listParts, reset PartNumberMarker,eg:begin to partNumber is 8
        Integer nextMarker = partNumbers.get( 7 );
        listRequest.setPartNumberMarker( nextMarker );
        listResult = s3Client.listParts( listRequest );
        List< Integer > actPartNumbersList2 = getPartNumbers( listResult );
        List< Integer > expPartNumbers2 = partNumbers.subList( nextMarker,
                partNumbers.size() );
        Assert.assertEquals( actPartNumbersList2, expPartNumbers2 );
        Assert.assertFalse( listResult.isTruncated() );

        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                bucketName, keyName, uploadId );
        s3Client.abortMultipartUpload( request );
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

    private List< Integer > partUpload( AmazonS3 s3Client, String bucketName,
            String key, String uploadId, File file ) {
        List< Integer > partNumbers = new ArrayList<>();
        int filePosition = 0;
        int partSize = 1024 * 1024 * 5;
        long fileSize = file.length();
        for ( int i = 1; filePosition < fileSize; i++ ) {
            long eachPartSize = Math.min( partSize, fileSize - filePosition );
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( filePosition )
                    .withPartNumber( i ).withPartSize( eachPartSize )
                    .withBucketName( bucketName ).withKey( key )
                    .withUploadId( uploadId );
            s3Client.uploadPart( partRequest );
            partNumbers.add( i );
            filePosition += partSize;
        }
        return partNumbers;
    }

    private List< Integer > getPartNumbers( PartListing listResult ) {
        List< PartSummary > listParts = listResult.getParts();
        List< Integer > actPartNumbersList = new ArrayList<>();
        for ( PartSummary partNumbers : listParts ) {
            int partNumber = partNumbers.getPartNumber();
            actPartNumbersList.add( partNumber );
        }
        return actPartNumbersList;
    }
}
