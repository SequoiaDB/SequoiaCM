package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4371:带maxparts查询分段列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4371 extends TestScmBase {
    private int runSuccessNum = 0;
    private int expRunSuccessNum = 3;
    private AmazonS3 s3Client;
    private File localPath;
    private String filePath;
    private File file;
    private long fileSize = 25 * 1024 * 1024;
    private int maxPartNumber = 5;
    private String key = "/aa/bb/obj4371";
    private String bucketName = "bucket4371";
    private String uploadId;
    private List< PartETag > partETags = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName, key );
        partETags = PartUploadUtils.partUpload( s3Client, bucketName, key,
                uploadId, file, fileSize / maxPartNumber );
        PartUploadUtils.listPartsAndCheckPartNumbers( s3Client, bucketName, key,
                partETags, uploadId );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_ltMaxPartNumber() {
        ListPartsRequest request = new ListPartsRequest( bucketName, key,
                uploadId );
        request.setMaxParts( maxPartNumber - 1 );
        PartListing partList = s3Client.listParts( request );
        List< PartSummary > parts = partList.getParts();
        Assert.assertEquals( parts.size(), maxPartNumber - 1 );
        for ( int i = 0; i < parts.size(); i++ ) {
            PartSummary partSumm = parts.get( i );
            Assert.assertEquals( partSumm.getETag(),
                    partETags.get( i ).getETag() );
            Assert.assertEquals( partSumm.getPartNumber(), i + 1 );
        }
        int nextPartNumberMarker = partList.getNextPartNumberMarker();
        Assert.assertEquals( nextPartNumberMarker, maxPartNumber - 1 );

        // set nextPartNumberMarker
        request.setPartNumberMarker( nextPartNumberMarker );
        PartListing partList2 = s3Client.listParts( request );
        List< PartSummary > parts2 = partList2.getParts();
        Assert.assertEquals( parts2.size(), 1 );
        Assert.assertEquals( parts2.get( 0 ).getETag(),
                partETags.get( maxPartNumber - 1 ).getETag() );
        Assert.assertEquals( parts2.get( 0 ).getPartNumber(), maxPartNumber );

        runSuccessNum++;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_gtMaxPartNumber() {
        ListPartsRequest request = new ListPartsRequest( bucketName, key,
                uploadId );
        request.setMaxParts( maxPartNumber + 1 );
        PartListing partList = s3Client.listParts( request );
        List< PartSummary > parts = partList.getParts();
        Assert.assertEquals( parts.size(), maxPartNumber );
        for ( int i = 0; i < parts.size(); i++ ) {
            PartSummary partSumm = parts.get( i );
            Assert.assertEquals( partSumm.getETag(),
                    partETags.get( i ).getETag() );
            Assert.assertEquals( partSumm.getPartNumber(), i + 1 );
        }
        runSuccessNum++;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test_etMaxPartNumber() {
        ListPartsRequest request = new ListPartsRequest( bucketName, key,
                uploadId );
        request.setMaxParts( maxPartNumber );
        PartListing partList = s3Client.listParts( request );
        List< PartSummary > parts = partList.getParts();
        Assert.assertEquals( parts.size(), maxPartNumber );
        for ( int i = 0; i < parts.size(); i++ ) {
            PartSummary partSumm = parts.get( i );
            Assert.assertEquals( partSumm.getETag(),
                    partETags.get( i ).getETag() );
            Assert.assertEquals( partSumm.getPartNumber(), i + 1 );
        }
        runSuccessNum++;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccessNum == expRunSuccessNum ) {
                s3Client.abortMultipartUpload( new AbortMultipartUploadRequest(
                        bucketName, key, uploadId ) );
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
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
}