package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: SCM-4819 :: 并发删除和获取相同对象
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Object4819 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4819";
    private String keyName = "对象%key4819";
    private List< String > keyNameList = new ArrayList<>();
    private int fileSize = 1024 * 1024 * 2;
    private int updateSize = 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning(s3Client, bucketName, "Enabled");
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        DeleteObject deleteObject = new DeleteObject( keyName );
        GetObject getObject = new GetObject( keyName );
        es.addWorker( deleteObject );
        es.addWorker( getObject );
        es.run();

        checkDeleteResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private class DeleteObject {
        private String keyName;

        private DeleteObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteObject( bucketName, keyName );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private class GetObject {
        String keyName;

        private GetObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                S3Object object = s3Client.getObject( bucketName, keyName );
                checkGetObjectResult( object, bucketName, keyName );
            } catch ( AmazonS3Exception e ) {
                if ( !e.getErrorCode().equals( "NoSuchKey" ) ) {
                    throw e;
                }
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private void checkGetObjectResult( S3Object object, String bucketName,
            String key ) throws Exception {
        ObjectMetadata metadata = object.getObjectMetadata();
        String versionId = metadata.getVersionId();
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key, versionId );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

    private void checkDeleteResult() throws IOException {
        String currentVersion = "2.0";
        String historyVersion = "1.0";
        int versionNum = 2;
        VersionListing verList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        List< S3VersionSummary > objectVersionList = verList
                .getVersionSummaries();
        Assert.assertEquals( objectVersionList.size(), versionNum );
        for ( S3VersionSummary obj : objectVersionList ) {
            Assert.assertEquals( obj.getBucketName(), bucketName,
                    "bucketName is wrong!" );
            Assert.assertEquals( obj.getKey(), keyName, "keyName is wrong!" );
            if ( obj.isDeleteMarker() ) {
                Assert.assertEquals( obj.getVersionId(), currentVersion );
            } else {
                Assert.assertEquals( obj.getVersionId(), historyVersion );
                Assert.assertEquals( obj.getETag(),
                        TestTools.getMD5( filePath ) );
            }
        }
    }
}
