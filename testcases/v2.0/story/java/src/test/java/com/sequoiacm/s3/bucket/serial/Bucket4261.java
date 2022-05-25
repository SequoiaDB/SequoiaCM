package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @Descreption SCM-4261:使用SCM API指定匹配条件、排序条件、skil、limit列取桶
 * @Author YiPan
 * @CreateDate 2022/5/17
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4261 extends TestScmBase {
    private final String bucketNameBase = "bucket4261no";
    private final String ignoreBuckeName = "bucket4261test";
    private List< String > bucketNames = new ArrayList<>();
    private ScmSession session;
    private AmazonS3 s3Client = null;
    private final int bucketNum = 30;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        s3Client = S3Utils.buildS3Client();
        for ( int i = 0; i < bucketNum; i++ ) {
            String buckeName = bucketNameBase + i;
            S3Utils.clearBucket( s3Client, bucketNameBase + i );
            s3Client.createBucket( buckeName );
            bucketNames.add( buckeName );
        }
        S3Utils.clearBucket( s3Client, ignoreBuckeName );
        s3Client.createBucket( ignoreBuckeName );
    }

    @Test
    public void test() throws Exception {
        // 指定name忽略ignoreBuckeName，按name降序排序，skip 5，limit 3
        int skip = 5;
        int limit = 3;
        BSONObject cond = ScmQueryBuilder.start()
                .not( ScmQueryBuilder.start( ScmAttributeName.Bucket.NAME )
                        .is( ignoreBuckeName ).get() )
                .get();
        BSONObject orderBy = ScmQueryBuilder
                .start( ScmAttributeName.Bucket.NAME ).is( -1 ).get();
        ScmCursor< ScmBucket > scmBucketScmCursor = ScmFactory.Bucket
                .listBucket( session, cond, orderBy, skip, limit );
        Collections.sort( bucketNames, new StringComparator() );
        List< String > expBucketNames = bucketNames.subList( skip,
                skip + limit );
        S3Utils.checkBucketList( scmBucketScmCursor, expBucketNames, true );

        // 指定匹配条件和排序为null，列取所有
        scmBucketScmCursor = ScmFactory.Bucket.listBucket( session, null, null,
                0, -1 );
        // 预期结果在这里添加ignoreBucket，同时清理方法可以直接使用这个集合清理所有测试桶
        bucketNames.add( ignoreBuckeName );
        S3Utils.checkBucketList( scmBucketScmCursor, bucketNames, false );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                for ( String buckeName : bucketNames ) {
                    S3Utils.clearBucket( s3Client, buckeName );
                }
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            session.close();
        }
    }

    // 用于构造预期结果时给集合降序排序
    private class StringComparator implements Comparator< String > {
        public int compare( String s1, String s2 ) {
            int flag = 0;
            if ( s1.compareTo( s2 ) < 0 ) {
                flag = 1;
            } else if ( s1.compareTo( s2 ) > 0 ) {
                flag = -1;
            }
            return flag;
        }
    }
}