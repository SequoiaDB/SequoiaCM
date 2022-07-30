package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4632 :: 不带versionId获取对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4632 extends TestScmBase {
    private final String bucketName = "bucket4632";
    private final String key = "aa/bb/object4632";
    private boolean runSuccess = false;
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 5;
    private int updateSize = 1024 * 2;
    private int objectNums = 20;
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
        S3Utils.clearBucket( s3Client, bucketName );

        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test
    public void testGetObject() throws Exception {
        updateObject( bucketName );
        getObjectAndCheckResult( bucketName );
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
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void updateObject( String bucketName ) {
        for ( int i = 0; i < objectNums; i++ ) {
            s3Client.putObject( bucketName, key, new File( filePath ) );
        }
        // create the new Object
        s3Client.putObject( bucketName, key, new File( updatePath ) );
    }

    private void getObjectAndCheckResult( String bucketName ) throws Exception {
        S3Object object = s3Client.getObject( bucketName, key );
        ObjectMetadata metadata = object.getObjectMetadata();
        // check the versionId is maximum versionId:21.0
        String versionId = metadata.getVersionId();
        String curVersionId = ( objectNums + 1 ) + ".0";
        Assert.assertEquals( versionId, curVersionId );

        // check the etag equal to the md5 of the last update content
        String etag = metadata.getETag();
        Assert.assertEquals( etag, TestTools.getMD5( updatePath ) );

        // check the content
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );
    }
}
