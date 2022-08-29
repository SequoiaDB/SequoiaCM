package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Iterator;

/**
 * @descreption SCM-4676 :: 开启版本控制，不带versionId删除标记为删除的对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4676 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4676";
    private String key = "&aa&%maa&bb*中文&objectOfdeleteTag4676";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName);
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        // generate object of delete tag,the vesionID is "0" and "1"
        s3Client.deleteObject( bucketName, key );
        s3Client.deleteObject( bucketName, key );
    }

    @Test(groups = { GroupTags.base })
    public void testDeleteObject() throws Exception {
        s3Client.deleteObject( bucketName, key );
        checkDeleteObjectResult( bucketName, key );
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

    private void checkDeleteObjectResult( String bucketName, String key )
            throws Exception {
        // current version object not exist
        boolean isExistObject = s3Client.doesObjectExist( bucketName, key );
        Assert.assertFalse( isExistObject, "the object should not exist!" );

        // delete the object of delete tag , add a delete tag
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        Iterator< S3VersionSummary > versionIter = versionList
                .getVersionSummaries().iterator();
        int count = 0;
        while ( versionIter.hasNext() ) {
            S3VersionSummary vs = versionIter.next();
            String getKey = vs.getKey();
            boolean isDeleteMarker = vs.isDeleteMarker();
            Assert.assertEquals( getKey, key );
            Assert.assertTrue( isDeleteMarker );
            count++;
        }
        int deleteTagNums = 3;
        Assert.assertEquals( count, deleteTagNums );
    }
}
