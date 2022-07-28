package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-4372:带partnumberMarker查询分段列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4372 extends TestScmBase {
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );
    private int partNumber = 5;
    private String bucketName = "bucket4372";
    private String keyName = "key4372";
    private AmazonS3 s3Client = null;
    private long fileSize = partNumber * PartUploadUtils.partLimitMinSize;
    private File localPath = null;
    private File file = null;
    private String filePath = null;
    private String uploadId = "";

    @DataProvider(name = "partNumberMarkerProvider")
    public Object[][] generateObjectNumber() {
        // parameter : partNumberMarker, expPartNumbersList
        return new Object[][] {
                // test a : 指定第一个partnumber
                new Object[] { 1, Arrays.asList( 2, 3, 4, 5 ) },
                // test b : 指定中间位置的分段号
                new Object[] { partNumber / 2, Arrays.asList( 3, 4, 5 ) },
                // test c : 指定最后一个分段号
                new Object[] { partNumber, new ArrayList<>() },
                // test d : 指定倒数第二个分段号
                new Object[] { partNumber - 1, Collections.singletonList( 5 ) },
                // test e : 指定大于所有分段号的值
                new Object[] { partNumber + 1, new ArrayList<>() } };
    }

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
        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        PartUploadUtils.partUpload( s3Client, bucketName, keyName, uploadId,
                file );
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "partNumberMarkerProvider")
    public void testListParts( Integer partNumberMarker,
            List< Integer > expPartNumbersList ) {
        ListPartsRequest request = new ListPartsRequest( bucketName, keyName,
                uploadId );
        request.setPartNumberMarker( partNumberMarker );
        PartListing listResult = s3Client.listParts( request );
        List< PartSummary > listParts = listResult.getParts();
        List< Integer > actPartNumbersList = new ArrayList<>();
        for ( PartSummary parts : listParts ) {
            int partNumber = parts.getPartNumber();
            actPartNumbersList.add( partNumber );
        }

        // check the keyName
        Assert.assertEquals( actPartNumbersList, expPartNumbersList );
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
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
