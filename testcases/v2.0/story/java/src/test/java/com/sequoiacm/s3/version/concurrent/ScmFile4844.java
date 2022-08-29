package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
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
 * @descreption SCM-4844 :: 开启版本控制，并发列取文件版本列表
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4844 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4844";
    private String keyName = "aa/bb/object4844";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private List< String > filePathList = new ArrayList<>();
    private List< String > md5List = new ArrayList<>();
    private List< String > keyList = new ArrayList<>();
    private int objectNum = 10;
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
            md5List.add( TestTools.getMD5( filePath ) );
        }

        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
        for ( int j = 0; j < objectNum; j++ ) {
            String key = keyName + "_" + j;
            for ( int i = 0; i < versionNum; i++ ) {
                S3Utils.createFile( bucket, key, filePathList.get( i ) );
            }
            keyList.add( key );
        }
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < versionNum; i++ ) {
            te.addWorker( new ListFilesThread() );
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

    private class ListFilesThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            List< ScmFileBasicInfo > fileList = S3Utils.getVersionList( session,
                    ws, bucketName );
            Assert.assertEquals( fileList.size(), objectNum * versionNum );

            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            int count = 0;
            for ( ScmFileBasicInfo file : fileList ) {
                Assert.assertEquals( file.getMajorVersion(),
                        ( versionNum - count % versionNum ) );
                Assert.assertEquals( file.getFileName(),
                        keyList.get( count / versionNum ) );

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
                String fileMD5 = md5List.get( scmFile.getMajorVersion() - 1 ) ;
                Assert.assertEquals( fileMD5, downloadMD5 );

                count++;
            }
        }
    }
}
