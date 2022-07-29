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
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description SCM-4829 :: 指定当前版本列取文件;SCM-4831 ::指定历史版本列取文件; SCM-4833
 *              ::指定所有版本列取文件
 * @author wuyan
 * @Date 2022.07.13
 * @version 1.00
 */
public class ScmFile4829_4831_4833 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4829";
    private String fileName1 = "scmfile4829a";
    private String fileName2 = "scmfile4829b";
    private String fileName3 = "scmfile4829c";
    private String authorName = "author4829";
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
        fileId1 = S3Utils.createFile( scmBucket, fileName1, filedata,
                authorName );
        S3Utils.createFile( scmBucket, fileName1, updatedata, authorName );
        fileId2 = S3Utils.createFile( scmBucket, fileName2, filedata,
                authorName );
        S3Utils.createFile( scmBucket, fileName2, updatedata );
        S3Utils.createFile( scmBucket, fileName2, updatedata, authorName );
        fileId3 = S3Utils.createFile( scmBucket, fileName3, filedata );
    }

    @Test
    public void test() throws Exception {
        // test4829:指定当前版本列取文件(匹配author条件)
        List< String > expectFileNames = new LinkedList< String >();
        expectFileNames.add( fileName1 );
        expectFileNames.add( fileName2 );
        listInstanceByCurrentVersion( expectFileNames );

        // test4831:指定历史版本列取文件(匹配size条件)
        int historyVersionNum = 2;
        listInstanceByHistoryVersion( expectFileNames, historyVersionNum );

        // test4833：指定所有版本列取文件(匹配fildId条件)
        int allVersionNum = 6;
        expectFileNames.add( fileName3 );
        listInstanceByAllVersion( expectFileNames, allVersionNum );

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

    private void listInstanceByCurrentVersion( List< String > expectFileNames )
            throws ScmException {
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
        // exist 2 current version file
        int expFileNum = 2;
        Assert.assertEquals( size, expFileNum );
        Assert.assertEqualsNoOrder( actFileNames.toArray(),
                expectFileNames.toArray() );

    }

    private void listInstanceByHistoryVersion( List< String > expectFileNames,
            int fileVersionNum ) throws ScmException {
        BSONObject obj1 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId1.get() ).get();
        BSONObject obj2 = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileId2.get() ).get();
        BSONObject obj4 = ScmQueryBuilder.start().or( obj1, obj2 ).get();
        BSONObject obj3 = ScmQueryBuilder.start( ScmAttributeName.File.SIZE )
                .lessThan( updateSize ).get();
        BSONObject condition = ScmQueryBuilder.start().and( obj3, obj4 ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_HISTORY, condition );
        List< String > actFileNames = new ArrayList<>();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            actFileNames.add( file.getFileName() );
            size++;
        }
        cursor.close();

        List< String > actFileNamelist = actFileNames.stream().distinct()
                .collect( Collectors.toList() );
        Assert.assertEqualsNoOrder( actFileNamelist.toArray(),
                expectFileNames.toArray() );

        // exist 2 history version file
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
        List< String > actFileNames = new ArrayList<>();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            actFileNames.add( file.getFileName() );
            size++;
        }
        cursor.close();

        List< String > actFileNamelist = actFileNames.stream().distinct()
                .collect( Collectors.toList() );
        Assert.assertEqualsNoOrder( actFileNamelist.toArray(),
                expectFileNames.toArray() );

        // exist all version file
        Assert.assertEquals( size, versionNum,
                "query cond :" + condition.toString() );
    }
}
