package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @descreption SCM-4877 :: 开启版本控制，并发创建和删除相同文件
 * @author Zhaoyujing
 * @Date 2020/7/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4877 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4877";
    private String keyName = "aa/bb/object4877";
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
        S3Utils.clearBucket( s3Client, bucketName );
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
    }

    @Test
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new DeleteFileThread() );
        te.addWorker( new PutObjectThread());
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
        Assert.assertEquals(versionListing.getVersionSummaries().size(), 2);
        if ( versionListing.getVersionSummaries().get(0).isDeleteMarker()) {
            Assert.assertFalse(versionListing.getVersionSummaries().get(1).isDeleteMarker());
        }
        if ( ! versionListing.getVersionSummaries().get(0).isDeleteMarker()) {
            Assert.assertTrue(versionListing.getVersionSummaries().get(1).isDeleteMarker());
        }
    }

    private class DeleteFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmSession session= null;
            try {
                session = TestScmTools.createSession(ScmInfo.getSite());
                ScmBucket bucket = ScmFactory.Bucket.getBucket(session,
                        bucketName);
                bucket.deleteFile(keyName, false);
            } finally {
                if (session != null){
                    session.close();
                }
            }
        }
    }

    private class PutObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            s3Client.putObject( bucketName, keyName, "content4877");
        }
    }
}
