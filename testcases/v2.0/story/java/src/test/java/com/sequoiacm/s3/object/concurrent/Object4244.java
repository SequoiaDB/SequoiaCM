package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4244 :: 并发解除文件与桶的关联
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4244 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4243";
    private String key = "aa/bb/object4243";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
    }

    @Test
    public void test() throws Exception {
        // scm create file
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
        ScmId fileId = createScmFile( bucket );

        ThreadExecutor te = new ThreadExecutor();
        DetachFile t1 = new DetachFile();
        DetachFile t2 = new DetachFile();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        if ( ( t1.getRetCode() != 0 ) && ( t2.getRetCode() != 0 ) ) {
            Assert.fail( "all detach failed t1:" + t1.getThrowable() + " t2:"
                    + t2.getThrowable() );
        }
        if ( t1.getRetCode() == 0 && t2.getRetCode() == 0 ) {
            Assert.fail( "all detach success" );
        }

        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getBucketId(), null );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    class DetachFile extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmFactory.Bucket.detachFile( session, bucketName, key );
            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            }
        }
    }

    private ScmId createScmFile( ScmBucket bucket ) throws ScmException {
        ScmFile file = bucket.createFile( key );
        file.setContent( filePath );
        file.setFileName( key );
        file.setAuthor( "author4244" );

        file.save( new ScmUploadConf( true, true ) );
        return file.getFileId();
    }
}
