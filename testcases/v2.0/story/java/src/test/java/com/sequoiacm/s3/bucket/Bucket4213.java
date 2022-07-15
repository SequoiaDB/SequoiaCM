package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4213 :: SCM API创建桶，S3接口操作桶
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4213 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4213";
    private String key = "object4213";
    private int bucket_number = 20;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException {
        // scm api create buckets
        List< String > bucketList = new ArrayList<>();
        for ( int i = 0; i < bucket_number; i++ ) {
            String bucketNameN = bucketName + "-" + i;
            ScmFactory.Bucket.createBucket( ws, bucketNameN );
            bucketList.add( bucketNameN );
        }

        // s3 list buckets, check buckets number== bucket_number
        checkBucketNumbers( bucketName, bucketList );

        // s3 get bucket objects
        ScmFactory.Bucket.createBucket( ws, bucketName );
        s3Client.putObject( bucketName, key, "content" );
        s3Client.listObjects( bucketName );
        s3Client.deleteObject( bucketName, key );
        s3Client.deleteBucket( bucketName );

        // s3 delete buckets bucket_number/2
        for ( int i = 0; i < bucket_number / 2; i++ ) {
            String bucketNameN = bucketName + "-" + i;
            s3Client.deleteBucket( bucketNameN );
            bucketList.remove( bucketNameN );
        }

        // s3 list buckets
        checkBucketNumbers( bucketName, bucketList );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
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

    private void checkBucketNumbers( String bucketPrefix,
            List< String > bucketList ) {
        List< String > bucketNames = new ArrayList<>();
        List< Bucket > buckets = s3Client.listBuckets();
        int count = 0;

        for ( int i = 0; i < buckets.size(); i++ ) {
            if ( buckets.get( i ).getName().startsWith( bucketPrefix ) ) {
                bucketNames.add(buckets.get( i ).getName());
                count++;
            }
        }

        if ( count != bucketList.size() ) {
            Assert.fail( "receive buckets number " + count
                    + " != expect number " + bucketList.size() );
        }

        Assert.assertEqualsNoOrder( bucketNames.toArray(),
                bucketList.toArray() );
    }

}
