package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4849 :: 开启版本控制，并发更新相同文件属性
 * @author wuyan
 * @Date 2022.07.23
 * @version 1.00
 */
public class ScmFile4849 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4849";
    private String fileName = "scmfile4849";
    private ScmId fileId = null;
    private int fileSize = 1024 *3;
    private int updateSize = 1024 *  2;
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

    @Test
    public void testCreateObject() throws Exception {
        String updateTitle = "updateTitle4859";
        String updateAuthor = "updateAuthor4859";
        int curVersion = 2;
        int hisVersion = 1;
        ThreadExecutor es = new ThreadExecutor();

        UpdateFileAuthorAttr updateFileAuthorAttr = new UpdateFileAuthorAttr( updateAuthor,  hisVersion);
        UpdateFileTitleAttr updateFileTitleAttr = new UpdateFileTitleAttr(updateTitle,curVersion);
        es.addWorker( updateFileAuthorAttr);
        es.addWorker( updateFileTitleAttr );
        es.run();
        checkUpdateResult( updateTitle, updateAuthor);
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

    private class UpdateFileAuthorAttr {
        private String author;
        private int version;

        private UpdateFileAuthorAttr( String author,int version ) {
            this.author = author;
            this.version = version;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = scmBucket.getFile( fileName, version, 0 );
                file.setAuthor(author);
            } finally {
                session.close();
            }
        }
    }

    private class UpdateFileTitleAttr {
        private String title;
        private int version;

        private UpdateFileTitleAttr( String title,int version ) {
            this.title = title;
            this.version = version;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket scmBucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = scmBucket.getFile( fileName, version, 0 );
                file.setTitle( title );
            } finally {
                session.close();
            }
        }
    }



    private void checkUpdateResult( String curVersionTitle, String hisVersionAuthor ) throws Exception {
        int curVersion = 2;
        int hisVersion = 1;
        ScmFile curFile = scmBucket.getFile( fileName );
        Assert.assertEquals( curFile.getFileId(), fileId );
        Assert.assertEquals( curFile.getMajorVersion(), curVersion );
        Assert.assertEquals( curFile.getTitle(), curVersionTitle );
        Assert.assertEquals( curFile.getAuthor(), "" );
        Assert.assertEquals( curFile.getSize(), updateSize );
        S3Utils.checkFileContent( curFile, updatePath, localPath );

        ScmFile hisFile = scmBucket.getFile( fileName, hisVersion, 0 );
        Assert.assertEquals( hisFile.getFileId(), fileId );
        Assert.assertEquals( hisFile.getTitle(), fileName );
        Assert.assertEquals( hisFile.getAuthor(), hisVersionAuthor );
        Assert.assertEquals( hisFile.getSize(), fileSize );
        S3Utils.checkFileContent( hisFile, filePath, localPath );

    }

}
