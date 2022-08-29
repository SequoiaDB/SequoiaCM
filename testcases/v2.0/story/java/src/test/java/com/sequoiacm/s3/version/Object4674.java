package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4674 :: 桶开启版本控制，不带versionId删除对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4674 extends TestScmBase {
    private boolean runSuccess = false;
    private String key = "&&aa&%maa&bb*中文&object4674";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 300;
    private int updateSize = 1024 * 20;
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

    @Test(groups = { GroupTags.base })
    public void testDeleteObject() throws Exception {
        s3Client.putObject( enableVerBucketName, key, new File( filePath ) );
        s3Client.putObject( enableVerBucketName, key, new File( updatePath ) );
        s3Client.deleteObject( enableVerBucketName, key );
        checkDeleteObjectResult( enableVerBucketName, key );
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

    private void checkDeleteObjectResult( String bucketName, String key )
            throws Exception {
        // current version object has been deleted
        boolean isExistObject = s3Client.doesObjectExist( bucketName, key );
        Assert.assertFalse( isExistObject, "the object should not exist!" );

        // deleted object has been a history version object,the versionId is
        // "2.0"
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key, "2.0" );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );

        // check the oldest version object,the version is "1.0"
        String downOldfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key, "1.0" );
        Assert.assertEquals( downOldfileMd5, TestTools.getMD5( filePath ) );
    }
}
