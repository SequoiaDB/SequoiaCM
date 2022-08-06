package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
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
 * @Description: SCM-4856 :: 开启版本控制，并发删除和获取相同文件（指定相同版本）
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4856 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4856";
    private String fileName = "scmfile4856";
    private ScmId fileId = null;
    private int fileSize = 1024 * 2;
    private int updateSize = 1024 * 1024;
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
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, fileName, filePath );
        S3Utils.createFile( scmBucket, fileName, updatePath );
    }

    //http://jira.web:8080/browse/SEQUOIACM-1014
    @Test(enabled = false)
    public void testCreateObject() throws Exception {
        int version = 2;
        ThreadExecutor es = new ThreadExecutor();
        DeleteFile deleteFile = new DeleteFile( version );
        GetFile GetFile = new GetFile( version );
        es.addWorker( deleteFile );
        es.addWorker( GetFile );
        es.run();

        checkDeleteResult();
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

    private class DeleteFile extends ResultStore {
        private int deleteVersion;

        private DeleteFile( int deleteVersion ) {
            this.deleteVersion = deleteVersion;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session1 = null;
            try {
                session1 = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session1,
                        bucketName );
                scmBucket.deleteFileVersion( fileName, deleteVersion, 0 );
            } finally {
                session1.close();
            }
        }
    }

    private class GetFile {
        private ScmFile file = null;
        private int getVersion;

        private GetFile( int getVersion ) {
            this.getVersion = getVersion;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            try {
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                file = scmBucket.getFile( fileName, getVersion, 0 );

                Assert.assertEquals( file.getMajorVersion(), getVersion );
                Assert.assertEquals( file.getFileId(), fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                file.getContent( downloadPath );
                Assert.assertEquals( TestTools.getMD5( updatePath ),
                        TestTools.getMD5( downloadPath ) );
            } catch ( ScmException e ) {
                // -262:"FILE_NOT_FOUND"
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
        }
    }

    private void checkDeleteResult() throws Exception {
        int currentVersion = 1;
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
