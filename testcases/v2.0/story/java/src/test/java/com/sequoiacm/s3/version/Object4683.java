package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @descreption SCM-4683 :: 带versionId删除对象，该对象为删除标记
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4683 extends TestScmBase {
    private String bucketName = "bucket4683";
    private String keyName = "object4683";
    private int deleteVersionNum = 3;
    private List< S3VersionSummary > expDeleteMarkersList = new ArrayList< S3VersionSummary >();
    private AmazonS3 s3Client = null;
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );

    @DataProvider(name = "removeProvider")
    public Object[][] generateRemoveIndex() {
        return new Object[][] {
                // delete the latest version of deletemarker
                new Object[] { 0 },
                // delete the history version of deletemarker
                new Object[] { 1 } };
    }

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        // create bucket and set bucket status is enabled
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test(dataProvider = "removeProvider")
    public void testGetObjectList( int remove_index ) {
        for ( int i = 1; i <= deleteVersionNum; i++ ) {
            s3Client.deleteObject( bucketName, keyName );
            S3VersionSummary version = new S3VersionSummary();
            version.setKey( keyName );
            version.setIsDeleteMarker( true );
            version.setVersionId( i + ".0" );
            expDeleteMarkersList.add( version );
        }

        // remove_index ：which index should be deleted from expDeleteMarkersList
        s3Client.deleteVersion( bucketName, keyName,
                expDeleteMarkersList.get( remove_index ).getVersionId() );

        // check the object version list
        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( req );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        checkDeleteMarkerResult( verList, remove_index );

        S3Utils.deleteAllObjectVersions( s3Client, bucketName );
        expDeleteMarkersList.clear();
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( actSuccessTests.get() == generateRemoveIndex().length ) {
                S3Utils.clearBucket( s3Client, bucketName);
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkDeleteMarkerResult( List< S3VersionSummary > verList,
            int removeIndex ) {
        expDeleteMarkersList.remove( removeIndex );
        Assert.assertEquals( verList.size(), expDeleteMarkersList.size() );
        for ( int i = 0; i < verList.size(); i++ ) {
            Assert.assertEquals( verList.get( i ).getKey(), expDeleteMarkersList
                    .get( ( deleteVersionNum - 2 ) - i ).getKey() );
            Assert.assertEquals( verList.get( i ).isDeleteMarker(),
                    expDeleteMarkersList.get( ( deleteVersionNum - 2 ) - i )
                            .isDeleteMarker() );
            Assert.assertEquals( verList.get( i ).getVersionId(),
                    expDeleteMarkersList.get( ( deleteVersionNum - 2 ) - i )
                            .getVersionId() );
        }
    }
}
