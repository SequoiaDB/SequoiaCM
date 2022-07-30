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
 * @descreption SCM-4697 :: 带Key-marker查询对象版本列表
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4697 extends TestScmBase {
    private String bucketName = "bucket4697";
    private String[] keyName = { "dir1%dir2%test1", "dir2%test2", "test3",
            "test4" };
    private List< S3VersionSummary > expVersionList = new ArrayList<>();
    private int oneObjVersionNum = 3;
    private String fileContent = "content4697";
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        // create bucket
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        for ( int i = 0; i < keyName.length; i++ ) {
            // create multi-version objects and save the key and versionId to
            // expVersionList
            for ( int j = 0; j < oneObjVersionNum; j++ ) {
                PutObjectResult result = s3Client.putObject( bucketName,
                        keyName[ i ], fileContent );
                S3VersionSummary version = new S3VersionSummary();
                version.setKey( keyName[ i ] );
                version.setVersionId(
                        String.valueOf( oneObjVersionNum - j ) + ".0" );
                expVersionList.add( version );
            }
        }
    }

    @Test
    public void testGetObjectList() throws Exception {
        List< S3VersionSummary > versionList = new ArrayList<>();
        // test a:指定位置为中间记录
        int index = ( keyName.length / 2 ) - 1;
        versionList = getVersionList( keyName[ index ] );
        checkVersionResult( versionList, index );

        // test b:指定第一条记录
        index = 0;
        versionList = getVersionList( keyName[ index ] );
        checkVersionResult( versionList, index );

        // test c:指定最后一条记录
        index = keyName.length - 1;
        versionList = getVersionList( keyName[ index ] );
        Assert.assertEquals( versionList.size(), 0,
                "testc:The number of returned results is not zero" );

        // test d:指定匹配最后一条记录
        index = ( keyName.length - 1 ) - 1;
        versionList = getVersionList( keyName[ index ] );
        checkVersionResult( versionList, index );

        // test e:指定匹配不到记录
        // i:指定记录小于所有记录
        versionList = getVersionList( "aaa" );
        checkVersionResult( versionList, -1 );

        // ii:指定记录大小位于所有记录中间
        index = ( keyName.length / 2 ) - 1;
        versionList = getVersionList( keyName[ index ] + "1" );
        checkVersionResult( versionList, index );

        // iii:指定记录大于所有记录
        index = keyName.length - 1;
        versionList = getVersionList( keyName[ index ] + "1" );
        Assert.assertEquals( versionList.size(), 0,
                "teste:The number of returned results is not zero" );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private List< S3VersionSummary > getVersionList( String KeyMarker ) {
        ListVersionsRequest req = new ListVersionsRequest()
                .withBucketName( bucketName ).withKeyMarker( KeyMarker );
        VersionListing versionList = s3Client.listVersions( req );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        return verList;
    }

    private void checkVersionResult( List< S3VersionSummary > versionList,
            int index ) {
        int startIndex = ( index + 1 ) * oneObjVersionNum;
        int expNum = ( keyName.length * oneObjVersionNum ) - startIndex;
        Assert.assertEquals( versionList.size(), expNum,
                "The total number of results is incorrect" );
        for ( int i = 0; i < versionList.size(); i++ ) {
            Assert.assertEquals( versionList.get( i ).getKey(),
                    expVersionList.get( startIndex ).getKey() );
            Assert.assertEquals( versionList.get( i ).getVersionId(),
                    expVersionList.get( startIndex ).getVersionId() );
            startIndex++;
        }

    }
}
