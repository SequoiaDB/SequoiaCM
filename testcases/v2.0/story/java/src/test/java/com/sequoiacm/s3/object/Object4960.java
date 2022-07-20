package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @descreption SCM-4960:使用deleteObjects接口，关闭版本控制删除对象
 * @author YiPan
 * @date 2022/7/19
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4960 extends TestScmBase {
    private final String bucketName = "bucket4960";
    private final String baseObjectKey = "object4960";
    private AmazonS3 s3Client;
    private String content = "test4960";
    private List< String > expDeletekey;
    private List< String > expExistkey;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        // 清理环境
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        expDeletekey = new ArrayList<>();
        expExistkey = new ArrayList<>();
        // 预期被删除对象的key
        for ( int i = 0; i < 10; i++ ) {
            expDeletekey.add( baseObjectKey + i );
        }
        // 预期剩余对象的key
        for ( int i = 10; i < 20; i++ ) {
            expExistkey.add( baseObjectKey + i );
        }

    }

    @Test
    public void test() throws Exception {
        // 生成删除当前版本的deleteObjectsRequest
        DeleteObjectsRequest deleteRequest = getDeleteRequest( null );

        // 新建桶未进行任何版本控制操作，不指定版本号删除对象
        putObject();
        List< DeleteObjectsResult.DeletedObject > deletedObjects = s3Client
                .deleteObjects( deleteRequest ).getDeletedObjects();
        String deleteMarkerVersion = null;
        checkDeleteResult( deletedObjects, deleteMarkerVersion, null );
        checkExistKeys( expExistkey );

        // 禁用版本控制,不指定版本号删除对象
        S3Utils.updateBucketVersionConfig( s3Client, bucketName,
                BucketVersioningConfiguration.SUSPENDED );
        putObject();
        deletedObjects = s3Client.deleteObjects( deleteRequest )
                .getDeletedObjects();
        deleteMarkerVersion = "null";
        checkDeleteResult( deletedObjects, deleteMarkerVersion, null );
        checkExistKeys( expExistkey );
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

    private void putObject() {
        for ( int i = 0; i < 20; i++ ) {
            s3Client.putObject( bucketName, baseObjectKey + i, content );
        }
    }

    private DeleteObjectsRequest getDeleteRequest( String version ) {
        List< DeleteObjectsRequest.KeyVersion > deleteKeyVersion = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            if ( version == null ) {
                deleteKeyVersion.add( new DeleteObjectsRequest.KeyVersion(
                        baseObjectKey + i ) );
            } else {
                deleteKeyVersion.add( new DeleteObjectsRequest.KeyVersion(
                        baseObjectKey + i, version ) );
            }
        }
        DeleteObjectsRequest request = new DeleteObjectsRequest( bucketName );
        request.setKeys( deleteKeyVersion );
        return request;
    }

    private void checkDeleteResult(
            List< DeleteObjectsResult.DeletedObject > deletedObjects,
            String DeleteMarkerVersionId, String version ) {
        List< String > actDeletekey = new ArrayList<>();
        for ( DeleteObjectsResult.DeletedObject obj : deletedObjects ) {
            actDeletekey.add( obj.getKey() );
            Assert.assertEquals( obj.getDeleteMarkerVersionId(),
                    DeleteMarkerVersionId );
            Assert.assertEquals( obj.getVersionId(), version );
        }
        Assert.assertEqualsNoOrder( actDeletekey.toArray(),
                expDeletekey.toArray() );
    }

    private void checkExistKeys( List< String > expExistkey ) {
        List< String > actExistkey = new ArrayList<>();
        List< S3ObjectSummary > objectSummaries = s3Client
                .listObjects( bucketName ).getObjectSummaries();
        for ( S3ObjectSummary s : objectSummaries ) {
            actExistkey.add( s.getKey() );
        }
        Assert.assertEqualsNoOrder( actExistkey.toArray(),
                expExistkey.toArray() );
    }
}
