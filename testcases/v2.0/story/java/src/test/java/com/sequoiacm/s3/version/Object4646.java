package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
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
 * @descreption SCM-4646 ::
 *              指定ifUnModifiedSince和ifModifiedSince条件获取对象，不匹配ifUnModifiedSince
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4646 extends TestScmBase {
    private boolean runSuccess = false;
    private String key = "/aa/bb/object4646";
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

        // set date one day less than create time
        long timestamp = createDate.getTime() - 96784000l;
        Date unModifyDate = new Date( timestamp );
        String curVersionId = "2.0";
        GetObjectRequest request = new GetObjectRequest( enableVerBucketName,
                key, curVersionId );
        request.withUnmodifiedSinceConstraint( unModifyDate )
                .withModifiedSinceConstraint( createDate );
        S3Object object = s3Client.getObject( request );

        Assert.assertNull( object, "does not match object!" );
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
}
