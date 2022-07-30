package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Iterator;

/**
 * @descreption SCM-4677 :: 对象存在多个版本，版本控制由开启设置为禁用，不带versionId删除对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4677 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4677";
    private final String key = "//aa/%maa/bb*中文/object4677";
    private AmazonS3 s3Client = null;
    private final int fileSize = 1024 * 300;
    private final int updateSize = 1024 * 20;
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
        S3Utils.clearBucket( s3Client, bucketName);

        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        s3Client.putObject( bucketName, key, new File( filePath ) );
        s3Client.putObject( bucketName, key, new File( updatePath ) );
    }

    @Test
    public void test() throws Exception {
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        s3Client.deleteObject( bucketName, key );
        checkDeleteObjectResult( bucketName, key );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName);
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
        // current version object does not exist
        boolean isExistObject = s3Client.doesObjectExist( bucketName, key );
        Assert.assertFalse( isExistObject, "the object should not exist!" );

        // deleted object has been a history version object,the versionId is
        // "2.0"
        String downFileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key, "2.0" );
        Assert.assertEquals( downFileMd5, TestTools.getMD5( updatePath ) );

        // check the oldest version object,the version is "1.0"
        String downOldFileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key, "1.0" );
        Assert.assertEquals( downOldFileMd5, TestTools.getMD5( filePath ) );

        // delete the object, add a delete tag ,the object num is 3
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        Iterator< S3VersionSummary > versionIter = versionList
                .getVersionSummaries().iterator();
        int count = 0;
        int deleteTagNums = 0;
        while ( versionIter.hasNext() ) {
            S3VersionSummary vs = versionIter.next();
            String getKey = vs.getKey();
            boolean isDeleteMarker = vs.isDeleteMarker();
            if ( isDeleteMarker ) {
                // the object of delete tag versionId is "null"
                Assert.assertEquals( vs.getVersionId(), "null" );
                deleteTagNums++;
            }
            Assert.assertEquals( getKey, key );
            count++;
        }
        int expDeleteTagNums = 1;
        int expObjectNums = 3;
        Assert.assertEquals( count, expObjectNums );
        Assert.assertEquals( deleteTagNums, expDeleteTagNums );
    }
}
