package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4687 :: 使用deleteObjects接口，开启版本控制删除对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4687 extends TestScmBase {
    private String bucketName = "bucket4687";
    private String keyName = "object4687";
    private String fileContent = "content4687";
    private int fileNum = 10;
    private int versionNum = 5;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private List< String > allObjectKeys = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        for ( int i = 0; i < fileNum; i++ ) {
            allObjectKeys.add( keyName + i );
        }
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        String deleteMarkerVersion;
        String deleteVersion;
        String historyVersion;
        String LastestVersion;
        List< DeleteObjectsResult.DeletedObject > deletedObjects;

        // 不指定版本号删除对象
        putObjects();
        deleteMarkerVersion = "6.0";
        deleteVersion = null;
        historyVersion = "5.0";
        deletedObjects = s3Client
                .deleteObjects( getDeleteRequest( deleteVersion ) )
                .getDeletedObjects();
        checkDeleteInfo( deletedObjects, allObjectKeys, deleteMarkerVersion,
                deleteVersion );
        checkExistObjectAndVersion( new ArrayList< String >(), null, null );
        checkObjectVersion( historyVersion );

        // 删除版本为"5.0"的对象
        S3Utils.deleteAllObjectVersions( s3Client, bucketName );
        putObjects();
        deleteVersion = "5.0";
        LastestVersion = "4.0";
        deleteMarkerVersion = null;
        deletedObjects = s3Client
                .deleteObjects( getDeleteRequest( deleteVersion ) )
                .getDeletedObjects();
        checkDeleteInfo( deletedObjects, allObjectKeys, deleteMarkerVersion,
                deleteVersion );
        checkExistObjectAndVersion( allObjectKeys, LastestVersion,
                deleteVersion );
        // 删除版本为"1.0"的对象
        deleteVersion = "1.0";
        LastestVersion = "4.0";
        deleteMarkerVersion = null;
        deletedObjects = s3Client
                .deleteObjects( getDeleteRequest( deleteVersion ) )
                .getDeletedObjects();
        checkDeleteInfo( deletedObjects, allObjectKeys, deleteMarkerVersion,
                deleteVersion );
        checkExistObjectAndVersion( allObjectKeys, LastestVersion,
                deleteVersion );
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

    private void putObjects() {
        for ( int j = 0; j < versionNum; j++ ) {
            for ( int i = 0; i < fileNum; i++ ) {
                s3Client.putObject( bucketName, keyName + i, fileContent );
            }
        }
    }

    private DeleteObjectsRequest getDeleteRequest( String version ) {
        List< DeleteObjectsRequest.KeyVersion > deleteVersion = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            if ( version == null ) {
                deleteVersion.add(
                        new DeleteObjectsRequest.KeyVersion( keyName + i ) );
            } else {
                deleteVersion.add( new DeleteObjectsRequest.KeyVersion(
                        keyName + i, version ) );
            }
        }
        DeleteObjectsRequest request = new DeleteObjectsRequest( bucketName );
        request.setKeys( deleteVersion );
        return request;
    }

    private void checkDeleteInfo(
            List< DeleteObjectsResult.DeletedObject > deletedObjects,
            List< String > expDeleteObjectKeys, String DeleteMarkerVersionId,
            String version ) {
        List< String > actDeletekey = new ArrayList<>();
        for ( DeleteObjectsResult.DeletedObject obj : deletedObjects ) {
            actDeletekey.add( obj.getKey() );
            Assert.assertEquals( obj.getDeleteMarkerVersionId(),
                    DeleteMarkerVersionId );
            Assert.assertEquals( obj.getVersionId(), version );
        }
        Assert.assertEquals( actDeletekey, expDeleteObjectKeys );
    }

    private void checkExistObjectAndVersion( List< String > expExistKey,
            String version, String deleteVersion ) {
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
            GetObjectRequest getObjectRequest = new GetObjectRequest(
                    bucketName, objectSummary.getKey(), deleteVersion );
            try {
                s3Client.getObject( getObjectRequest );
                Assert.fail( "except fail but success" );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
            }
        }

        Assert.assertEqualsNoOrder( actExistkey.toArray(),
                expExistKey.toArray() );

    }

    private void checkObjectVersion( String historyVersion ) {
        for ( String key : allObjectKeys ) {
            GetObjectRequest getObjectRequest = new GetObjectRequest(
                    bucketName, key, historyVersion );
            s3Client.getObject( getObjectRequest );
        }
    }
}
