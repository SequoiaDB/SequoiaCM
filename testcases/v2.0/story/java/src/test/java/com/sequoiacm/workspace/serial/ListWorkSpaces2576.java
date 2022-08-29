package com.sequoiacm.workspace.serial;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ListUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-2576 :: 分页列取排序的工作区
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class ListWorkSpaces2576 extends TestScmBase {
    private AtomicInteger actSuccessTestCount = new AtomicInteger( 0 );
    private SiteWrapper site;
    private ScmSession session;
    private int wsNum = 3;
    private List< ScmWorkspaceInfo > wsList = new ArrayList<>();
    private List< String > wsNames = new ArrayList<>();
    private String wsNamePrefix = "ws2576";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        for ( int i = 0; i < wsNum; i++ ) {
            String wsName = wsNamePrefix + "-" + i;
            ScmWorkspaceUtil.deleteWs( wsName, session );
            ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
            wsNames.add( wsName );
        }
        ScmCursor< ScmWorkspaceInfo > cursor = ScmFactory.Workspace
                .listWorkspace( session );
        while ( cursor.hasNext() ) {
            wsList.add( cursor.getNext() );
        }
        cursor.close();
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        // 单个字段
        List< ScmWorkspaceInfo > positiveList = new ArrayList<>();
        positiveList.addAll( wsList );
        List< ScmWorkspaceInfo > negativeList = new ArrayList<>();
        negativeList.addAll( wsList );
        ListUtils.sort( positiveList, true, "name" );
        ListUtils.sort( negativeList, false, "name" );

        // 多个字段
        List< ScmWorkspaceInfo > dPositiveList = new ArrayList<>();
        dPositiveList.addAll( wsList );
        List< ScmWorkspaceInfo > dNegativeList = new ArrayList<>();
        dNegativeList.addAll( wsList );
        ListUtils.sort( dPositiveList, new String[] { "createTime", "name" },
                new boolean[] { true, false } );
        ListUtils.sort( dNegativeList, new String[] { "createTime", "name" },
                new boolean[] { false, true } );

        BSONObject positive = ScmQueryBuilder.start( "name" ).is( 1 ).get();
        BSONObject negative = ScmQueryBuilder.start( "name" ).is( -1 ).get();
        BSONObject dPositive = ScmQueryBuilder.start( "create_time" ).is( 1 )
                .and( "name" ).is( -1 ).get();
        BSONObject dNegative = ScmQueryBuilder.start( "create_time" ).is( -1 )
                .and( "name" ).is( 1 ).get();

        double expTotalNum = ( double ) wsList.size();
        return new Object[][] {
                // filter skip limit scmRoles expPageSize expTotalNum
                // orderby:positive
                { positive, 0, 1, positiveList,
                        ( int ) Math.ceil( expTotalNum / 1 ), expTotalNum },
                { positive, 1, 50, positiveList,
                        ( int ) Math.ceil( ( expTotalNum - 1 ) / 50 ),
                        expTotalNum - 1 },
                { positive, 3, 100, positiveList,
                        ( int ) Math.ceil( ( expTotalNum - 3 ) / 100 ),
                        expTotalNum - 3 },
                { positive, 0, -1, positiveList, 1, expTotalNum },
                { positive, 10, 0, positiveList, 0, 0 },
                // orderby:negative
                { negative, 0, 1, negativeList,
                        ( int ) Math.ceil( expTotalNum / 1 ), expTotalNum },
                { negative, 1, 50, negativeList,
                        ( int ) Math.ceil( ( expTotalNum - 1 ) / 50 ),
                        expTotalNum - 1 },
                { negative, 3, 100, negativeList,
                        ( int ) Math.ceil( ( expTotalNum - 3 ) / 100 ),
                        expTotalNum - 3 },
                { negative, 0, -1, negativeList, 1, expTotalNum },
                { negative, 10, 0, negativeList, 0, 0 },
                // orderby:多个字段
                { dPositive, 0, 10, dPositiveList,
                        ( int ) Math.ceil( expTotalNum / 10 ), expTotalNum },
                { dNegative, 0, 10, dNegativeList,
                        ( int ) Math.ceil( expTotalNum / 10 ), expTotalNum } };
    }

    @Test(groups = { GroupTags.base }, dataProvider = "dataProvider")
    private void test( BSONObject orderby, long skip, long limit,
            List< ScmWorkspaceInfo > wsList, int expPageSize,
            double expTotalNum ) throws Exception {
        int actPageSize = 0;
        long tmpSkip = skip;
        double totalNum = 0;
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            while ( tmpSkip < wsList.size() ) {
                ScmCursor< ScmWorkspaceInfo > cursor = ScmFactory.Workspace
                        .listWorkspace( session, orderby, tmpSkip, limit );
                int count = 0;
                while ( cursor.hasNext() ) {
                    ScmWorkspaceInfo info = cursor.getNext();
                    ScmWorkspaceInfo expWs = wsList
                            .get( ( int ) ( tmpSkip + count ) );
                    try {
                        Assert.assertEquals( info.getName(), expWs.getName() );
                        Assert.assertEquals( info.getDesc(), expWs.getDesc() );
                        Assert.assertEquals( info.getCreateTime(),
                                expWs.getCreateTime() );
                        count++;
                    } catch ( Exception e ) {
                        throw new Exception( "info = " + info.toString(), e );
                    }
                }
                cursor.close();
                if ( limit == 0 ) {
                    break;
                }
                if ( count == 0 ) {
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
        Assert.assertEquals( totalNum, expTotalNum, "orderby = " + orderby );
        Assert.assertEquals( actPageSize, expPageSize,
                "orderby = " + orderby + ",totalNum = " + totalNum );
        actSuccessTestCount.getAndIncrement();
    }

    @Test(groups = { GroupTags.base })
    private void testInvalid() throws Exception {
        try {
            ScmFactory.Workspace.listWorkspace( session, new BasicBSONObject(),
                    -1, 0 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        try {
            ScmFactory.Workspace.listWorkspace( session, new BasicBSONObject(),
                    0, -2 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        actSuccessTestCount.getAndIncrement();
    }

    @Test(groups = { GroupTags.base })
    private void testInvalidSession() throws Exception {
        try {
            ScmFactory.Workspace.listWorkspace( null, new BasicBSONObject(), -1,
                    0 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        ScmSession scmSession = TestScmTools.createSession( site );
        scmSession.close();
        try {
            ScmFactory.Workspace.listWorkspace( scmSession,
                    new BasicBSONObject(), 0, 0 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                throw e;
            }
        }
        actSuccessTestCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( actSuccessTestCount.get() == ( generateRangData().length + 2 )
                    || TestScmBase.forceClear ) {
                for ( String wsName : wsNames ) {
                    ScmWorkspaceUtil.deleteWs( wsName, session );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
