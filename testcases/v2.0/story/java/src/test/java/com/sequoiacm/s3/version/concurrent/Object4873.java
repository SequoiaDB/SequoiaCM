package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Iterator;

/**
 * @Description: SCM-4873 ::开启版本控制，并发删除相同文件
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Object4873 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4873";
    private String keyName = "key4873";
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;

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

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        S3Utils.createFile( scmBucket, keyName, filePath );
        S3Utils.createFile( scmBucket, keyName, updatePath );
        s3Client = S3Utils.buildS3Client();

    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        S3DeleteObject s3DeleteObject = new S3DeleteObject( keyName );
        SCMDeleteObject scmDeleteObject = new SCMDeleteObject( keyName );
        es.addWorker( s3DeleteObject );
        es.addWorker( scmDeleteObject );
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
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class S3DeleteObject {
        private String keyName;

        private S3DeleteObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            s3Client.deleteObject( bucketName, keyName );
        }
    }

    private class SCMDeleteObject {
        private String keyName;

        private SCMDeleteObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            scmBucket.deleteFile( keyName, false );
        }
    }

    private void checkDeleteResult() throws Exception {
        String currentVersion = "4.0";
        String historyV3 = "3.0";
        String historyV2 = "2.0";
        String historyV1 = "1.0";
        int versionNum = 4;
        int size = 0;
        VersionListing verList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        while ( true ) {
            Iterator< S3VersionSummary > versionIter = verList
                    .getVersionSummaries().iterator();
            while ( versionIter.hasNext() ) {
                S3VersionSummary vs = versionIter.next();
                Assert.assertEquals( vs.getBucketName(), bucketName,
                        "bucketName is wrong!" );
                Assert.assertEquals( vs.getKey(), keyName,
                        "keyName is wrong!" );
                String version = vs.getVersionId();
                if ( version.equals( currentVersion )
                        || version.equals( historyV3 ) ) {
                    Assert.assertTrue( vs.isDeleteMarker() );
                    Assert.assertEquals( vs.getSize(), 0 );
                }
                if ( version.equals( historyV1 ) ) {
                    Assert.assertFalse( vs.isDeleteMarker() );
                    Assert.assertEquals( vs.getBucketName(), bucketName );
                    Assert.assertEquals( vs.getETag(),
                            TestTools.getMD5( filePath ) );
                    Assert.assertEquals( vs.getSize(), fileSize );
                }
                if ( version.equals( historyV2 ) ) {
                    Assert.assertFalse( vs.isDeleteMarker() );
                    Assert.assertEquals( vs.getETag(),
                            TestTools.getMD5( updatePath ) );
                    Assert.assertEquals( vs.getSize(), updateSize );
                    // 删除版本文件还存在历史版本中
                    String downfileMd5 = S3Utils.getMd5OfObject( s3Client,
                            localPath, bucketName, keyName, version );
                    Assert.assertEquals( downfileMd5,
                            TestTools.getMD5( updatePath ) );
                }
                size++;
            }
            if ( verList.isTruncated() ) {
                verList = s3Client.listNextBatchOfVersions( verList );
            } else {
                break;
            }
        }
        Assert.assertEquals( versionNum, size );
    }
}
