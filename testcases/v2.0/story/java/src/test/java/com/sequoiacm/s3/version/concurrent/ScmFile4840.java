package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @descreption SCM-4840 :: 开启版本控制，并发创建相同文件
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4840 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4840";
    private String keyName = "aa/bb/object4840";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private int versionNums = 10;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket(session, s3WorkSpaces, bucketName);
        ScmBucket bucket = ScmFactory.Bucket.createBucket(ws, bucketName);
        bucket.enableVersionControl();
    }

    //SEQUOIACM-1025暂时屏蔽
    @Test(enabled = false)
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < versionNums; i++ ) {
            te.addWorker( new CreateFileThread() );
        }
        te.run();

        checkFileList();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket(session, s3WorkSpaces, bucketName);
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileList() throws Exception {
        List<ScmFileBasicInfo> fileList = S3Utils.getVersionList(session, ws, bucketName);

        Assert.assertEquals( fileList.size(), versionNums );
        int count  = versionNums;
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        for ( ScmFileBasicInfo file : fileList ) {
            Assert.assertEquals( file.getMajorVersion(), count );
            Assert.assertEquals( file.getFileName(), keyName);

            ScmFile scmFile = bucket.getFile( file.getFileName(),
                    file.getMajorVersion(), file.getMinorVersion() );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            OutputStream fileOutputStream = new FileOutputStream(
                    downloadPath );
            scmFile.getContent( fileOutputStream );
            fileOutputStream.close();

            String downloadMD5 = TestTools.getMD5( downloadPath );
            String fileMD5 = TestTools.getMD5( filePath );
            Assert.assertEquals( fileMD5, downloadMD5 );

            count--;
        }
    }

    private class CreateFileThread extends ResultStore {
        
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket(session, bucketName);
            ScmFile file = bucket.createFile( keyName );
            file.setContent( filePath );
            file.setAuthor( "author4840" );
            
            file.save();
        }
    }
}
