package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4685 :: 设置不同版本控制状态，删除对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4685 extends TestScmBase {
    private String bucketName = "bucket4685";
    private String keyName = "object4685";
    private String fileContent = "content4685";
    private List< String > versionId = new ArrayList();
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName);
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        s3Client.putObject( bucketName, keyName, fileContent );
    }

    @Test
    public void testGetObjectList() throws Exception {
        PutObjectResult result = null;
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        result = s3Client.putObject( bucketName, keyName, fileContent );
        versionId.add( result.getVersionId() );

        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        s3Client.putObject( bucketName, keyName, fileContent );

        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        result = s3Client.putObject( bucketName, keyName, fileContent );
        versionId.add( result.getVersionId() );

        // delete object with versions
        for ( int i = 0; i < versionId.size(); i++ ) {
            s3Client.deleteVersion( bucketName, keyName, versionId.get( i ) );
        }
        s3Client.deleteVersion( bucketName, keyName, "null" );
        // check the object version list
        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( req );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        Assert.assertEquals( verList.size(), 0, "object is still exist!" );

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
