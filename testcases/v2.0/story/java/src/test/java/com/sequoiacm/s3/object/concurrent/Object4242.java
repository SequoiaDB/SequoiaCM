package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.common.ScmType;
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

/**
 * @descreption SCM-4242 :: SCM API下载文件和S3接口更新文件并发
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4242 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4242";
    private String key = "aa/bb/object4242";

    private int fileSize = 1024;
    private String text1 = "aaa";
    private String text2 = "bbb";
    private String text3 = "ccc";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, key, text1 );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        S3UpdateObject t1 = new S3UpdateObject();
        ScmGetFile t2 = new ScmGetFile();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        Assert.assertTrue( s3Client.doesObjectExist( bucketName, key ) );

        s3Client.putObject( bucketName, key, text3 );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
        }

        if ( s3Client != null ) {
            s3Client.shutdown();
        }

        if ( session != null ) {
            session.close();
        }
    }

    class S3UpdateObject {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            s3Client.putObject( bucketName, key, text2 );
        }
    }

    class ScmGetFile {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            ScmFile file = bucket.getFile( key );
            ScmInputStream is = ScmFactory.File.createInputStream(
                    ScmType.InputStreamType.UNSEEKABLE, file );

            byte[] read_buf = new byte[ fileSize ];
            int read_len = 0;
            if ( ( read_len = is.read( read_buf, 0, fileSize ) ) > 0 ) {
                String text = new String( read_buf, 0, read_len );
                if ( !( text1.equals( text ) ) && !( text2.equals( text ) ) ) {
                    Assert.fail(
                            "down file content does not match text1 and text2" );
                }
            } else {
                Assert.fail( "down file failed" );
            }
        }
    }
}
