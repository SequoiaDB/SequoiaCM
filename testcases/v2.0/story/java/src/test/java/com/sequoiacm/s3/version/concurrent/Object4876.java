package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4876 :: 开启版本控制，并发使用物理方式和非物理方式删除相同文件
 * @author Zhaoyujing
 * @Date 2020/7/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4876 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4876";
    private String keyName = "aa/bb/object4876";
    private int fileSize = 1024 * 10;
    private File localPath = null;
    private String filePath = null;

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

        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
        ScmFileUtils.createFile( bucket, keyName, filePath );
        ScmFileUtils.createFile( bucket, keyName, filePath );
    }

    @Test
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new DeleteFileThread() );
        te.addWorker( new DeleteObjectThread());
        te.run();

        checkFileList();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {

                session.close();
            }

            if (s3Client != null) {
                s3Client.shutdown();
            }
        }
    }

    private void checkFileList() {
        VersionListing versionListing = s3Client.listVersions(bucketName, null);
        if (versionListing.getVersionSummaries().size() > 0) {
            Assert.assertEquals(versionListing.getVersionSummaries().size(), 1);
            Assert.assertTrue(versionListing.getVersionSummaries().get(0).isDeleteMarker());
        }
    }

    private class DeleteFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmSession session= null;
            try {
                session = ScmSessionUtils.createSession(ScmInfo.getSite());
                ScmBucket bucket = ScmFactory.Bucket.getBucket(session,
                        bucketName);
                bucket.deleteFile(keyName, true);
            } finally {
                if (session != null){
                    session.close();
                }
            }
        }
    }

    private class DeleteObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
        }
    }
}
