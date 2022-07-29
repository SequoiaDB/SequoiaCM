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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: SCM-4852 :: 开启版本控制，并发更新和删除相同文件
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4852 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4852";
    private String fileName = "scmfile4852";
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
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, fileName, filePath );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        UpdateFile updateFile = new UpdateFile( fileName );
        DeleteFile deleteFile = new DeleteFile( fileName );
        es.addWorker( updateFile );
        es.addWorker( deleteFile );
        es.run();

        if ( updateFile.getRetCode() == 0 ) {
            checkUpdateAndDeleteSuccessResult();
        } else {
            // error=FILE_NOT_FOUND(-262)
            Assert.assertEquals( updateFile.getRetCode(), ScmError.FILE_NOT_FOUND.getErrorCode(),
                    updateFile.getThrowable().getMessage() );
            checkUpdateFailedResult();
        }

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

    private class UpdateFile extends ResultStore {
        private String fileName;

        private UpdateFile( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = scmBucket.getFile( fileName );
                file.updateContent( updatePath );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            } finally {
                session.close();
            }
        }
    }

    private class DeleteFile {
        private String fileName;

        private DeleteFile( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                scmBucket.deleteFile( fileName, false );
            } finally {
                session.close();
            }
        }
    }

    private void checkUpdateAndDeleteSuccessResult() throws Exception {
        int curVersion = 3;
        int hisVersionV2 = 2;
        int hisVersionV1 = 1;
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        List< String > actFileNames = new ArrayList<>();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            Assert.assertEquals( file.getFileId(), fileId,
                    "---error file version is " + version );
            Assert.assertEquals( file.getFileName(), fileName );
            if ( file.isDeleteMarker() && version == curVersion ) {
                // 检查删除文件移到历史版本，读取文件内容正确
                ScmFile fileV2 = scmBucket.getFile( fileName, hisVersionV2, 0 );
                S3Utils.checkFileContent( fileV2, updatePath, localPath );
            } else if ( file.isDeleteMarker() && version == hisVersionV2 ) {
                // 删除先执行，更新后执行，则删除标记为V2版本
                // 当前版本为更新文件
                ScmFile fileCur = scmBucket.getFile( fileName, curVersion, 0 );
                S3Utils.checkFileContent( fileCur, updatePath, localPath );
            } else {
                Assert.assertFalse( file.isDeleteMarker(),
                        "---file version = " + version );
            }
            size++;
        }
        cursor.close();
        int versionNum = 3;
        Assert.assertEquals( size, versionNum );
        ScmFile fileCur = scmBucket.getFile( fileName, hisVersionV1, 0 );
        S3Utils.checkFileContent( fileCur, filePath, localPath );
    }

    private void checkUpdateFailedResult() throws Exception {
        int curVersion = 2;
        int hisVersion = 1;
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            int version = file.getMajorVersion();
            Assert.assertEquals( file.getFileId(), fileId,
                    "---error file version is " + version );
            Assert.assertEquals( file.getFileName(), fileName );

            if ( version == curVersion ) {
                Assert.assertTrue( file.isDeleteMarker() );
            } else {
                // 删除文件移到历史文件中
                Assert.assertEquals( file.getMajorVersion(), hisVersion );
                Assert.assertFalse( file.isDeleteMarker(),
                        "---file version =" + version );
                ScmFile fileHis = scmBucket.getFile( fileName, version, 0 );
                S3Utils.checkFileContent( fileHis, filePath, localPath );
            }
            size++;
        }
        cursor.close();
        int versionNum = 2;
        Assert.assertEquals( size, versionNum );
    }
}
