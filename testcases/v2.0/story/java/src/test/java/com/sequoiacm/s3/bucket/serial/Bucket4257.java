package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @Descreption SCM-4257:使用SCM API指定匹配条件列取、统计桶
 * @Author YiPan
 * @CreateDate 2022/5/17
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4257 extends TestScmBase {
    private final String bucketNameBase = "bucket4257no";
    private final String ignoreBuckeName = "bucket4257test";
    private List< String > bucketNames = new ArrayList<>();
    private List< String > envBuckets;
    private ScmSession session;
    private AmazonS3 s3Client = null;
    private final int bucketNum = 30;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        envBuckets = S3Utils.getEnvBuckets();
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        s3Client = S3Utils.buildS3Client();
        for ( int i = 0; i < bucketNum; i++ ) {
            String bucketName = bucketNameBase + i;
            S3Utils.clearBucket( s3Client, bucketNameBase + i );
            s3Client.createBucket( bucketName );
            bucketNames.add( bucketName );
        }
        S3Utils.clearBucket( s3Client, ignoreBuckeName );
        s3Client.createBucket( ignoreBuckeName );
        // 排除自己残留导致的错误排除
        envBuckets.removeAll( bucketNames );
        envBuckets.remove( ignoreBuckeName );
    }

    @Test
    public void test() throws Exception {
        // 指定name忽略ignoreBuckeName，按name降序排序列取桶
        BSONObject cond = ScmQueryBuilder.start()
                .not( ScmQueryBuilder.start( ScmAttributeName.Bucket.NAME )
                        .is( ignoreBuckeName ).get() )
                .get();
        BSONObject orderBy = ScmQueryBuilder
                .start( ScmAttributeName.Bucket.NAME ).is( -1 ).get();
        ScmCursor< ScmBucket > scmBucketScmCursor = ScmFactory.Bucket
                .listBucket( session, cond, orderBy, 0, -1 );
        // 在预期结果中处理环境残留的干扰
        bucketNames.addAll( envBuckets );
        Collections.sort( bucketNames, new StringComparator() );
        S3Utils.checkBucketList( scmBucketScmCursor, bucketNames, true );

        // 指定name忽略ignoreBuckeName，按name降序排序统计桶
        long bucketNum = ScmFactory.Bucket.countBucket( session, cond );
        Assert.assertEquals( bucketNum, bucketNames.size() );

        // 指定匹配条件和排序为null，列取所有桶
        scmBucketScmCursor = ScmFactory.Bucket.listBucket( session, null, null,
                0, -1 );
        // 预期结果在这里添加ignoreBucket，同时清理方法可以直接使用这个集合清理所有测试桶
        bucketNames.add( ignoreBuckeName );
        S3Utils.checkBucketList( scmBucketScmCursor, bucketNames, false );

        // 指定匹配条件和排序为null，统计所有桶
        bucketNum = ScmFactory.Bucket.countBucket( session, null );
        Assert.assertEquals( bucketNum, bucketNames.size() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                bucketNames.removeAll( envBuckets );
                for ( String bucketName : bucketNames ) {
                    S3Utils.clearBucket( s3Client, bucketName );
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