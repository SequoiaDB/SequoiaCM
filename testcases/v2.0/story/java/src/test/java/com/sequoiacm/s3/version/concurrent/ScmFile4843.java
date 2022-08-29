package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @descreption SCM-4843 :: 开启版本控制，并发获取相同文件
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4843 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4843";
    private String keyName = "aa/bb/object4843";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();
    private int versionNum = 4;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < versionNum; i++ ) {
            String filePath = localPath + File.separator + "localFile_" + i
                    + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize );
            filePathList.add( filePath );
        }

        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
        for ( int i = 0; i < versionNum; i++ ) {
            S3Utils.createFile( bucket, keyName, filePathList.get( i ) );
        }
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < versionNum; i++ ) {
            te.addWorker( new GetFileThread() );
        }

        for ( int i = 0; i < versionNum; i++ ) {
            te.addWorker( new GetFileThread( versionNum / 2 ) );
        }

        for ( int i = 0; i < versionNum; i++ ) {
            te.addWorker( new GetFileThread( i + 1 ) );
        }

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

    private class GetFileThread extends ResultStore {
        Integer majorVersion = null;

        GetFileThread() {
        }

        GetFileThread( int majorVersion ) {
            this.majorVersion = majorVersion;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            ScmFile scmFile;
            if ( null == majorVersion ) {
                scmFile = bucket.getFile( keyName );
                majorVersion = versionNum;
            } else {
                scmFile = bucket.getFile( keyName, majorVersion, 0 );
            }
            Assert.assertEquals( scmFile.getMajorVersion(),
                    majorVersion.intValue() );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            OutputStream fileOutputStream = new FileOutputStream(
                    downloadPath );
            scmFile.getContent( fileOutputStream );
            fileOutputStream.close();

            String downloadMD5 = TestTools.getMD5( downloadPath );
            String fileMD5 = TestTools
                    .getMD5( filePathList.get( majorVersion - 1 ) );
            Assert.assertEquals( fileMD5, downloadMD5 );
        }
    }
}
