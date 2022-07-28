package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-4435:withMetadata接口参数校验
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4435 extends TestScmBase {
    private final String bucketName = "bucket4435";
    private final String keyName = "key4435";
    private File file = null;
    private AmazonS3 s3Client = null;
    private final AtomicInteger actSuccessTests = new AtomicInteger( 0 );

    @DataProvider(name = "legalMetadataProvider")
    public Object[][] generateMetadata() {
        Map< String, String > expMeta = new HashMap<>();
        expMeta.put( "test1", "1234" );
        expMeta.put( "test2", "" );
        expMeta.put( "test3", null );

        Map< String, String > expMeta2 = new HashMap<>();
        expMeta2.put( "test", TestTools.getRandomString( 2044 ) );

        return new Object[][] {
                // test a : 合法元数据信息，空串，null
                new Object[] { expMeta },
                // test b : 长度等于2kB （key+value总大小）
                new Object[] { expMeta2 } };
    }

    @BeforeClass
    private void setUp() throws Exception {
        File localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        long fileSize = 5 * 1024;
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "legalMetadataProvider")
    public void testLegalMetaData( Map< String, String > meta ) {
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
                bucketName, keyName );
        ObjectMetadata userMetadata = new ObjectMetadata();
        userMetadata.setUserMetadata( meta );
        initRequest.withObjectMetadata( userMetadata );
        InitiateMultipartUploadResult result = s3Client
                .initiateMultipartUpload( initRequest );
        String uploadId = result.getUploadId();
        Assert.assertNotEquals( uploadId, null );
        List< PartETag > partEtags = PartUploadUtils.partUpload( s3Client,
                bucketName, keyName, uploadId, file );
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );

        ObjectMetadata metadata = s3Client.getObjectMetadata( bucketName,
                keyName );
        Map< String, String > actMeta;
        actMeta = metadata.getUserMetadata();
        Assert.assertEquals( actMeta.size(), meta.size(), "expMeta is : "
                + printMap( meta ) + "actMeta is : " + printMap( actMeta ) );
        for ( Map.Entry< String, String > entry : meta.entrySet() ) {
            String expValue = entry.getValue() == null ? "" : entry.getValue();
            String actValue = actMeta.get( entry.getKey() ) == null ? ""
                    : actMeta.get( entry.getKey() );
            if ( !expValue.equals( actValue ) ) {
                Assert.fail( "Metadata is wrong ! exp : " + expValue
                        + ", but found : " + actValue );
            }
        }
        actSuccessTests.getAndIncrement();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testIllegalMetaData() {
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
                bucketName, keyName );
        ObjectMetadata metadata = new ObjectMetadata();
        Map< String, String > meta = new HashMap<>();
        meta.put( "test", TestTools.getRandomString( 2045 ) );
        metadata.setUserMetadata( meta );
        initRequest.withObjectMetadata( metadata );
        try {
            s3Client.initiateMultipartUpload( initRequest );
            Assert.fail( "when size more than 2KB , it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorMessage(),
                    "Your metadata headers exceed the maximum allowed metadata size." );
        }
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( actSuccessTests.get() == generateMetadata().length + 1 ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private String printMap( Map< String, String > map ) {
        StringBuilder str = new StringBuilder();
        for ( Map.Entry< String, String > entry : map.entrySet() ) {
            str.append( "Key = " ).append( entry.getKey() )
                    .append( " value = " ).append( entry.getValue() )
                    .append( " " );
        }
        return str.toString();
    }
}
