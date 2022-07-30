package com.sequoiacm.s3.version.concurrent;

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
 * @descreption SCM-4860 :: 禁用版本控制，并发删除和获取文件列表
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4860 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4860";
    private String keyName = "aa/bb/object4860";
    private int fileSize = 1024 * 1024;
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

        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.suspendVersionControl();
        S3Utils.createFile(bucket, keyName, filePath);
    }

    @Test
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new DeleteFileThread() );
        te.addWorker( new ListFilesThread() );
        te.run();

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
        }
    }

    private class DeleteFileThread extends ResultStore {

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            bucket.deleteFile( keyName, false );
        }
    }

    private class ListFilesThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            List< ScmFileBasicInfo > fileList = S3Utils.getVersionList(session, ws, bucketName);
            Assert.assertEquals( fileList.size(), 1 );
        }
    }
}
