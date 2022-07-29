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
import java.util.Collections;
import java.util.List;

/**
 * @Description SCM-4630 :: 桶开启版本控制，增加多个同名对象
 * @author wuyan
 * @Date 2022.07.07
 * @version 1.00
 */
public class Object4630 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "aa/bb/object4630";
    private String bucketName = "bucket4630";
    private AmazonS3 s3Client = null;
    private int versionNum = 20;
    private String expContent = "object_________写入对象内容数据_file4630";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test
    public void testCreateObject() throws Exception {
        List< String > contentList = new ArrayList<>();
        for ( int i = 0; i < versionNum; i++ ) {
            String currentExpContent = expContent + "." + i;
            s3Client.putObject( bucketName, keyName, currentExpContent );
            contentList.add( currentExpContent );
        }
        Collections.reverse( contentList );
        checkPutObjectResult( contentList );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        keyName );
                s3Client.deleteBucket( bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkPutObjectResult( List< String > contentList )
            throws Exception {
        VersionListing listing = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        List< S3VersionSummary > versionlist = listing.getVersionSummaries();
        int objectListSize = versionlist.size();
        Assert.assertEquals( objectListSize, versionNum );

        // 检查版本和内容
        for ( int i = 0; i < objectListSize; i++ ) {
            String versionId = versionlist.get( i ).getVersionId();
            Assert.assertEquals( versionId,
                    String.valueOf( ( versionNum - i ) * 1.0 ),
                    "versionid is wrong!" );
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    keyName, versionId );
            S3Object object = s3Client.getObject( request );
            S3ObjectInputStream s3is = object.getObjectContent();
            byte[] bytes = new byte[ s3is.available() ];
            s3is.read( bytes );
            String actContent = new String( bytes );
            s3is.close();
            Assert.assertEquals( actContent, contentList.get( i ),
                    "---key version =" + versionId );

        }
    }
}
