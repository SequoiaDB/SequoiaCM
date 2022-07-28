package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.Base64;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.testcommon.TestTools;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @description SCM-4313:上传一个分段
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4313 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4313";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private File file = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        long fileSize = 6 * 1024 * 1024;
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
    public void testUpload() throws Exception {
        String keyName = "/aa/bb/object4313";

        // 指定content-Md5分段长度等于对象长度
        long partSize = 6 * 1024 * 1024;
        testUpload1( keyName + "a", partSize );

        // 分段长度小于对象长度
        partSize = 5 * 1024 * 1024;
        testUpload3( keyName + "c", partSize );
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

    private void testUpload1( String keyName, long setPartSize )
            throws Exception {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );

        List< PartETag > partEtags = new ArrayList<>();
        String[] md5 = getMD5s( file, setPartSize );

        String contentMd5 = md5[ 0 ];

        // 指定content-Md5上传一个分段
        UploadPartRequest partRequest = new UploadPartRequest().withFile( file )
                .withFileOffset( 0 ).withPartNumber( 1 )
                .withPartSize( setPartSize ).withBucketName( bucketName )
                .withKey( keyName ).withUploadId( uploadId )
                .withMD5Digest( contentMd5 );
        UploadPartResult uploadPartResult = s3Client.uploadPart( partRequest );
        partEtags.add( uploadPartResult.getPartETag() );
        String expPartMd5 = md5[ 1 ];
        String actPartMd5 = uploadPartResult.getPartETag().getETag();
        Assert.assertEquals( actPartMd5, expPartMd5, "part number = 1" );

        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );
        String expMd5 = TestTools.getMD5( filePath );
        String actMd5 = S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName );
        Assert.assertEquals( actMd5, expMd5 );
    }

    private void testUpload2( String keyName, long setPartSize ) {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );

        UploadPartRequest partRequest = new UploadPartRequest().withFile( file )
                .withFileOffset( 0 ).withPartNumber( 1 )
                .withPartSize( setPartSize ).withBucketName( bucketName )
                .withKey( keyName ).withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "upload should fail." );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "IncompleteBody" );
        }

        Assert.assertFalse( s3Client.doesObjectExist( bucketName, keyName ) );
    }

    private void testUpload3( String keyName, long setPartSize )
            throws Exception {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );

        List< PartETag > partEtags = new ArrayList<>();
        String[] md5 = getMD5s( file, setPartSize );
        UploadPartRequest partRequest = new UploadPartRequest().withFile( file )
                .withFileOffset( 0 ).withPartNumber( 1 )
                .withPartSize( setPartSize ).withBucketName( bucketName )
                .withKey( keyName ).withUploadId( uploadId );
        UploadPartResult uploadPartResult = s3Client.uploadPart( partRequest );
        partEtags.add( uploadPartResult.getPartETag() );

        String expPartMd5 = md5[ 1 ];
        String actPartMd5 = uploadPartResult.getPartETag().getETag();
        Assert.assertEquals( actPartMd5, expPartMd5, "part number = "
                + uploadPartResult.getPartETag().getPartNumber() );

        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );
        String expMd5 = Objects
                .requireNonNull( getMD5s( file, setPartSize ) )[ 1 ];
        String downloadMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downloadMd5, expMd5 );
    }

    private String[] getMD5s( File file, long partsize ) throws IOException {
        String[] md5s = new String[ 2 ];
        FileInputStream fileInputStream = null;
        int length = ( int ) file.length();
        try {
            MessageDigest md5 = MessageDigest.getInstance( "MD5" );
            fileInputStream = new FileInputStream( file );
            byte[] buffer = new byte[ length ];
            if ( fileInputStream.read( buffer ) != -1 ) {
                md5.update( buffer, ( int ) ( long ) 0, ( int ) partsize );
            }

            byte[] digest = md5.digest();
            // 请求中携带md5需经过base64加密
            md5s[ 0 ] = Base64.encodeAsString( digest );
            // 文件指定部分的md5值
            md5s[ 1 ] = new String( Hex.encodeHex( digest ) );
            return md5s;
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        } finally {
            if ( fileInputStream != null ) {
                fileInputStream.close();
            }
        }
    }
}
