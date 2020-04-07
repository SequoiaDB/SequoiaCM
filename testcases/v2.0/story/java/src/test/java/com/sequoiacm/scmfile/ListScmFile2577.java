package com.sequoiacm.scmfile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ListUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2577 ::  SCM-2577:正逆序分页列取当前文件列表
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class ListScmFile2577 extends TestScmBase {
    private AtomicInteger expSuccessTestCount = new AtomicInteger( 0 );
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private int fileNum = 100;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String fileNamePrefix = "file2577";
    private BSONObject filter1;
    private BSONObject filter2;
    private BSONObject filter3;
    private BSONObject filter4;
    private List< ScmFileBasicInfo > fileList1 = new ArrayList<>();
    private List< ScmFileBasicInfo > fileList2 = new ArrayList<>();
    private List< ScmFileBasicInfo > fileList3 = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        //clean
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNamePrefix ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        //prepare file
        for ( int i = 0; i < fileNum; i++ ) {
            String fileName = fileNamePrefix + "-" + i;
            ScmFile scmFile = ScmFactory.File.createInstance( ws );
            scmFile.setAuthor( fileNamePrefix );
            scmFile.setTitle( fileName + "-" + ( fileNum - i ) );
            scmFile.setFileName( fileName );
            if ( i % 2 == 0 ) {
                scmFile.addTag( "transfer" );
            } else {
                scmFile.addTag( "tally" );
            }
            fileIdList.add( scmFile.save() );
        }
        //prepare filter
        //all file
        filter1 = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNamePrefix ).get();
        //part of the file
        filter2 = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNamePrefix )
                .and( ScmAttributeName.File.TAGS ).in( "tally" ).get();
        //one file
        filter3 = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNamePrefix )
                .and( ScmAttributeName.File.FILE_NAME )
                .is( fileNamePrefix + "-" + 50 ).get();
        //zero file
        filter4 = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNamePrefix )
                .and( ScmAttributeName.File.FILE_NAME )
                .is( fileNamePrefix + "1-" + 50 ).get();
        getInitScmFileInfo( filter1, fileList1 );
        getInitScmFileInfo( filter2, fileList2 );
        getInitScmFileInfo( filter3, fileList3 );
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        BSONObject positive = ScmQueryBuilder
                .start( ScmAttributeName.File.CREATE_TIME ).is( 1 ).get();
        BSONObject negative = ScmQueryBuilder
                .start( ScmAttributeName.File.CREATE_TIME ).is( -1 ).get();

        BSONObject dPositive = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( 1 )
                .and( ScmAttributeName.File.CREATE_TIME ).is( -1 ).get();
        BSONObject dNegative = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( -1 )
                .and( ScmAttributeName.File.CREATE_TIME ).is( 1 ).get();
        String[] sortNameAtr1 = new String[] { "createDate" };
        boolean[] typeAtr1 = new boolean[] { true };
        boolean[] typeAtr2 = new boolean[] { false };

        String[] sortNameAtr2 = new String[] { "fileName", "createDate" };
        boolean[] typeAtr3 = new boolean[] { true, false };
        boolean[] typeAtr4 = new boolean[] { false, true };

        return new Object[][] {
                //filter  skip   limit initScmFiles  sortNameAtr  typeArr
                //orderby:单个字段 正序
                //skip=0  limit=1
                { filter1, positive, 0, 1, fileList1, sortNameAtr1, typeAtr1 },
                //skip>0 limt=50
                { filter1, positive, 1, 50, fileList1, sortNameAtr1, typeAtr1 },
                //skip>10 limt=50
                { filter2, positive, 10, 100, fileList2, sortNameAtr1,
                        typeAtr1 },
                //skip == fileList.size
                { filter3, positive, 1, 10, fileList3, sortNameAtr1, typeAtr1 },
                //skip > fileList.size
                { filter4, positive, 1, 10, new ArrayList< ScmFileBasicInfo >(),
                        sortNameAtr1, typeAtr1 },
                //limit > fileList.size
                { filter2, positive, 0, fileList2.size() + 1, fileList2,
                        sortNameAtr1, typeAtr1 },
                //limit = -1
                { filter2, positive, 10, -1, fileList2, sortNameAtr1,
                        typeAtr1 },

                //orderby:单个字段 逆序
                { filter1, negative, 0, 1, fileList1, sortNameAtr1, typeAtr2 },
                //skip>10 limt=50
                { filter2, negative, 10, 100, fileList2, sortNameAtr1,
                        typeAtr2 },
                //skip == fileList.size
                { filter3, negative, 1, 10, fileList3, sortNameAtr1, typeAtr2 },
                //skip > fileList.size
                { filter4, negative, 1, 10, new ArrayList< ScmFileBasicInfo >(),
                        sortNameAtr1, typeAtr2 },
                //limit > fileList.size
                { filter2, negative, 0, fileList2.size() + 1, fileList2,
                        sortNameAtr1, typeAtr2 },
                //limit = -1
                { filter2, negative, 0, -1, fileList2, sortNameAtr1, typeAtr2 },

                //orderby:多个字段 正逆序
                { filter1, dPositive, 0, 10, fileList1, sortNameAtr2,
                        typeAtr3 },
                { filter1, dNegative, 2, 20, fileList1, sortNameAtr2, typeAtr4 }
        };
    }

    @Test(dataProvider = "dataProvider")
    private void test( BSONObject filter, BSONObject orderby, long skip,
            long limit, List< ScmFileBasicInfo > list
            , String[] sortnameArr, boolean[] typeArr ) throws Exception {
        List< ScmFileBasicInfo > tmpList = new ArrayList<>();
        tmpList.addAll( list );
        ListUtils.sort( tmpList, sortnameArr, typeArr );
        int actPageSize = 0;
        long tmpSkip = skip;
        int totalNum = 0;
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            while ( tmpSkip < tmpList.size() ) {
                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                        .listInstance( ws, ScmType.ScopeType.SCOPE_CURRENT,
                                filter, orderby, tmpSkip, limit );
                int count = 0;
                while ( cursor.hasNext() ) {
                    ScmFileBasicInfo act = cursor.getNext();
                    ScmFileBasicInfo exp = tmpList
                            .get( ( int ) ( tmpSkip + count ) );
                    try {
                        Assert.assertEquals( act.getFileName(),
                                exp.getFileName() );
                        Assert.assertEquals( act.getUser(), exp.getUser() );
                        Assert.assertEquals( act.getFileId(), exp.getFileId() );
                        Assert.assertEquals( act.getUser(), exp.getUser() );
                        Assert.assertEquals( act.getCreateDate(),
                                exp.getCreateDate() );
                        Assert.assertEquals( act.getMajorVersion(),
                                exp.getMajorVersion() );
                        Assert.assertEquals( act.getMinorVersion(),
                                exp.getMinorVersion() );
                        Assert.assertEquals( act.getMimeType(),
                                exp.getMimeType() );
                        count++;
                    } catch ( AssertionError e ) {
                        throw new Exception( "filter = " + filter.toString() +
                                ",orderby = " + orderby.toString()
                                + ",skip = " + skip + ",limit = " + limit +
                                "，act = " + act.toString() + ",exp = "
                                + exp.toString(), e );
                    }
                }
                if ( limit == 0 || count == 0 ) {
                    break;
                }
                tmpSkip += count;
                totalNum += count;
                actPageSize++;
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        int size = tmpList.size();
        try {
            if ( skip < tmpList.size() && limit != 0 ) {
                Assert.assertEquals( totalNum, size - skip );
                if ( limit != -1 ) {
                    Assert.assertEquals( actPageSize,
                            ( int ) Math.ceil( ( ( double ) size / limit ) ) );
                } else {
                    Assert.assertEquals( actPageSize, 1 );
                }
            } else {
                Assert.assertEquals( totalNum, 0 );
                Assert.assertEquals( actPageSize, 0 );
            }
        } catch ( AssertionError e ) {
            throw new Exception(
                    "filter = " + filter.toString() + ",orderby = " +
                            orderby.toString()
                            + ",skip = " + skip + ",limit = " + limit, e );
        }
        expSuccessTestCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( expSuccessTestCount.get() == 15 || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void getInitScmFileInfo( BSONObject filter,
            List< ScmFileBasicInfo > fileList ) throws ScmException {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            cursor = ScmFactory.File
                    .listInstance( ws, ScmType.ScopeType.SCOPE_CURRENT,
                            filter );
            while ( cursor.hasNext() ) {
                fileList.add( cursor.getNext() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }
}


