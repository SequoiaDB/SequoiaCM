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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4842 :: 开启版本控制，并发更新不同文件
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4842 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4842";
    private String keyName = "aa/bb/object4842";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private int objectNum = 10;
    private int versionNum = 2;
    private List< String > keyList = new ArrayList<>();

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
        bucket.enableVersionControl();
    }

    @Test
    public void testCreateBucket() throws Exception {
        for ( int i = 0; i < objectNum; i++ ) {
            String key = keyName + "_" + i;
            createFile( key );
            keyList.add( key );
        }
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < objectNum; i++ ) {
            te.addWorker(
                    new UpdateFileThread( keyList.get( i ) ) );
        }
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
        }
    }

    private void checkFileList() throws Exception {
        int count = 0;
        List< ScmFileBasicInfo > fileList = S3Utils.getVersionList( session, ws,
                bucketName );
        Assert.assertEquals( fileList.size(), objectNum * versionNum );

        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        String fileMD5 = TestTools.getMD5( filePath );
        for ( ScmFileBasicInfo file : fileList ) {
            Assert.assertEquals( file.getMajorVersion(), ( 2 - count % 2 ) );
            Assert.assertEquals( file.getFileName(), keyList.get( count / 2 ) );

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
            Assert.assertEquals( fileMD5, downloadMD5 );

            count++;
        }
    }

    private class UpdateFileThread extends ResultStore {
        String key;

        UpdateFileThread( String key ) {
            this.key = key;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            ScmFile file = bucket.getFile( key );
            file.updateContent( filePath );
        }
    }

    private void createFile( String key ) throws ScmException {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        S3Utils.createFile( bucket, key, filePath );
    }
}
