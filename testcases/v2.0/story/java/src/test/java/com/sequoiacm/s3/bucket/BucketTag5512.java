package com.sequoiacm.s3.bucket;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.TagSet;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5512:操作桶标签
 * @author YiPan
 * @date 2022/12/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BucketTag5512 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private TagSet tagSet = new TagSet();
    private TagSet newTagSet = new TagSet();
    private TagSet newUnSortedTagSet = new TagSet();
    private Map< String, String > map = new TreeMap<>();
    private Map< String, String > newMap = new HashMap<>();
    private Map< String, String > newUnsortedMap = new HashMap<>();
    private String bucketName = "bucket5512";
    private String enableBucketName = "enablebucket5512";
    private String suspendedBucketName = "suspendedbucket5512";
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        S3Utils.clearBucket( s3Client, enableBucketName );
        S3Utils.clearBucket( s3Client, suspendedBucketName );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        initTag();
    }

    @DataProvider(name = "bucketNames")
    public Object[] initBucket() {
        s3Client.createBucket( bucketName );
        s3Client.createBucket( enableBucketName );
        s3Client.createBucket( suspendedBucketName );
        S3Utils.updateBucketVersionConfig( s3Client, enableBucketName,
                BucketVersioningConfiguration.ENABLED );
        S3Utils.updateBucketVersionConfig( s3Client, suspendedBucketName,
                BucketVersioningConfiguration.SUSPENDED );
        return new Object[] { bucketName, enableBucketName,
                suspendedBucketName };
    }

    @Test(groups = { GroupTags.base }, dataProvider = "bucketNames")
    public void test( String bucketName ) throws ScmException {
        testS3API( bucketName );

        testSCMAPI( bucketName );
        runSuccessCount.getAndIncrement();
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccessCount.get() == 3 || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
                S3Utils.clearBucket( s3Client, enableBucketName );
                S3Utils.clearBucket( s3Client, suspendedBucketName );
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

    private void testSCMAPI( String bucketName ) throws ScmException {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        // 设置标签
        bucket.setCustomTag( map );
        // 获取校验
        Assert.assertEquals( bucket.getCustomTag(), map );

        // 重复设置降序标签
        bucket.setCustomTag( newUnsortedMap );

        // 获取校验按升序排序
        Assert.assertEquals( bucket.getCustomTag(), newMap );

        // 删除桶标签
        bucket.deleteCustomTag();
        //SEQUOIACM-1175验证删除标签后，Map对象不被清空
        Assert.assertNotEquals( map, new TreeMap< String, String >() );

        // 获取校验
        Assert.assertEquals( bucket.getCustomTag(), null );
    }

    private void testS3API( String bucketName ) {
        // 设置标签
        S3Utils.setBucketTag( s3Client, bucketName, tagSet );

        // 获取校验
        BucketTaggingConfiguration configuration = s3Client
                .getBucketTaggingConfiguration( bucketName );
        Assert.assertEquals( configuration.getTagSet().toString(),
                tagSet.toString() );

        // 重复设置降序标签
        S3Utils.setBucketTag( s3Client, bucketName, newUnSortedTagSet );

        // 获取校验按升序排序
        configuration = s3Client.getBucketTaggingConfiguration( bucketName );
        Assert.assertEquals( configuration.getTagSet().toString(),
                newTagSet.toString() );
        // 删除桶标签
        s3Client.deleteBucketTaggingConfiguration( bucketName );

        // 获取校验
        configuration = s3Client.getBucketTaggingConfiguration( bucketName );
        Assert.assertNull( configuration );
    }

    private void initTag() {
        String baseKey = "test";
        String baseValue = "value";
        // 构造升序的标签
        for ( int i = 0; i < 10; i++ ) {
            map.put( baseKey + i, baseValue + i );
            tagSet.setTag( baseKey + i, baseValue + i );
            if ( i > 5 ) {
                newMap.put( baseKey + i, baseValue + i );
                newTagSet.setTag( baseKey + i, baseValue + i );
            }
        }
        // 构造降序的标签
        for ( int i = 9; i > 5; i-- ) {
            newUnsortedMap.put( baseKey + i, baseValue + i );
            newUnSortedTagSet.setTag( baseKey + i, baseValue + i );
        }
    }
}
