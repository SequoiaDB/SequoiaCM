package com.sequoiacm.s3.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


/**
 * @Description SCM-4830 :: 指定当前版本列取文件，其中当前版本为deleteMarker标记 ; SCM-4832 ::
 *              指定历史版本列取文件，其中历史版本存在deleteMarker标记 ; SCM-4834
 *              ::指定所有版本列取文件，存在deleteMarker标记
 * @author wuyan
 * @Date 2022.07.14
 * @version 1.00
 */
public class ScmFile4830_4832_4834 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4830";
    private String fileName1 = "scmfile4830a";
    private String fileName2 = "scmfile4830b";
    private String fileName3 = "scmfile4830c";
    private String authorName = "author4830";
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmId fileId3 = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 10;
    private byte[] filedata = new byte[ fileSize ];
    private int updateSize = 1024 * 28;
    private byte[] updatedata = new byte[ updateSize ];
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        // scmfile1版本：v1（deleteMarker标记）、v2、v3
        scmBucket.deleteFile( fileName1, false );
        fileId1 = S3Utils.createFile( scmBucket, fileName1, filedata,
                authorName );
        S3Utils.createFile( scmBucket, fileName1, updatedata, authorName );
        // scmfile2版本：v1、v2、v3（deleteMarker标记）、v4
        fileId2 = S3Utils.createFile( scmBucket, fileName2, filedata,
                authorName );
        S3Utils.createFile( scmBucket, fileName2, updatedata );
        scmBucket.deleteFile( fileName2, false );
        S3Utils.createFile( scmBucket, fileName2, updatedata, authorName );
        // scmfile3版本：v1、v2（deleteMarker标记）
        fileId3 = S3Utils.createFile( scmBucket, fileName3, filedata );
        scmBucket.deleteFile( fileName3, false );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // test4829:指定当前版本列取文件(匹配author条件)
        List< String > expectFileNames = new LinkedList< String >();
        expectFileNames.add( fileName1 );
        expectFileNames.add( fileName2 );
        int currentVersionNum = 2;
        listInstanceByCurrentVersion( expectFileNames, currentVersionNum );
        countInstanceByCurrentVersion( currentVersionNum );

        // test4831:指定历史版本列取文件(匹配size条件),包含2个deleteMarker标记版本
        int historyVersionNum = 6;
        expectFileNames.add( fileName3 );
        listInstanceByHistoryVersion( expectFileNames, historyVersionNum );
        countInstanceByHistoryVersion( historyVersionNum );

        // test4833：指定所有版本列取文件(匹配fildId条件)
        int allVersionNum = 9;
        listInstanceByAllVersion( expectFileNames, allVersionNum );
        countInstanceByAllVersion( allVersionNum );

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

    private void listInstanceByCurrentVersion( List< String > expectFileNames,
            int fileVersionNum ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, condition );
        List< String > actFileNames = new ArrayList<>();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            actFileNames.add( file.getFileName() );
            size++;
        }
        cursor.close();

        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                expectFileNames.toArray(),
                "act fileName are :" + actFileNames.toString() );

        // exist 2 current version file
        Assert.assertEquals( size, fileVersionNum );
    }

    private void listInstanceByHistoryVersion( List< String > expectFileNames,
            int fileVersionNum ) throws ScmException {
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId1.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId2.get() ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId3.get() ).get();
        BSONObject condition = ScmQueryBuilder.start().or( obj1, obj2, obj3 )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_HISTORY, condition );

        HashSet<String> actFileNames = new HashSet<>();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            String fileName = file.getFileName();
            int version = file.getMajorVersion();
            if ( fileName.equals( fileName1 ) && version == 3 ) {
                Assert.assertTrue( file.isDeleteMarker() );
            }
            actFileNames.add( fileName );
            size++;
        }
        cursor.close();

        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                expectFileNames.toArray(),
                "query cond :" + condition.toString() );
        // exist history version file
        Assert.assertEquals( size, fileVersionNum,
                "query cond :" + condition.toString() );
    }

    private void listInstanceByAllVersion( List< String > expectFileNames,
            int versionNum ) throws ScmException {
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId1.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId2.get() ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId3.get() ).get();
        BSONObject condition = ScmQueryBuilder.start().or( obj1, obj2, obj3 )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        HashSet<String> actFileNames = new HashSet<>();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();

            actFileNames.add( file.getFileName() );
            size++;
        }
        cursor.close();


        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                expectFileNames.toArray(),
                "act fileName are :" + actFileNames.toString()
                        + " , query cond :" + condition.toString() );

        // exist all version file
        Assert.assertEquals( size, versionNum,
                "query cond :" + condition.toString() );
    }

    private void countInstanceByCurrentVersion( int fileVersionNum )
            throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        long size = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, condition );
        Assert.assertEquals( size, fileVersionNum );
    }

    private void countInstanceByHistoryVersion( int fileVersionNum )
            throws ScmException {
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId1.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId2.get() ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId3.get() ).get();
        BSONObject condition = ScmQueryBuilder.start().or( obj1, obj2, obj3 )
                .get();
        long size = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_HISTORY, condition );
        Assert.assertEquals( size, fileVersionNum );
    }

    private void countInstanceByAllVersion( int fileVersionNum )
            throws ScmException {
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId1.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId2.get() ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId3.get() ).get();
        BSONObject condition = ScmQueryBuilder.start().or( obj1, obj2, obj3 )
                .get();
        long size = ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        Assert.assertEquals( size, fileVersionNum );
    }
}
