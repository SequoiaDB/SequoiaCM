package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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
 * @descreption SCM-4686 :: 使用deleteObjects接口，关闭版本控制删除对象
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4686 extends TestScmBase {
    private String bucketName = "bucket4686";
    private String keyName = "object4686";
    private String fileContent = "content4686";
    private int fileNum = 20;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private List< String > expDeleteKey = new ArrayList<>();
    private List< String > expExistKey = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        // 预期被删除对象的key
        for ( int i = 0; i < fileNum / 2; i++ ) {
            expDeleteKey.add( keyName + i );
        }
        // 预期剩余对象的key
        for ( int i = fileNum / 2; i < fileNum; i++ ) {
            expExistKey.add( keyName + i );
        }
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 新建桶 未进行任何版本控制操作，不指定版本号删除对象
        String deleteMarkerVersion = null;
        String deleteVersion = null;
        // 上传 20 个对象
        putObjects();
        List< DeleteObjectsResult.DeletedObject > deletedObjects;
        deletedObjects = s3Client.deleteObjects( getDeleteRequest() )
                .getDeletedObjects();
        checkDelete( deletedObjects, deleteMarkerVersion, deleteVersion );

        // 关闭版本控制,不指定版本号删除对象
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        // 上传 20 个对象, 覆盖同名对象
        putObjects();
        deleteVersion = null;
        deleteMarkerVersion = "null";
        deletedObjects = s3Client.deleteObjects( getDeleteRequest() )
                .getDeletedObjects();
        checkDelete( deletedObjects, deleteMarkerVersion, deleteVersion );
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
        for ( int i = 0; i < fileNum; i++ ) {
            s3Client.putObject( bucketName, keyName + i, fileContent );
        }
    }

    private DeleteObjectsRequest getDeleteRequest() {
        List< DeleteObjectsRequest.KeyVersion > deleteKeyVersion = new ArrayList<>();
        for ( int i = 0; i < fileNum / 2; i++ ) {
            deleteKeyVersion
                    .add( new DeleteObjectsRequest.KeyVersion( keyName + i ) );
        }
        DeleteObjectsRequest request = new DeleteObjectsRequest( bucketName )
                .withKeys( deleteKeyVersion );
        return request;
    }

    private void checkDelete(
            List< DeleteObjectsResult.DeletedObject > deletedObjects,
            String DeleteMarkerVersionId, String version ) {
        List< String > actDeleteKey = new ArrayList<>();
        List< String > actExistKey = new ArrayList<>();
        for ( DeleteObjectsResult.DeletedObject obj : deletedObjects ) {
            actDeleteKey.add( obj.getKey() );
            Assert.assertEquals( obj.getDeleteMarkerVersionId(),
                    DeleteMarkerVersionId );
            Assert.assertEquals( obj.getVersionId(), version );
        }
        List< S3ObjectSummary > objectSummaries = s3Client
                .listObjects( bucketName ).getObjectSummaries();
        for ( S3ObjectSummary s : objectSummaries ) {
            actExistKey.add( s.getKey() );
        }
        Assert.assertEquals( actDeleteKey, expDeleteKey );
        Assert.assertEquals( actExistKey, expExistKey );
    }
}
