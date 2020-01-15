package com.sequoiacm.auth.serial;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ListUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: SCM-2575 :: 分页列取排序的角色
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class AuthServer_ListRoles2575 extends TestScmBase {
    private AtomicInteger actSuccessTestCount = new AtomicInteger( 0 );
    private int expSuccessTestCount = 10;
    private SiteWrapper site;
    private ScmSession session;
    private int roleNum = 1000;
    private List<String> roleNames = new ArrayList<>();
    private List<ScmRole> scmRoles = new ArrayList<>();
    private String roleNamePrefix = "role2575";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        for ( int i = 0; i < roleNum; i++ ) {
            String roleName = roleNamePrefix + "-" + i;
            try {
                ScmFactory.Role.deleteRole( session, roleName );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    throw e;
                }
            }
            ScmFactory.Role.createRole( session, roleName, roleName );
            roleNames.add( roleName );
        }
        ScmCursor<ScmRole> cursor = ScmFactory.Role.listRoles( session );
        while ( cursor.hasNext() ) {
            scmRoles.add( cursor.getNext() );
        }
        cursor.close();
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        List<ScmRole> positiveList = new ArrayList<>();
        positiveList.addAll( scmRoles );
        List<ScmRole> negativeList = new ArrayList<>();
        negativeList.addAll( scmRoles );
        ListUtils.sort( positiveList, true, "roleName" );
        ListUtils.sort( negativeList, false, "roleName" );

        BSONObject positive = ScmQueryBuilder.start( "roleName" ).is( 1 ).get();
        BSONObject negative = ScmQueryBuilder.start( "roleName" ).is( -1 )
                .get();
        double expTotalNum = scmRoles.size();
        return new Object[][] {
                //filter  skip   limit scmRoles  expPageSize expTotalNum
                //orderby:positive
                { positive, 0, 1, positiveList,
                        ( int ) Math.ceil( expTotalNum / 1 ), expTotalNum },
                { positive, 1, 50, positiveList,
                        ( int ) Math.ceil( ( ( expTotalNum - 1 ) ) / 50 ),
                        expTotalNum - 1 }, { positive, 30, 100, positiveList,
                ( int ) Math.ceil( ( ( expTotalNum - 30 ) ) / 100 ),
                expTotalNum - 30 }, { positive, 10, 0, positiveList, 0, 0 },
                { positive, 5, -1, positiveList, 1, expTotalNum - 5 },
                //orderby:negative
                { negative, 0, 1, negativeList,
                        ( int ) Math.ceil( ( expTotalNum ) / 1 ), expTotalNum },
                { negative, 1, 50, negativeList,
                        ( int ) Math.ceil( ( ( expTotalNum - 1 ) ) / 50 ),
                        expTotalNum - 1 }, { negative, 10, 100, negativeList,
                ( int ) Math.ceil( ( ( expTotalNum - 10 ) ) / 100 ),
                expTotalNum - 10 }, { negative, 10, 0, negativeList, 0, 0 },
                { negative, 5, -1, negativeList, 1, expTotalNum - 5 } };
    }

    @Test(dataProvider = "dataProvider")
    private void test( BSONObject orderby, long skip, long limit,
            List<ScmRole> expScmRoles, int expPageSize, double expTotalNum )
            throws Exception {
        int actPageSize = 0;
        long tmpSkip = skip;
        double totalNum = 0;
        while ( tmpSkip < expScmRoles.size() ) {
            ScmCursor<ScmRole> cursor = ScmFactory.Role
                    .listRoles( session, orderby, tmpSkip, limit );
            int count = 0;
            while ( cursor.hasNext() ) {
                ScmRole info = cursor.getNext();
                ScmRole expRole = expScmRoles
                        .get( ( int ) ( tmpSkip + count ) );
                try {
                    Assert.assertEquals( info.getRoleName(),
                            expRole.getRoleName() );
                    Assert.assertEquals( info.getDescription(),
                            expRole.getDescription() );
                    count++;
                } catch ( Exception e ) {
                    throw new Exception( "user = " + info.toString(), e );
                }
            }
            cursor.close();
            if ( limit == 0 || count == 0 ) {
                break;
            }
            tmpSkip += count;
            totalNum += count;
            actPageSize++;
        }
        Assert.assertEquals( totalNum, expTotalNum, "orderby = " + orderby );
        Assert.assertEquals( actPageSize, expPageSize,
                "orderby = " + orderby + ",totalNum = " + totalNum );
        actSuccessTestCount.getAndIncrement();
    }

    @Test
    private void testInvalid() throws Exception {
        try {
            ScmFactory.Role.listRoles( session, new BasicBSONObject(), -1, 0 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        try {
            ScmFactory.User.listUsers( session, new BasicBSONObject(), 0, -2 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        actSuccessTestCount.getAndIncrement();
    }

    @Test
    private void testInvalidSession() throws Exception {
        try {
            ScmFactory.Role.listRoles( null, new BasicBSONObject(), -1, 0 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        ScmSession scmSession = TestScmTools.createSession( site );
        scmSession.close();
        try {
            ScmFactory.User
                    .listUsers( scmSession, new BasicBSONObject(), 0, 1 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                throw e;
            }
        }
        actSuccessTestCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( actSuccessTestCount.get() == expSuccessTestCount
                    || TestScmBase.forceClear ) {
                for ( String roleName : roleNames ) {
                    ScmFactory.Role.deleteRole( session, roleName );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}


