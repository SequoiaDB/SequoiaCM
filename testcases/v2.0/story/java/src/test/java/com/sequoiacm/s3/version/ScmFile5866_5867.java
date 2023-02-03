package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @Descreption SCM-5866 :: 使用CountFile接口列取版本文件; SCM-5867 :: 使用listFile接口列取版本文件;
 * @Author zhangaiping
 * @Date 2023.01.31
 * @Version 1.00
 */
public class ScmFile5866_5867 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private String bucketName = "bucket5866";
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private String authorName = "author5866";
    private String fileName1 = "scmfile5866_1";
    private String fileName2 = "scmfile5866_2";
    private String fileName4 = "scmfile5866_4";
    private String fileName5 = "scmfile5866_5";
    private String fileName6 = "scmfile5866_6";
    private ScmId fileId4 = null;
    private ScmId fileId5 = null;
    private ScmId fileId6 = null;
    private int updateSizeV2 = 1024 * 20;
    private byte[] updatedataV2 = new byte[ updateSizeV2 ];
    private int updateSizeV3 = 1024 * 30;
    private byte[] updatedataV3 = new byte[ updateSizeV3 ];
    private List< Long > createTimeList = new ArrayList<>();
    private List< Long > fileSizeList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();

        // scmfile5866_1 ~ scmfile5866_10版本：v1
        for ( int i = 0; i < 10; i++ ) {
            ScmId fileId = S3Utils.createFile( scmBucket,
                    "scmfile5866_" + ( i + 1 ), filedata, authorName );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            /*
             * tips:有一点值得注意，getCreateTime拿的是时间戳(Date类型)
             * 需要用getTime拿到经过hash计算的值才是最精确的(精确到毫秒甚至更细的单位进行的计算)
             * 如果使用时间戳，可能只会生成1~2个结果(精确到秒) 一秒之内创建完的就是1(比如说所有文件都是10:22:53秒这一秒创建完的)
             * 所有文件一秒之内没创建完，在第二秒创建完了就是2(7个是53秒，3个54秒) 以此类推。 List< Date >
             * createTimeList = new ArrayList<>(); Date create_time =
             * file.getCreateTime();
             */
            long create_time = file.getCreateTime().getTime();
            long fileSize = file.getSize();
            createTimeList.add( create_time );
            fileSizeList.add( fileSize );
            if ( file.getFileName().equals( "scmfile5866_4" ) ) {
                fileId4 = fileId;
            } else if ( file.getFileName().equals( "scmfile5866_5" ) ) {
                fileId5 = fileId;
            } else if ( file.getFileName().equals( "scmfile5866_6" ) ) {
                fileId6 = fileId;
            }
        }
        // scmfile1、scmfile2版本：v2 (deleteMarker标记)
        scmBucket.deleteFile( fileName1, false );
        scmBucket.deleteFile( fileName2, false );
        // 其他文件(scmfile5866_3~scmfile5866_10)分别创建多个版本，如v1,v2,v3
        for ( int i = 2; i < 10; i++ ) {
            S3Utils.createFile( scmBucket, "scmfile5866_" + ( i + 1 ),
                    updatedataV2, authorName );
            S3Utils.createFile( scmBucket, "scmfile5866_" + ( i + 1 ),
                    updatedataV3, authorName );
        }
    }

    @Test
    public void test() throws Exception {
        // SCM-5866:使用CountFile接口列取版本文件
        countFileByCurrentVersion();
        countFileByHistoryVersion();
        countFileByAllVersion();
        countFileNoVersion();
        // SCM-5867:使用listFile接口列取版本文件
        listFileByCurrentVersion();
        listFileByHistoryVerions();
        listFileByAllVersion();
        listFileNoVersion();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void countFileByCurrentVersion() throws Exception {
        // 当前文件不加筛选条件的数量((总上传)10-(逻辑删除)2=8)
        long currentCount = scmBucket.countFile(
                ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject() );
        long expectCurrentCount = 8;
        Assert.assertEquals( currentCount, expectCurrentCount );

        // 当前文件符合条件的数量(文件名为scmfile_5866_3 ~ scmfile_5866_10，共8个)
        String[] fileNameList = new String[ 8 ];
        for ( int i = 0; i < 8; i++ ) {
            fileNameList[ i ] = "scmfile5866_" + ( i + 3 );
        }
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).in( fileNameList )
                .get();
        long currentCountByCondition = scmBucket
                .countFile( ScmType.ScopeType.SCOPE_CURRENT, condition );
        long expectCurrentCountByCondition = 8;
        Assert.assertEquals( currentCountByCondition,
                expectCurrentCountByCondition );

        // 当前文件使用错误筛选条件的数量(文件名是scmfile5866_0的，数量为0)
        long currentCountByErrorCondition = scmBucket.countFile(
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                        .is( "scmfile5866_0" ).get() );
        long expectCurrentCountByErrorCondition = 0;
        Assert.assertEquals( currentCountByErrorCondition,
                expectCurrentCountByErrorCondition );

        // 指定非法的查询参数
        try {
            scmBucket.countFile( ScmType.ScopeType.SCOPE_CURRENT,
                    ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                            .is( authorName ).get() );
            Assert.fail( "expect fail but success!" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.INVALID_ARGUMENT ) ) {
                throw e;
            }
        }
    }

    private void countFileByHistoryVersion() throws Exception {
        // 历史文件不使用筛选条件的数量(除了V3版本的数量，2+8+8=18)
        long historyCount = scmBucket.countFile(
                ScmType.ScopeType.SCOPE_HISTORY, new BasicBSONObject() );
        long expectHistoryCount = 18;
        Assert.assertEquals( historyCount, expectHistoryCount );

        // 历史文件使用筛选条件的数量(记录所有历史文件的创建时间，判断是否是18个)
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.CREATE_TIME ).in( createTimeList )
                .get();
        long historyCountByCondition = scmBucket
                .countFile( ScmType.ScopeType.SCOPE_HISTORY, condition );
        long expectHistoryCountByCondition = 18;
        Assert.assertEquals( historyCountByCondition,
                expectHistoryCountByCondition );

        // 历史文件使用错误筛选条件的数量(创建时间为0000000000000，数量为0)
        long historyCountByErrorCondition = scmBucket
                .countFile( ScmType.ScopeType.SCOPE_HISTORY,
                        ScmQueryBuilder
                                .start( ScmAttributeName.File.CREATE_TIME )
                                .is( "0000000000000" ).get() );
        long expectHistoryCountByErrorCondition = 0;
        Assert.assertEquals( historyCountByErrorCondition,
                expectHistoryCountByErrorCondition );
    }

    private void countFileByAllVersion() throws Exception {
        // 全部文件不加筛选条件的数量(10_v1+2_v2+8_v2+8_v3=28)
        long allCount = scmBucket.countFile( ScmType.ScopeType.SCOPE_ALL,
                new BasicBSONObject() );
        long expectAllCount = 28;
        Assert.assertEquals( allCount, expectAllCount );

        // 全部文件加筛选条件的数量(文件大小为fileSize10k的,有scmfile5866_1 ~ scmfile5866_10=10)
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.SIZE ).in( fileSizeList ).get();
        long allCountByCondition = scmBucket
                .countFile( ScmType.ScopeType.SCOPE_ALL, condition );
        long expectAllCountByCondition = 10;
        Assert.assertEquals( allCountByCondition, expectAllCountByCondition );

        // 全部文件使用错误筛选条件的数量(文件大小为40k的，数量为0)
        // tip:is内，入0*1024匹配2条，入10*1024匹配10条，入20*1024，30*1024各匹配8条
        long allCountByErrorCondition = scmBucket.countFile(
                ScmType.ScopeType.SCOPE_ALL,
                ScmQueryBuilder.start( ScmAttributeName.File.SIZE )
                        .is( 40 * 1024 ).get() );
        long expectAllCountByErrorCondition = 0;
        Assert.assertEquals( allCountByErrorCondition,
                expectAllCountByErrorCondition );
    }

    private void countFileNoVersion() throws Exception {
        // 不指定查询版本的数量(2_v2+8_v3=10)
        long noVersionCount = scmBucket.countFile( new BasicBSONObject() );
        long expectNoVersionCount = 10;
        Assert.assertEquals( noVersionCount, expectNoVersionCount );

        // 指定无效的查询参数
        BSONObject errorCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( "scmfile5866_0" )
                .get();
        long size = scmBucket.countFile( errorCondition );
        Assert.assertEquals( size, 0 );
    }

    private void listFileByCurrentVersion() throws Exception {
        int expectSize = 8;
        int expectVersion = 3;
        HashSet< String > fileNameList = new HashSet<>();
        for ( int i = 0; i < 8; i++ ) {
            fileNameList.add( "scmfile5866_" + ( i + 3 ) );
        }
        // 当前文件查询文件列表信息
        List< String > actFileNames = new ArrayList<>();
        ScmCursor< ScmFileBasicInfo > currentList = scmBucket.listFile(
                ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject(),
                new BasicBSONObject(), 0, -1 );
        while ( currentList.hasNext() ) {
            ScmFileBasicInfo file = currentList.getNext();
            Assert.assertFalse( file.isDeleteMarker(), file.getFileName() );
            Assert.assertEquals( file.getMajorVersion(), expectVersion );
            actFileNames.add( file.getFileName() );
        }
        currentList.close();
        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                fileNameList.toArray(),
                "query cond :" + new BasicBSONObject() );

        // 清空中间量
        actFileNames.clear();
        fileNameList.clear();

        // 指定查询条件的数量
        fileNameList.add( fileName4 );
        fileNameList.add( fileName5 );
        fileNameList.add( fileName6 );
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId4.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId5.get() ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId6.get() ).get();
        BSONObject condition = ScmQueryBuilder.start().or( obj1, obj2, obj3 )
                .get();
        BSONObject orderby = new BasicBSONObject();
        orderby.put( ScmAttributeName.File.FILE_NAME, -1 );
        ScmCursor< ScmFileBasicInfo > currentListByCondition = scmBucket
                .listFile( ScmType.ScopeType.SCOPE_CURRENT, condition, orderby,
                        0, -1 );
        while ( currentListByCondition.hasNext() ) {
            ScmFileBasicInfo file = currentListByCondition.getNext();
            actFileNames.add( file.getFileName() );
            Assert.assertFalse( file.isDeleteMarker() );
        }
        currentListByCondition.close();
        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                fileNameList.toArray(), "query cond :" + condition );

        int size = 0;
        // 指定无效的查询参数
        BSONObject errorCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( "scmfile5866_0" )
                .get();
        ScmCursor< ScmFileBasicInfo > currentListByErrorParam = scmBucket
                .listFile( ScmType.ScopeType.SCOPE_CURRENT, errorCondition,
                        new BasicBSONObject(), 0, expectSize );
        while ( currentListByErrorParam.hasNext() ) {
            currentListByErrorParam.getNext();
            size += 1;
        }
        currentListByErrorParam.close();
        Assert.assertEquals( size, 0 );

        // 指定非法的查询参数
        try {
            scmBucket.listFile( ScmType.ScopeType.SCOPE_CURRENT,
                    ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                            .is( authorName ).get(),
                    new BasicBSONObject(), 0, expectSize );
            Assert.fail( "expect fail but success!" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.INVALID_ARGUMENT ) ) {
                throw e;
            }
        }
    }

    private void listFileByHistoryVerions() throws ScmException {
        int size = 0;
        int expectSize = 18;
        List< String > expectV1FileNameList = new ArrayList<>();
        List< String > expectV2FileNameList = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            expectV1FileNameList.add( "scmfile5866_" + ( i + 1 ) );
        }
        for ( int i = 2; i < 10; i++ ) {
            expectV2FileNameList.add( "scmfile5866_" + ( i + 1 ) );
        }

        // 历史文件查询文件列表信息
        HashSet< String > actV1FileNames = new HashSet<>();
        HashSet< String > actV2FileNames = new HashSet<>();
        ScmCursor< ScmFileBasicInfo > historyList = scmBucket.listFile(
                ScmType.ScopeType.SCOPE_HISTORY, new BasicBSONObject(),
                new BasicBSONObject(), 0, -1 );
        while ( historyList.hasNext() ) {
            ScmFileBasicInfo file = historyList.getNext();
            Assert.assertFalse( file.isDeleteMarker() );
            if ( file.getMajorVersion() == 1 ) {
                actV1FileNames.add( file.getFileName() );
            } else if ( file.getMajorVersion() == 2 ) {
                actV2FileNames.add( file.getFileName() );
            }
            size += 1;
        }
        historyList.close();
        Assert.assertEqualsNoOrder( actV1FileNames.toArray(),
                expectV1FileNameList.toArray(),
                "query cond :" + new BasicBSONObject() );
        Assert.assertEqualsNoOrder( actV2FileNames.toArray(),
                expectV2FileNameList.toArray(),
                "query cond :" + new BasicBSONObject() );
        Assert.assertEquals( size, expectSize );

        // 清空中间量
        size = 0;

        // 历史文件指定查询条件的数量
        List< String > fileNameList = new ArrayList<>();
        fileNameList.add( fileName6 );
        fileNameList.add( fileName5 );
        fileNameList.add( fileName4 );
        HashSet< String > actFileNames = new HashSet<>();
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId4.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId5.get() ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId6.get() ).get();
        BSONObject condition = ScmQueryBuilder.start().or( obj1, obj2, obj3 )
                .get();
        BSONObject orderby = new BasicBSONObject();
        orderby.put( ScmAttributeName.File.CREATE_TIME, -1 );
        ScmCursor< ScmFileBasicInfo > historyListByCondition = scmBucket
                .listFile( ScmType.ScopeType.SCOPE_HISTORY, condition, orderby,
                        0, expectSize );
        while ( historyListByCondition.hasNext() ) {
            ScmFileBasicInfo file = historyListByCondition.getNext();
            actFileNames.add( file.getFileName() );
            size += 1;
        }
        historyListByCondition.close();
        Assert.assertEquals( actFileNames.toArray(), fileNameList.toArray(),
                "query cond :" + condition );
        Assert.assertEquals( size, 6 );

        // 清空中间值
        size = 0;

        // 指定无效的查询参数
        BSONObject errorCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( "scmfile5866_0" )
                .get();
        ScmCursor< ScmFileBasicInfo > historyListByErrorParam = scmBucket
                .listFile( ScmType.ScopeType.SCOPE_HISTORY, errorCondition,
                        new BasicBSONObject(), 0, expectSize );
        while ( historyListByErrorParam.hasNext() ) {
            ScmFileBasicInfo file = historyListByErrorParam.getNext();
            size += 1;
        }
        historyListByErrorParam.close();
        Assert.assertEquals( size, 0 );
    }

    private void listFileByAllVersion() throws Exception {
        int size = 0;
        int expectSize = 28;

        // 全部文件查询文件列表信息
        ScmCursor< ScmFileBasicInfo > allList = scmBucket.listFile(
                ScmType.ScopeType.SCOPE_ALL, new BasicBSONObject(),
                new BasicBSONObject(), 0, -1 );
        while ( allList.hasNext() ) {
            ScmFileBasicInfo file = allList.getNext();
            String fileName = file.getFileName();
            int version = file.getMajorVersion();
            if ( fileName.equals( fileName1 ) && version == 2
                    || fileName.equals( fileName2 ) && version == 2 ) {
                Assert.assertTrue( file.isDeleteMarker() );
            } else {
                Assert.assertFalse( file.isDeleteMarker() );
            }
            size++;
        }
        allList.close();
        Assert.assertEquals( size, expectSize );

        // 清空中间量
        size = 0;

        // 全部文件指定查询条件
        List< String > fileNameList = new ArrayList<>();
        fileNameList.add( fileName4 );
        fileNameList.add( fileName5 );
        fileNameList.add( fileName6 );
        HashSet< String > actFileNames = new HashSet<>();
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId4.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId5.get() ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId6.get() ).get();
        BSONObject condition = ScmQueryBuilder.start().or( obj1, obj2, obj3 )
                .get();
        ScmCursor< ScmFileBasicInfo > allListByCondition = scmBucket.listFile(
                ScmType.ScopeType.SCOPE_ALL, condition, new BasicBSONObject(),
                0, -1 );
        while ( allListByCondition.hasNext() ) {
            ScmFileBasicInfo file = allListByCondition.getNext();
            actFileNames.add( file.getFileName() );
            size += 1;
        }
        allListByCondition.close();
        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                fileNameList.toArray(), "query cond :" + condition );
        // 4-6文件 v1 ~ v3版本，共9个
        Assert.assertEquals( size, 9 );

        // 清空中间值
        size = 0;

        // 指定无效的查询参数
        BSONObject errorCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( "scmfile5866_0" )
                .get();
        ScmCursor< ScmFileBasicInfo > ALLListByErrorParam = scmBucket.listFile(
                ScmType.ScopeType.SCOPE_ALL, errorCondition,
                new BasicBSONObject(), 0, -1 );
        while ( ALLListByErrorParam.hasNext() ) {
            ScmFileBasicInfo file = ALLListByErrorParam.getNext();
            size += 1;
        }
        ALLListByErrorParam.close();
        Assert.assertEquals( size, 0 );
    }

    private void listFileNoVersion() throws ScmException {
        int size = 0;
        int expectVersion2 = 2;
        int expectVersion3 = 3;

        // 全部文件查询文件列表信息
        HashSet< String > fileNameList = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            fileNameList.add( "scmfile5866_" + ( i + 1 ) );
        }
        List< String > actFileNames = new ArrayList<>();
        ScmCursor< ScmFileBasicInfo > noVersionList = scmBucket.listFile(
                new BasicBSONObject(), new BasicBSONObject(), 0, -1 );
        while ( noVersionList.hasNext() ) {
            ScmFileBasicInfo file = noVersionList.getNext();
            String filename = file.getFileName();
            if ( filename.equals( fileName1 )
                    || filename.equals( fileName2 ) ) {
                Assert.assertTrue( file.isDeleteMarker(), file.getFileName() );
                Assert.assertEquals( file.getMajorVersion(), expectVersion2 );
            } else {
                Assert.assertFalse( file.isDeleteMarker(), file.getFileName() );
                Assert.assertEquals( file.getMajorVersion(), expectVersion3 );
            }
            actFileNames.add( file.getFileName() );
        }
        noVersionList.close();
        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                fileNameList.toArray(),
                "query cond :" + new BasicBSONObject() );

        // 指定无效的查询参数
        BSONObject errorCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.MAJOR_VERSION ).is( "4" ).get();
        ScmCursor< ScmFileBasicInfo > NoVersionListByErrorParam = scmBucket
                .listFile( errorCondition, new BasicBSONObject(), 0, -1 );
        while ( NoVersionListByErrorParam.hasNext() ) {
            NoVersionListByErrorParam.getNext();
            size += 1;
        }
        NoVersionListByErrorParam.close();
        Assert.assertEquals( size, 0 );
    }
}
