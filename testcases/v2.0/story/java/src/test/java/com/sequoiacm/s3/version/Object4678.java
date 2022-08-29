package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4678 :: 开启版本控制，带versionId删除最新版本对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4678 extends TestScmBase {
    private final String bucketName = "bucket4678";
    private final String keyName = "testKey4678";
    private final List< S3VersionSummary > expVersionList = new ArrayList<>();
    private final int oneObjVersionNum = 3;
    private final String fileContent = "content4678";
    private File localPath = null;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName);
        // create bucket
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        for ( int i = 1; i <= oneObjVersionNum; i++ ) {
            PutObjectResult result = s3Client.putObject( bucketName, keyName,
                    fileContent + "." + i + ".0" );
            S3VersionSummary version = new S3VersionSummary();
            version.setKey( keyName );
            version.setVersionId( result.getVersionId() );
            expVersionList.add( version );
        }
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // delete object with the latest version id
        String latestVersionId = oneObjVersionNum + ".0";
        s3Client.deleteVersion( bucketName, keyName, latestVersionId );
        expVersionList.remove( oneObjVersionNum - 1 );

        try {
            s3Client.getObject( new GetObjectRequest( bucketName, keyName,
                    latestVersionId ) );
            Assert.fail( "the object still exist!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
        }

        // check the object version list
        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( req );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        Assert.assertEquals( verList.size(), expVersionList.size() );
        for ( int i = 0; i < verList.size(); i++ ) {
            Assert.assertEquals( verList.get( i ).getKey(), expVersionList
                    .get( ( oneObjVersionNum - 2 ) - i ).getKey() );
            Assert.assertEquals( verList.get( i ).getVersionId(), expVersionList
                    .get( ( oneObjVersionNum - 2 ) - i ).getVersionId() );
        }

        // check that the current latest version of the object is correct by the
        // MD5 value
        String currLatestVersionId = ( oneObjVersionNum - 1 ) + ".0";
        String downFileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downFileMd5, TestTools.getMD5(
                ( fileContent + "." + currLatestVersionId ).getBytes() ) );

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
}
