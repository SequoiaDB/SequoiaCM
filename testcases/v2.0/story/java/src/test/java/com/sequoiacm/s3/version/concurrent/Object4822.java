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
import java.util.*;

/**
 * @Description: SCM-4822 :: 开启版本控制，并发删除和获取对象列表
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Object4822 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4822";
    private String keyName = "对象%key4822";
    private int fileSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private int objectNums = 5;
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning(s3Client, bucketName, "Enabled");
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();

        List< String > expKeys = new ArrayList<>();
        for ( int i = 0; i < objectNums; i++ ) {
            String key = keyName + "_" + i;
            s3Client.putObject( bucketName, key, new File( filePath ) );
            expKeys.add( key );
            DeleteObject deleteObject = new DeleteObject( key );
            es.addWorker( deleteObject );
            expKeys.add( key );
        }
        ListObjectVersons listObjectVersons = new ListObjectVersons();
        es.addWorker( listObjectVersons );
        es.run();
        listObjectsAndCheckResult( expKeys );
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

    private class ListObjectVersons {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            System.out.println( "---begin to listVersion=" + new Date() );
            try {
                VersionListing verList = s3Client
                        .listVersions( new ListVersionsRequest()
                                .withBucketName( bucketName ) );
                List< S3VersionSummary > objectVersionList = verList
                        .getVersionSummaries();
                for ( S3VersionSummary obj : objectVersionList ) {
                    Assert.assertEquals( obj.getBucketName(), bucketName,
                            "bucketName is wrong!" );
                    if ( obj.isDeleteMarker() ) {
                        Assert.assertEquals( obj.getVersionId(), "2.0",
                                obj.getKey() );
                    } else {
                        Assert.assertEquals( obj.getVersionId(), "1.0",
                                obj.getKey() );
                        Assert.assertEquals( obj.getETag(),
                                TestTools.getMD5( filePath ) );
                    }
                }
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }

    private void listObjectsAndCheckResult( List< String > expKeys )
            throws Exception {
        List< String > actVersionKeys = new ArrayList<>();
        String deleteTagVersion = "2.0";
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        while ( true ) {
            Iterator< S3VersionSummary > versionIter = versionList
                    .getVersionSummaries().iterator();

            while ( versionIter.hasNext() ) {
                S3VersionSummary vs = versionIter.next();
                String actKey = vs.getKey();
                String versionId = vs.getVersionId();
                if ( versionId.equals( deleteTagVersion ) ) {
                    // 当前版本文件为删除标记
                    Assert.assertTrue( vs.isDeleteMarker(),
                            "the key must be deleteTag!" );
                } else {
                    // 历史版本文件为原删除版本文件
                    checkDeleteObjectResult( bucketName, actKey, versionId );
                    Assert.assertFalse( vs.isDeleteMarker() );
                }
                actVersionKeys.add( actKey );
            }
            if ( versionList.isTruncated() ) {
                versionList = s3Client.listNextBatchOfVersions( versionList );
            } else {
                break;
            }
            Collections.sort( actVersionKeys );
            Collections.sort( expKeys );
            Assert.assertEquals( actVersionKeys, expKeys );
        }
    }

    private void checkDeleteObjectResult( String bucketName, String key,
            String versionId ) throws Exception {
        // 当前版本为删除标记
        boolean isExistObject = s3Client.doesObjectExist( bucketName, key );
        Assert.assertFalse( isExistObject, "the object should not exist!" );

        // 删除版本文件还存在历史版本中
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, key, versionId );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( filePath ) );
    }

}
