package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Date;

/**
 * @descreption SCM-4644 :: 指定ifNoneMatch条件，带versionId获取对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4644 extends TestScmBase {
    private boolean runSuccess = false;
    private String key = "/aa/bb/object4644";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 20;
    private int updateSize = 1024 * 15;
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

        // set date one day later than current time
        long currentTimestamp = new Date().getTime();
        long timestamp = currentTimestamp + 96784000l;
        Date date = new Date( timestamp );
        String eTag = TestTools.getMD5( updatePath );
        GetObjectRequest request = new GetObjectRequest( enableVerBucketName,
                key ).withNonmatchingETagConstraint( eTag )
                        .withUnmodifiedSinceConstraint( date );
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
}
