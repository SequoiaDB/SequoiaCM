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

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4679 :: 开启版本控制，带versionId删除历史版本对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4679 extends TestScmBase {
    private String bucketName = "bucket4679";
    private String keyName = "testKey4679";
    private List< S3VersionSummary > expVersionList = new ArrayList<>();
    private int oneObjVersionNum = 3;
    private String fileContent = "content4679";
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName);
        // create bucket
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );

        for ( int i = 0; i < oneObjVersionNum; i++ ) {
            PutObjectResult result = s3Client.putObject( bucketName, keyName,
                    fileContent + "." + i + ".0" );
            S3VersionSummary version = new S3VersionSummary();
            version.setKey( keyName );
            version.setVersionId( result.getVersionId() );
            expVersionList.add( version );
        }
    }

    @Test(groups = { GroupTags.base })
    public void testGetObjectList() throws Exception {
        // delete object with latest version id
        String historyVersionId = "1.0";
        s3Client.deleteVersion( bucketName, keyName, historyVersionId );
        expVersionList.remove( 0 );

        try {
            s3Client.getObject( new GetObjectRequest( bucketName, keyName,
                    historyVersionId ) );
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
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName);
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
