package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
import java.io.IOException;
import java.util.Date;

/**
 * @descreption SCM-4645 :: 指定ifUnModifiedSince和ifModifiedSince条件获取对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4645 extends TestScmBase {
    private boolean runSuccess = false;
    private String key = "/aa/bb/object4645";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 100;
    private int updateSize = 1024 * 150;
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
        Date createDate = getCreateDate( enableVerBucketName );
        s3Client.putObject( enableVerBucketName, key, new File( updatePath ) );

        // set date one day later than create time
        long timestamp1 = createDate.getTime() + 96784000l;
        // current time 1 seccond earlier to reduce acquisition error
        long timestamp2 = createDate.getTime() - 1000;
        Date unModifydate = new Date( timestamp1 );
        Date modifydate = new Date( timestamp2 );

        String curVersionId = "2.0";
        GetObjectRequest request = new GetObjectRequest( enableVerBucketName,
                key, curVersionId )
                        .withUnmodifiedSinceConstraint( unModifydate )
                        .withModifiedSinceConstraint( modifydate );
        S3Object object = s3Client.getObject( request );

        // match current version object
        checkGetObjectResult( object, updatePath );
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

    private Date getCreateDate( String bucketName ) {
        S3Object object = s3Client.getObject( bucketName, key );
        ObjectMetadata metadata = object.getObjectMetadata();
        Date date = metadata.getLastModified();
        return date;
    }

    private void checkGetObjectResult( S3Object object, String filePath )
            throws Exception {
        S3ObjectInputStream s3is = object.getObjectContent();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Utils.inputStream2File( s3is, downloadPath );
        String getMd5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( getMd5, TestTools.getMD5( filePath ) );
    }
}
