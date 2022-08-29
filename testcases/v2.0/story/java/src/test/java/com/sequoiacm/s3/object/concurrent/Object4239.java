package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4239 :: S3接口更新文件内容和SCM API删除文件并发
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4239 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4239";
    private String key = "aa/bb/object4239";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );

        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, key, "aaa" );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        S3UpdateObject t1 = new S3UpdateObject();
        ScmDeleteFile t2 = new ScmDeleteFile();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        s3Client.putObject( bucketName, key, "ccc" );

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
            Thread.sleep( 10 );
            s3Client.putObject( bucketName, key, "bbb" );
        }
    }

    class ScmDeleteFile {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            ScmFile file = bucket.getFile( key );
            Thread.sleep( 10 );
            file.delete( true );
        }
    }
}
