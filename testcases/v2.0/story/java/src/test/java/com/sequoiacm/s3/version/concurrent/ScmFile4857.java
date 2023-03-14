package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: SCM-4857 :: 开启版本控制，并发删除和获取同一文件的不同版本
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4857 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4857";
    private String fileName = "scmfile4857";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
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
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = ScmFileUtils.createFile( scmBucket, fileName, filePath );
        ScmFileUtils.createFile( scmBucket, fileName, updatePath );
    }

    @Test
    public void testCreateObject() throws Exception {
        int deleteVersion = 1;
        int getVersion = 2;
        ThreadExecutor es = new ThreadExecutor();
        GetFile updateFile = new GetFile( getVersion );
        DeleteFile deleteFile = new DeleteFile( deleteVersion );
        es.addWorker( updateFile );
        es.addWorker( deleteFile );
        es.run();

        checkDeleteResult( getVersion );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class GetFile extends ResultStore {
        private int getVersion;
        private ScmFile file = null;

        private GetFile( int getVersion ) {
            this.getVersion = getVersion;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            file = scmBucket.getFile( fileName, getVersion, 0 );
        }

        @ExecuteOrder(step = 2)
        private void checkGetFileResult() throws Exception {
            Assert.assertEquals( file.getMajorVersion(), getVersion );
            Assert.assertEquals( file.getFileId(), fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( updatePath ),
                    TestTools.getMD5( downloadPath ) );
        }
    }

    private class DeleteFile {
        private int deletVersion;

        private DeleteFile( int deletVersion ) {
            this.deletVersion = deletVersion;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                scmBucket.deleteFileVersion( fileName, deletVersion, 0 );
            } finally {
                session.close();
            }
        }
    }

    private void checkDeleteResult( int currentVersion ) throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        List< BSONObject > actFileNames = new ArrayList<>();
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            Assert.assertEquals( file.getFileId(), fileId,
                    "---error file version is " + version );
            Assert.assertEquals( file.getFileName(), fileName );
            Assert.assertEquals( version, currentVersion );
            BSONObject fileInfo = new BasicBSONObject();
            fileInfo.put( file.getFileName(), version );
            actFileNames.add( fileInfo );
            size++;
        }
        int fileVersionNum = 1;
        Assert.assertEquals( size, fileVersionNum, actFileNames.toString() );
    }
}
