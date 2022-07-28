package com.sequoiacm.bigfile;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-4351:上传分段时指定分段长度较大
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4351 extends TestScmBase {
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );
    private String bucketName = "bucket4351";
    private String keyName = "key4351";
    private String uploadId = "";
    private MultiValueMap< Integer, String > expPartsMap = null;
    private AmazonS3 s3Client = null;
    private long fileSize = 5 * 1024 * 1024 * 1024L;
    private File localPath = null;
    private File file = null;
    private String filePath = null;

    @DataProvider(name = "partSizeProvider")
    public Object[][] generateObjectNumber() {
        return new Object[][] {
                // test a : 500M
                new Object[] { 500 * 1024 * 1024 },
                // test b : 1G
                new Object[] { 1 * 1024 * 1024 * 1024L } };
    }

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    // 用例执行时间过长，ci上暂时屏蔽
    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "partSizeProvider", enabled = false)
    public void testUpload( long partSize ) throws Exception {
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        expPartsMap = new LinkedMultiValueMap< Integer, String >();
        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        List< PartETag > partEtags = partUpload( partSize );
        checkPartList();
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );

        String expMd5 = TestTools.getMD5( filePath );
        // delete uploaded files to save device space
        if ( !file.delete() ) {
            throw new Exception( "delete upload file failed!" );
        }
        // check
        String downloadMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downloadMd5, expMd5 );
        TestTools.LocalFile.removeFile( localPath );
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( actSuccessTests.get() == generateObjectNumber().length ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void checkPartList() {
        MultiValueMap< Integer, String > actPartsMap = new LinkedMultiValueMap< Integer, String >();
        ListPartsRequest request = new ListPartsRequest( bucketName, keyName,
                uploadId );
        PartListing result = s3Client.listParts( request );
        List< PartSummary > parts = result.getParts();
        for ( PartSummary part : parts ) {
            actPartsMap.add( part.getPartNumber(), part.getETag() );
            actPartsMap.add( part.getPartNumber(),
                    String.valueOf( part.getSize() ) );
        }
        Assert.assertEquals( actPartsMap.size(), expPartsMap.size(),
                "actPartsMap = " + actPartsMap.toString() + ",expMap = "
                        + expPartsMap.toString() );
        for ( Entry< Integer, List< String > > entry : expPartsMap
                .entrySet() ) {
            Assert.assertEquals( actPartsMap.get( entry.getKey() ),
                    expPartsMap.get( entry.getKey() ),
                    "actPartsMap = " + actPartsMap.toString() + ",expMap = "
                            + expPartsMap.toString() );
        }
    }

    private List< PartETag > partUpload( long partSize ) throws IOException {
        List< PartETag > partEtags = new ArrayList<>();
        long filePosition = 0;
        long fileSize = file.length();
        for ( int i = 1; filePosition < fileSize; i++ ) {
            long eachPartSize = Math.min( partSize, fileSize - filePosition );
            UploadPartRequest partRequest = new UploadPartRequest()
                    .withFile( file ).withFileOffset( filePosition )
                    .withPartNumber( i ).withPartSize( eachPartSize )
                    .withBucketName( bucketName ).withKey( keyName )
                    .withUploadId( uploadId );
            UploadPartResult uploadPartResult = s3Client
                    .uploadPart( partRequest );

            expPartsMap.add( i, TestTools.getLargeFilePartMD5( file,
                    filePosition, eachPartSize ) );
            expPartsMap.add( i, String.valueOf( eachPartSize ) );

            partEtags.add( uploadPartResult.getPartETag() );
            filePosition += partSize;
        }
        return partEtags;
    }
}
