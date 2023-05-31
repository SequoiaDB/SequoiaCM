package com.sequoiacm.s3.bucketQuota;

import java.io.File;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmStatisticsObjectDelta;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.joda.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

import javax.xml.crypto.Data;

/**
 * @description SCM-6063:指定不同的过滤条件，统计桶流量信息
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6063 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName1 = "object6063a";
    private String keyName2 = "object6063b";
    private String keyName3 = "object6063c";
    private String bucketName1 = "bucket6063a";
    private String bucketName2 = "bucket6063b";
    private String bucketName3 = "bucket6063c";
    private int objectNum1 = 8;
    private int objectNum2 = 9;
    private int objectNum3 = 10;
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private ScmSession session = null;
    private Date beginRefreshData;
    private Date afterRefreshData;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName1 );
        S3Utils.clearBucket( s3Client, bucketName2 );
        S3Utils.clearBucket( s3Client, bucketName3 );
        s3Client.createBucket( bucketName1 );
        s3Client.createBucket( bucketName2 );
        s3Client.createBucket( bucketName3 );
        putObject();
        // 构造预期刷新时间，考虑机器时间同步问题，允许4s误差
        beginRefreshData = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( beginRefreshData );
        calendar.add( Calendar.SECOND, -180 );
        beginRefreshData = calendar.getTime();

        ScmSystem.Statistics.refreshObjectDelta( session, bucketName1 );
        ScmSystem.Statistics.refreshObjectDelta( session, bucketName2 );
        ScmSystem.Statistics.refreshObjectDelta( session, bucketName3 );

        afterRefreshData = new Date();
        calendar.setTime( afterRefreshData );
        calendar.add( Calendar.SECOND, 60 );
        afterRefreshData = calendar.getTime();
    }

    @Test
    public void test() throws Exception {
        // 按桶名过滤
        test1();
        // 按对象数量增量过滤
        test2();
        // 按对象大小增量过滤
        test3();
        // 按统计时间过滤
        test4();
        // 桶名+对象数量增量+对象大小增量+统计时间
        test5();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName1 );
                S3Utils.clearBucket( s3Client, bucketName2 );
                S3Utils.clearBucket( s3Client, bucketName3 );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            s3Client.shutdown();
        }
    }

    public void putObject() {
        for ( int i = 0; i < objectNum1; i++ ) {
            s3Client.putObject( bucketName1, keyName1 + i,
                    new File( filePath ) );
        }
        for ( int i = 0; i < objectNum2; i++ ) {
            s3Client.putObject( bucketName2, keyName2 + i,
                    new File( filePath ) );
        }
        for ( int i = 0; i < objectNum3; i++ ) {
            s3Client.putObject( bucketName3, keyName3 + i,
                    new File( filePath ) );
        }
    }

    public void test1() throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.BUCKET_NAME )
                .is( bucketName1 ).get();
        ScmStatisticsObjectDelta objectDelta = null;
        try ( ScmCursor< ScmStatisticsObjectDelta > cursor = ScmSystem.Statistics
                .listObjectDelta( session, cond ) ;) {
            while ( cursor.hasNext() ) {
                objectDelta = cursor.getNext();
            }
        }
        Assert.assertEquals( objectDelta.getBucketName(), bucketName1 );
        Assert.assertEquals( objectDelta.getCountDelta(), objectNum1 );
        Assert.assertEquals( objectDelta.getSizeDelta(),
                objectNum1 * fileSize );
        Assert.assertTrue(
                objectDelta.getUpdateTime().after( beginRefreshData ) );
        Assert.assertTrue(
                objectDelta.getUpdateTime().before( afterRefreshData ) );
    }

    public void test2() throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.COUNT_DELTA )
                .greaterThanEquals( objectNum3 ).get();
        ScmStatisticsObjectDelta objectDelta = null;
        try ( ScmCursor< ScmStatisticsObjectDelta > cursor = ScmSystem.Statistics
                .listObjectDelta( session, cond ) ;) {
            while ( cursor.hasNext() ) {
                objectDelta = cursor.getNext();
            }
        }
        Assert.assertEquals( objectDelta.getBucketName(), bucketName3 );
        Assert.assertEquals( objectDelta.getCountDelta(), objectNum3 );
        Assert.assertEquals( objectDelta.getSizeDelta(),
                objectNum3 * fileSize );
        Assert.assertTrue(
                objectDelta.getUpdateTime().after( beginRefreshData ) );
        Assert.assertTrue(
                objectDelta.getUpdateTime().before( afterRefreshData ) );
    }

    public void test3() throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.SIZE_DELTA )
                .is( objectNum2 * fileSize ).get();
        ScmStatisticsObjectDelta objectDelta = null;
        try ( ScmCursor< ScmStatisticsObjectDelta > cursor = ScmSystem.Statistics
                .listObjectDelta( session, cond ) ;) {
            while ( cursor.hasNext() ) {
                objectDelta = cursor.getNext();
            }
        }
        Assert.assertEquals( objectDelta.getBucketName(), bucketName2 );
        Assert.assertEquals( objectDelta.getCountDelta(), objectNum2 );
        Assert.assertEquals( objectDelta.getSizeDelta(),
                objectNum2 * fileSize );
        Assert.assertTrue(
                objectDelta.getUpdateTime().after( beginRefreshData ) );
        Assert.assertTrue(
                objectDelta.getUpdateTime().before( afterRefreshData ) );
    }

    public void test4() throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.UPDATE_TIME )
                .greaterThanEquals( beginRefreshData.getTime() ).get();
        ScmStatisticsObjectDelta objectDelta = null;
        try ( ScmCursor< ScmStatisticsObjectDelta > cursor = ScmSystem.Statistics
                .listObjectDelta( session, cond ) ;) {
            while ( cursor.hasNext() ) {
                ScmStatisticsObjectDelta delta = cursor.getNext();
                if ( delta.getBucketName().equals(bucketName1) )
                    objectDelta = delta;
            }
        }
        Assert.assertEquals( objectDelta.getBucketName(), bucketName1 );
        Assert.assertEquals( objectDelta.getCountDelta(), objectNum1 );
        Assert.assertEquals( objectDelta.getSizeDelta(),
                objectNum1 * fileSize );
        Assert.assertEquals( objectDelta.getSizeDelta(),
                objectNum1 * fileSize );
        Assert.assertTrue(
                objectDelta.getUpdateTime().after( beginRefreshData ) );
        Assert.assertTrue(
                objectDelta.getUpdateTime().before( afterRefreshData ) );
    }

    public void test5() throws ScmException {
        BSONObject cond1 = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.BUCKET_NAME )
                .is( bucketName1 ).get();
        BSONObject cond2 = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.COUNT_DELTA )
                .greaterThanEquals( objectNum1 ).get();
        BSONObject cond3 = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.SIZE_DELTA )
                .is( objectNum1 * fileSize ).get();
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.UPDATE_TIME )
                .greaterThan( beginRefreshData.getTime() ).and( cond1 ).and( cond2 )
                .and( cond3 ).get();

        ScmStatisticsObjectDelta objectDelta = null;
        try ( ScmCursor< ScmStatisticsObjectDelta > cursor = ScmSystem.Statistics
                .listObjectDelta( session, cond ) ;) {
            while ( cursor.hasNext() ) {
                ScmStatisticsObjectDelta delta = cursor.getNext();
                if ( delta.getBucketName().equals(bucketName1 ) )
                    objectDelta = delta;
            }
        }
        Assert.assertEquals( objectDelta.getBucketName(), bucketName1 );
        Assert.assertEquals( objectDelta.getCountDelta(), objectNum1 );
        Assert.assertEquals( objectDelta.getSizeDelta(),
                objectNum1 * fileSize );
        Assert.assertEquals( objectDelta.getSizeDelta(),
                objectNum1 * fileSize );
        Assert.assertTrue(
                objectDelta.getUpdateTime().after( beginRefreshData ) );
        Assert.assertTrue(
                objectDelta.getUpdateTime().before( afterRefreshData ) );
    }
}
