package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.*;
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
 * @descreption SCM-4879 :: 开启版本控制，并发指定不同版本更新和删除相同文件
 * @author Zhaoyujing
 * @Date 2020/7/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4879 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4879";
    private String keyName = "aa/bb/object4879";
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;

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

        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
        S3Utils.createFile( bucket, keyName, filePath );
        S3Utils.createFile( bucket, keyName, filePath );
        S3Utils.createFile( bucket, keyName, filePath );
    }

    @Test
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new CreateFileThread() );
        te.addWorker( new DeleteVersionThread());
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
        Assert.assertEquals(versionListing.getVersionSummaries().size(), 3);
        Assert.assertEquals(versionListing.getVersionSummaries().get(0).getVersionId(), "4.0");
        Assert.assertEquals(versionListing.getVersionSummaries().get(1).getVersionId(), "3.0");
        Assert.assertEquals(versionListing.getVersionSummaries().get(2).getVersionId(), "2.0");
    }

    private class CreateFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(session,
                        bucketName);
            S3Utils.createFile(bucket, keyName, updatePath);
        }
    }

    private class DeleteVersionThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            s3Client.deleteVersion( bucketName, keyName, "1.0");
        }
    }
}
