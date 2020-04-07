package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-2579:不指定排序分页列取文件列表
 * @author fanyu
 * @Date:2019年8月30日
 * @version:1.0
 */
public class ListScmFile2579 extends TestScmBase {
    private AtomicInteger expSuccessTestCount = new AtomicInteger( 0 );
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private int fileNum = 50;
    private List< String > fileIdList = new ArrayList<>();
    private String fileNamePrefix = "file2579";
    private BSONObject filter;
    private List< ScmFileBasicInfo > currFileList = new ArrayList<>();
    private List< ScmFileBasicInfo > histFileList = new ArrayList<>();

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
        byte[] bytes = new byte[ 2 ];
        Random random = new Random();
        random.nextBytes( bytes );
        //prepare file
        for ( int i = 0; i < fileNum; i++ ) {
            String fileName = fileNamePrefix + "-" + i;
            ScmFile scmFile = ScmFactory.File.createInstance( ws );
            scmFile.setAuthor( fileNamePrefix );
            scmFile.setTitle( fileName + "-" + ( fileNum - i ) );
            scmFile.setFileName( fileName );
            fileIdList.add( scmFile.save().get() );
        }
        //update file
        int count = 3;
        for ( int i = 0; i < fileIdList.size(); i = i + 2 ) {
            ScmFile file = ScmFactory.File
                    .getInstance( ws, new ScmId( fileIdList.get( i ) ) );
            //update Count
            for ( int j = 0; j < count; j++ ) {
                file.updateContent( new ByteArrayInputStream( bytes ) );
            }
        }
        //prepare filter
        filter = ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .in( fileIdList ).get();
        getInitScmFileInfo( ScmType.ScopeType.SCOPE_CURRENT, filter,
                currFileList );
        getInitScmFileInfo( ScmType.ScopeType.SCOPE_HISTORY, filter,
                histFileList );
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        return new Object[][] {
                //orderby:null
                { ScmType.ScopeType.SCOPE_CURRENT, filter, null, 0, 10,
                        currFileList },
                { ScmType.ScopeType.SCOPE_HISTORY, filter, null, 2, 5,
                        histFileList }
        };
    }

    @Test(dataProvider = "dataProvider")
    private void test( ScmType.ScopeType scopeType, BSONObject filter,
            BSONObject orderby,
            long skip, long limit, List< ScmFileBasicInfo > fileList )
            throws Exception {
        int actPageSize = 0;
        long tmpSkip = skip;
        int totalNum = 0;
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            while ( tmpSkip < fileList.size() ) {
                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                        .listInstance( ws, scopeType,
                                filter, orderby, tmpSkip, limit );
                int count = 0;
                while ( cursor.hasNext() ) {
                    ScmFileBasicInfo act = cursor.getNext();
                    try {
                        Assert.assertEquals(
                                act.getFileName().contains( fileNamePrefix ),
                                true );
                        count++;
                    } catch ( AssertionError e ) {
                        throw new Exception(
                                "filter = " + filter + ",orderby = " + orderby
                                        + ",skip = " + skip + ",limit = " +
                                        limit + "，act = " + act, e );
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
        int size = fileList.size();
        try {
            if ( skip < size && limit != 0 ) {
                Assert.assertEquals( totalNum, size - skip );
                if ( limit != -1 ) {
                    Assert.assertEquals( actPageSize, ( int ) Math
                            .ceil( ( ( double ) ( size - skip ) / limit ) ) );
                } else {
                    Assert.assertEquals( actPageSize, 1 );
                }
            } else {
                Assert.assertEquals( totalNum, 0, "orderby = " + orderby );
                Assert.assertEquals( actPageSize, 0 );
            }
        } catch ( AssertionError e ) {
            throw new Exception(
                    "filter = " + filter.toString() + ",orderby = " + orderby
                            + ",skip = " + skip + ",limit = " + limit, e );
        }
        expSuccessTestCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( expSuccessTestCount.get() == 2 || TestScmBase.forceClear ) {
                for ( String fileId : fileIdList ) {
                    ScmFactory.File
                            .deleteInstance( ws, new ScmId( fileId ), true );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void getInitScmFileInfo( ScmType.ScopeType scopeType,
            BSONObject filter, List< ScmFileBasicInfo > fileList )
            throws ScmException {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            cursor = ScmFactory.File.listInstance( ws, scopeType, filter );
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


