package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4639 :: 指定ifMatch条件，带versionId获取对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4639 extends TestScmBase {
    private boolean runSuccess = false;
    private String key = "aa/bb/object4639";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 40;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.deleteObjectAllVersions( s3Client, enableVerBucketName, key );
    }

    @Test
    public void testGetObject() throws Exception {
        s3Client.putObject( enableVerBucketName, key, new File( filePath ) );
        s3Client.putObject( enableVerBucketName, key, new File( updatePath ) );

        String curVersionId = "2.0";
        String hisVersionId = "1.0";
        // match curVersion object etag
        getObjectWithIfMatchAndCheckContent( enableVerBucketName, key,
                curVersionId, updatePath );
        // match hisVersion object etag
        getObjectWithIfMatchAndCheckContent( enableVerBucketName, key,
                hisVersionId, filePath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.deleteObjectAllVersions( s3Client, enableVerBucketName,
                        key );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void getObjectWithIfMatchAndCheckContent( String bucketName,
            String key, String versionId, String filePath ) throws Exception {
        String eTag = TestTools.getMD5( filePath );
        GetObjectRequest request = new GetObjectRequest( bucketName, key,
                versionId );
        request.withMatchingETagConstraint( eTag );
        S3Object object = s3Client.getObject( request );
        checkGetObjectResult( object, filePath, versionId );
    }

    private void checkGetObjectResult( S3Object object, String filePath,
            String versionId ) throws Exception {
        S3ObjectInputStream s3is = object.getObjectContent();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Utils.inputStream2File( s3is, downloadPath );
        String getMd5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( getMd5, TestTools.getMD5( filePath ) );

        // check the versionId
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                versionId );
    }
}
