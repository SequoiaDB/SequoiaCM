package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4234 :: SCM API列取、统计桶，S3接口并发创建、删除桶
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4234 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4234";

    private boolean runSuccess = false;
    private String bucketName = "bucket4234";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );

        site = ScmInfo.getSite();
        int siteNum = ScmInfo.getSiteNum();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.createS3WS(session, wsName);
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        createBuckets( bucketName, 1, 3 );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        ScmListBuckets t1 = new ScmListBuckets();
        ScmCountBuckets t2 = new ScmCountBuckets();
        CreateS3Bucket t3 = new CreateS3Bucket();
        DeleteS3Bucket t4 = new DeleteS3Bucket();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.addWorker( t3 );
        te.addWorker( t4 );
        te.run();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );
                ScmWorkspaceUtil.deleteWs( wsName, session );
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

    class ScmListBuckets {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            int count = 0;
            ScmCursor< ScmBucket > bucketCursor = ScmFactory.Bucket
                    .listBucket( session, wsName, scmUserName );
            while ( bucketCursor.hasNext() ) {
                ScmBucket bucket = bucketCursor.getNext();
                if ( bucket.getName().startsWith( bucketName ) ) {
                    count++;
                }
            }
            Assert.assertTrue( count < 10 );
        }
    }

    class ScmCountBuckets {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            long count = ScmFactory.Bucket.countBucket( session, wsName,
                    scmUserName );
            Assert.assertTrue( count < 10 );
        }
    }

    class CreateS3Bucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            createBuckets( bucketName, 4, 10 );
        }
    }

    class DeleteS3Bucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            for ( int i = 1; i <= 3; i++ ) {
                s3Client.deleteBucket( bucketName + "-" + i );
            }
        }
    }

    private void createBuckets( String prefix, int start, int end ) {
        for ( int i = start; i <= end; i++ ) {
            CreateBucketRequest request = new CreateBucketRequest(
                    prefix + "-" + i, wsName );
            s3Client.createBucket( request );
        }
    }

}
