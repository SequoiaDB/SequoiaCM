package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @descreption SCM-4229 :: S3接口和SCM API并发列取桶
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Buckets4229 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4229";
    private int bucket_number = 200;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        // prepared buckets
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );
        for ( int i = 0; i < bucket_number; i++ ) {
            String bucketNameN = bucketName + "-" + i;
            ScmFactory.Bucket.createBucket( ws, bucketNameN );
        }
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 100000 );
        ListS3Buckets t1 = new ListS3Buckets();
        ListSCMBuckets t2 = new ListSCMBuckets();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );
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

    class ListS3Buckets {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            List< Bucket > buckets = s3Client.listBuckets();
            int count = 0;

            for ( int i = 0; i < buckets.size(); i++ ) {
                if ( buckets.get( i ).getName().startsWith( bucketName ) ) {
                    count++;
                }
            }
            if ( count != bucket_number ) {
                Assert.fail( "receive buckets count " + count
                        + " != expect number " + bucket_number );
            }
        }
    }

    class ListSCMBuckets {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            int count = 0;
            ScmCursor< ScmBucket > bucketCursor = ScmFactory.Bucket
                    .listBucket( session, s3WorkSpaces, null );
            while ( bucketCursor.hasNext() ) {
                ScmBucket bucket = bucketCursor.getNext();
                if ( bucket.getName().startsWith( bucketName ) ) {
                    count++;
                }
            }

            if ( count != bucket_number ) {
                Assert.fail( "receive buckets count " + count
                        + " != expect number " + bucket_number );
            }
        }
    }
}
