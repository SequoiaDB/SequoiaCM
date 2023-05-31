package com.sequoiacm.s3.bucketQuota;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmStatisticsObjectDelta;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-6073:上传对象后再全部删除，统计桶流量信息 SCM-6074:上传对象后全部删除，然后在上传，统计桶流量信息
 *              SCM-6075:上传对象后删除部分对象，统计桶流量信息
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6073_6074_6075 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object6073";
    private String bucketName = "bucket6073";
    private int objectNum = 8;
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
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        putObject();
    }

    @Test
    public void test() throws Exception {
        // 上传对象后删除部分对象，统计桶流量信息
        test1();
        // 上传对象后再全部删除，统计桶流量信息
        test2();
        // 上传对象后全部删除，然后在上传，统计桶流量信息
        test3();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
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
        for ( int i = 0; i < objectNum; i++ ) {
            s3Client.putObject( bucketName, keyName + i, new File( filePath ) );
        }
    }

    public void checkObjectDelta( int objectNum ) throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.ObjectDelta.BUCKET_NAME )
                .is( bucketName ).get();
        ScmStatisticsObjectDelta objectDelta = null;
        try ( ScmCursor< ScmStatisticsObjectDelta > cursor = ScmSystem.Statistics
                .listObjectDelta( session, cond ) ;) {

            while ( cursor.hasNext() ) {
                objectDelta = cursor.getNext();
            }
        }
        Assert.assertEquals( objectDelta.getBucketName(), bucketName );
        Assert.assertEquals( objectDelta.getCountDelta(), objectNum );
        Assert.assertEquals( objectDelta.getSizeDelta(), objectNum * fileSize );
        Assert.assertTrue(
                objectDelta.getUpdateTime().after( beginRefreshData ) );
        Assert.assertTrue(
                objectDelta.getUpdateTime().before( afterRefreshData ) );
    }

    public void test1() throws ScmException {
        s3Client.deleteObject( bucketName, keyName + 0 );

        beginRefreshData = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( beginRefreshData );
        calendar.add( Calendar.SECOND, -2 );
        beginRefreshData = calendar.getTime();

        ScmSystem.Statistics.refreshObjectDelta( session, bucketName );

        afterRefreshData = new Date();
        calendar.setTime( afterRefreshData );
        calendar.add( Calendar.SECOND, 2 );
        afterRefreshData = calendar.getTime();

        checkObjectDelta( objectNum - 1 );
    }

    public void test2() throws ScmException {
        S3Utils.deleteAllObjects( s3Client, bucketName );

        beginRefreshData = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( beginRefreshData );
        calendar.add( Calendar.SECOND, -2 );
        beginRefreshData = calendar.getTime();

        ScmSystem.Statistics.refreshObjectDelta( session, bucketName );

        afterRefreshData = new Date();
        calendar.setTime( afterRefreshData );
        calendar.add( Calendar.SECOND, 2 );
        afterRefreshData = calendar.getTime();

        checkObjectDelta( 0 );
    }

    public void test3() throws ScmException {
        putObject();

        beginRefreshData = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( beginRefreshData );
        calendar.add( Calendar.SECOND, -2 );
        beginRefreshData = calendar.getTime();

        ScmSystem.Statistics.refreshObjectDelta( session, bucketName );

        afterRefreshData = new Date();
        calendar.setTime( afterRefreshData );
        calendar.add( Calendar.SECOND, 2 );
        afterRefreshData = calendar.getTime();

        checkObjectDelta( objectNum );
    }
}
