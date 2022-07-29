package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
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
 * @Description: SCM-4853 :: 开启版本控制，并发指定不同版本更新和删除相同文件
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4853 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4853";
    private String fileName = "scmfile4853";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;
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

    @Test
    public void testCreateObject() throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ThreadExecutor es = new ThreadExecutor();
        UpdateFile updateFile = new UpdateFile( fileName );
        DeleteFile deleteFile = new DeleteFile( fileName, historyVersion );
        es.addWorker( updateFile );
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
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private class UpdateFile {
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
                file.updateContent( filePath );
            } finally {
                session.close();
            }
        }
    }

    private class DeleteFile {
        private String fileName;
        private int deleteVersion;

        private DeleteFile( String fileName, int deleteVersion ) {
            this.fileName = fileName;
            this.deleteVersion = deleteVersion;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                scmBucket.deleteFileVersion( fileName, deleteVersion, 0 );
            } finally {
                session.close();
            }
        }
    }

    private void checkResult() throws Exception {
        int curVersion = 3;
        int hisVersionV2 = 2;
        int hisVersionV1 = 1;
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
                ScmFile fileCur = scmBucket.getFile( fileName );
                S3Utils.checkFileContent( fileCur, filePath, localPath );
            } else {
                // v2版本文件移到历史文件中
                Assert.assertEquals( file.getMajorVersion(), hisVersionV2 );
                Assert.assertFalse( file.isDeleteMarker(),
                        "---file version =" + version );
                ScmFile fileHis = scmBucket.getFile( fileName, version, 0 );
                S3Utils.checkFileContent( fileHis, updatePath, localPath );
            }

            // 指定V1版本已不存在
            Assert.assertNotEquals( version, hisVersionV1 );
            size++;
        }
        cursor.close();
        int versionNum = 2;
        Assert.assertEquals( size, versionNum );
    }
}
