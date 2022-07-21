package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.*;
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
 * @descreption SCM-4240 :: SCM API更新文件内容和S3接口删除文件并发
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4240 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private ScmSession session = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4240";
    private String key = "aa/bb/object4240";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, key, new File( filePath ) );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        S3DeleteObject t1 = new S3DeleteObject();
        ScmUpdateFile t2 = new ScmUpdateFile();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        Assert.assertFalse( s3Client.doesObjectExist( bucketName, key ) );

        s3Client.putObject( bucketName, key, "test content aa/bb/object4240" );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
            TestTools.LocalFile.removeFile( localPath );
        }

        if ( s3Client != null ) {
            s3Client.shutdown();
        }

        if ( session != null ) {
            session.close();
        }
    }

    class S3DeleteObject {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            s3Client.deleteObject( bucketName, key );
        }
    }

    class ScmUpdateFile {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = bucket.getFile( key );
                file.updateContent( filePath );
            } catch ( ScmException e ) {
                Assert.assertEquals(e.getError().getErrorDescription(), "File not found");
            }
        }
    }
}
