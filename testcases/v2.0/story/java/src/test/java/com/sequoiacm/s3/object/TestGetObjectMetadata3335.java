package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Descreption SCM-3335:查询桶中对象元数据列表
 * @Author YiPan
 * @Date 2021/3/10
 */
public class TestGetObjectMetadata3335 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3335";
    private String objectName = "object3335";
    private int objectnum = 10;
    private int fileSize = 1024 * 10;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws IOException {
        List< String > keyList = putObjects();

        List< String > queryKeyList = new ArrayList<>();
        ListObjectsV2Result result = s3Client.listObjectsV2( bucketName );
        List< S3ObjectSummary > objects = result.getObjectSummaries();
        Assert.assertEquals( objects.size(), objectnum );
        // 元数据校验
        for ( S3ObjectSummary os : objects ) {
            String key = os.getKey();
            String etag = os.getETag();
            long size = os.getSize();
            queryKeyList.add( key );
            // check the etag and size
            Assert.assertEquals( etag, TestTools.getMD5( filePath ) );
            Assert.assertEquals( size, fileSize );
        }
        Collections.sort( keyList );
        Collections.sort( queryKeyList );
        Assert.assertEquals( queryKeyList, keyList );
    }

    @AfterClass
    private void tearDown() {
        try {
            S3Utils.clearBucket( s3Client, bucketName );
        } finally {
            s3Client.shutdown();
        }
    }

    private List< String > putObjects() {
        List< String > keyList = new ArrayList<>();
        for ( int i = 0; i < objectnum; i++ ) {
            String keyName = objectName + "_" + i
                    + TestTools.getRandomString( i );
            keyList.add( keyName );
            s3Client.putObject( bucketName, keyName, new File( filePath ) );
        }
        return keyList;
    }
}
