package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4859 :: 开启版本控制，并发更新属性和更新版本文件
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4859 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4859";
    private String fileName = "scmfile4859";
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
    }

    @Test
    public void testCreateObject() throws Exception {
        String updateTitle = "updateTitle4859";
        ThreadExecutor es = new ThreadExecutor();
        UpdateFile updateFile = new UpdateFile();
        UpdateFileAttr deleteFile = new UpdateFileAttr( updateTitle );
        es.addWorker( updateFile );
        es.addWorker( deleteFile );
        es.run();
        checkUpdateResult( updateTitle );
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

    private class UpdateFile {

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFileUtils.createFile( scmBucket, fileName, updatePath );
            } finally {
                session.close();
            }
        }
    }

    private class UpdateFileAttr {
        private String title;
        private int version = 1;

        private UpdateFileAttr( String title ) {
            this.title = title;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = scmBucket.getFile( fileName, version, 0 );
                file.setTitle( title );
            } finally {
                session.close();
            }
        }
    }

    private void checkUpdateResult( String title ) throws Exception {
        int curVersion = 2;
        int hisVersion = 1;
        ScmFile curFile = scmBucket.getFile( fileName );
        Assert.assertEquals( curFile.getFileId(), fileId );
        Assert.assertEquals( curFile.getMajorVersion(), curVersion );
        Assert.assertEquals( curFile.getTitle(), fileName );
        Assert.assertEquals( curFile.getSize(), updateSize );
        S3Utils.checkFileContent( curFile, updatePath, localPath );

        ScmFile hisFile = scmBucket.getFile( fileName, hisVersion, 0 );
        Assert.assertEquals( hisFile.getFileId(), fileId );
        Assert.assertEquals( hisFile.getTitle(), title );
        Assert.assertEquals( hisFile.getSize(), fileSize );
        S3Utils.checkFileContent( hisFile, filePath, localPath );

    }

}
