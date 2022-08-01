package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4851 ::开启版本控制，并发创建和删除相同文件
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4851 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4851";
    private String fileName = "scmfile4851";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
    private File localPath = null;
    private String filePath = null;
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
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
    }

    // SEQUOIACM-1006
    @Test(enabled = false)
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        CreateFile createFile = new CreateFile( fileName );
        DeleteFile deleteFile = new DeleteFile( fileName );
        es.addWorker( createFile );
        es.addWorker( deleteFile );
        es.run();

        checkResult();
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

    private class CreateFile {
        private String fileName;

        private CreateFile( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws ScmException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                fileId = S3Utils.createFile( scmBucket, fileName, filePath );
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
        private void exec() throws ScmException {
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

    private void checkResult() throws Exception {
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
            if ( file.isDeleteMarker() ) {
                if ( version == curVersion ) {
                    // 检查删除文件移到历史版本，读取文件内容正确
                    ScmFile file1 = scmBucket.getFile( fileName, hisVersion,
                            0 );
                    S3Utils.checkFileContent( file1, filePath, localPath );
                } else {
                    // 文件存在当前版本，历史版本为删除标记
                    Assert.assertEquals( file.getMajorVersion(), hisVersion );
                    ScmFile file2 = scmBucket.getFile( fileName, curVersion,
                            0 );
                    S3Utils.checkFileContent( file2, filePath, localPath );
                }
            } else {
                if ( version == curVersion ) {
                    // 文件存在当前版本，历史版本为删除标记
                    ScmFile file2 = scmBucket.getFile( fileName, curVersion,
                            0 );
                    S3Utils.checkFileContent( file2, filePath, localPath );

                } else {
                    // 检查删除文件移到历史版本，读取文件内容正确
                    Assert.assertEquals( file.getMajorVersion(), hisVersion );
                    ScmFile file1 = scmBucket.getFile( fileName, hisVersion,
                            0 );
                    S3Utils.checkFileContent( file1, filePath, localPath );
                }
            }
            size++;
        }
        cursor.close();
        int versionNum = 2;
        Assert.assertEquals( size, versionNum );
    }
}
