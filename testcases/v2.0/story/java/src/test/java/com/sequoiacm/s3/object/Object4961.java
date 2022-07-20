package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4961:使用deleteObjects接口，开启版本控制删除对象
 * @author YiPan
 * @date 2022/7/19
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4961 extends TestScmBase {
    private final String bucketName = "bucket4961";
    private final String baseObjectKey = "object4961";
    private AmazonS3 s3Client;
    private String content = "test4961";
    private DeleteObjectsRequest deleteRequest;
    private List< DeleteObjectsResult.DeletedObject > deletedObjects;
    private List< String > allObjectKeys = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.updateBucketVersionConfig( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( int i = 0; i < 10; i++ ) {
            allObjectKeys.add( baseObjectKey + i );
        }
    }

    @Test
    public void test() throws Exception {
        String deleteMarkerVersion;
        String deleteVersion;
        String historyVersion;
        String LastestVersion;

        // 不指定版本号删除对象
        S3Utils.deleteAllObjectVersions( s3Client, bucketName );
        putObject( 5 );
        deleteMarkerVersion = "6.0";
        deleteVersion = null;
        historyVersion = "4.0";
        deleteRequest = getDeleteRequest( deleteVersion );
        deletedObjects = s3Client.deleteObjects( deleteRequest )
                .getDeletedObjects();
        checkDeleteInfo( deletedObjects, allObjectKeys, deleteMarkerVersion,
                deleteVersion );
        checkExistObjectAndVersion( new ArrayList< String >(), null, null );
        checkObjectVersion( historyVersion );

        // 删除版本为"4.0"的对象
        S3Utils.deleteAllObjectVersions( s3Client, bucketName );
        putObject( 5 );
        deleteVersion = "4.0";
        LastestVersion = "5.0";
        deleteMarkerVersion = null;
        deleteRequest = getDeleteRequest( deleteVersion );
        deletedObjects = s3Client.deleteObjects( deleteRequest )
                .getDeletedObjects();
        checkDeleteInfo( deletedObjects, allObjectKeys, deleteMarkerVersion,
                deleteVersion );
        checkExistObjectAndVersion( allObjectKeys, LastestVersion,
                deleteVersion );

        // 删除版本为"1"的对象
        S3Utils.deleteAllObjectVersions( s3Client, bucketName );
        putObject( 5 );
        deleteVersion = "1.0";
        LastestVersion = "5.0";
        deleteMarkerVersion = null;
        deleteRequest = getDeleteRequest( deleteVersion );
        deletedObjects = s3Client.deleteObjects( deleteRequest )
                .getDeletedObjects();
        checkDeleteInfo( deletedObjects, allObjectKeys, deleteMarkerVersion,
                deleteVersion );
        checkExistObjectAndVersion( allObjectKeys, LastestVersion,
                deleteVersion );

        // 不带版本删除生成deleterMarker 6.0
        S3Utils.deleteAllObjectVersions( s3Client, bucketName );
        putObject( 5 );
        deleteMarkerVersion = "6.0";
        deleteVersion = null;
        deleteRequest = getDeleteRequest( deleteVersion );
        deletedObjects = s3Client.deleteObjects( deleteRequest )
                .getDeletedObjects();
        // 带版本删除deleterMarker5.0(等于恢复了删除，当前版本为5)
        LastestVersion = "5.0";
        deleteRequest = getDeleteRequest( deleteMarkerVersion );
        deletedObjects = s3Client.deleteObjects( deleteRequest )
                .getDeletedObjects();
        checkDeleteInfo( deletedObjects, allObjectKeys, deleteMarkerVersion,
                deleteMarkerVersion );
        for ( int i = 0; i < 10; i++ ) {
            S3Object object = s3Client.getObject( bucketName,
                    baseObjectKey + i );
            Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                    LastestVersion );
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private void putObject( int VersionNum ) {
        for ( int j = 0; j < VersionNum; j++ ) {
            for ( int i = 0; i < 10; i++ ) {
                s3Client.putObject( bucketName, baseObjectKey + i, content );
            }
        }
    }

    private DeleteObjectsRequest getDeleteRequest( String version ) {
        List< DeleteObjectsRequest.KeyVersion > deleteVersion = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            if ( version == null ) {
                deleteVersion.add( new DeleteObjectsRequest.KeyVersion(
                        baseObjectKey + i ) );
            } else {
                deleteVersion.add( new DeleteObjectsRequest.KeyVersion(
                        baseObjectKey + i, version ) );
            }
        }
        DeleteObjectsRequest request = new DeleteObjectsRequest( bucketName );
        request.setKeys( deleteVersion );
        return request;
    }

    private void checkDeleteInfo(
            List< DeleteObjectsResult.DeletedObject > deletedObjects,
            List< String > expDeleteObjectskey, String DeleteMarkerVersionId,
            String version ) {
        List< String > actDeletekey = new ArrayList<>();
        for ( DeleteObjectsResult.DeletedObject obj : deletedObjects ) {
            actDeletekey.add( obj.getKey() );
            Assert.assertEquals( obj.getDeleteMarkerVersionId(),
                    DeleteMarkerVersionId );
            Assert.assertEquals( obj.getVersionId(), version );
        }
        Assert.assertEquals( actDeletekey, expDeleteObjectskey );
    }

    private void checkExistObjectAndVersion( List< String > expExistkey,
            String version, String... deleteVersion ) {
        List< String > actExistkey = new ArrayList<>();
        List< S3ObjectSummary > objectSummaries = s3Client
                .listObjects( bucketName ).getObjectSummaries();
        for ( S3ObjectSummary objectSummary : objectSummaries ) {
            actExistkey.add( objectSummary.getKey() );
            S3Object object = s3Client.getObject( bucketName,
                    objectSummary.getKey() );
            // 校验当前版本
            Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                    version );
            // 校验已删除版本
            for ( int i = 0; i < deleteVersion.length; i++ ) {
                GetObjectRequest getObjectRequest = new GetObjectRequest(
                        bucketName, objectSummary.getKey(),
                        deleteVersion[ i ] );
                try {
                    s3Client.getObject( getObjectRequest );
                    Assert.fail( "except fail but success" );
                } catch ( AmazonS3Exception e ) {
                    Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
                }
            }
        }
        Assert.assertEqualsNoOrder( actExistkey.toArray(),
                expExistkey.toArray() );

    }

    private void checkObjectVersion( String historyVersion ) {
        for ( String key : allObjectKeys ) {
            GetObjectRequest getObjectRequest = new GetObjectRequest(
                    bucketName, key, historyVersion );
            s3Client.getObject( getObjectRequest );
        }
    }
}
