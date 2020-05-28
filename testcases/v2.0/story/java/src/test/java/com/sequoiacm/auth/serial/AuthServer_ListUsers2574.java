package com.sequoiacm.auth.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-2574:分页列取用户列表
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class AuthServer_ListUsers2574 extends TestScmBase {
    private AtomicInteger actSuccessTestCount = new AtomicInteger( 0 );
    private int expSuccessTestCount = 8;
    private SiteWrapper site;
    private ScmSession session;
    private int userNum = 100;
    private List< String > userNames = new ArrayList<>();
    private String userNamePrefix = "user2574";
    private String roleName = "role2574";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmRole scmRole = null;
        try {
            ScmFactory.Role.deleteRole( session, roleName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        scmRole = ScmFactory.Role.createRole( session, roleName, roleName );
        for ( int i = 0; i < userNum; i++ ) {
            String userName = userNamePrefix + "-" + i;
            try {
                ScmFactory.User.deleteUser( session, userName );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    throw e;
                }
            }
            ScmUser scmUser = ScmFactory.User.createUser( session, userName,
                    ScmUserPasswordType.LOCAL, userName );
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRole( scmRole );
            ScmFactory.User.alterUser( session, scmUser, modifier );
            userNames.add( userName );
        }
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateRangData() throws Exception {
        BSONObject cond1 = ScmQueryBuilder
                .start( ScmAttributeName.User.ENABLED ).is( true )
                .and( ScmAttributeName.User.HAS_ROLE ).is( roleName ).get();
        BSONObject cond2 = ScmQueryBuilder
                .start( ScmAttributeName.User.ENABLED ).is( true )
                .and( ScmAttributeName.User.HAS_ROLE ).is( roleName + "1" )
                .get();
        return new Object[][] {
                // filter skip limit expPageSize expTotalRecord
                // fileter:null
                { cond1, 0, 1, ( int ) Math.ceil( ( double ) userNum / 1 ),
                        userNum },
                // skip:1
                { cond1, 1, 50,
                        ( int ) Math.ceil( ( ( double ) userNum - 1 ) / 50 ),
                        userNum - 1 },
                // limit:0
                { cond1, 10, 0, 0, 0 },
                // limit:-1
                { cond1, 0, -1, 1, userNum },
                // limit:100
                { cond2, 10, 100, 0, 0 },
                // fileter:cond2
                { cond2, 100, 100, 0, 0 } };
    }

    @Test(dataProvider = "dataProvider")
    private void test( BSONObject filter, long skip, long limit,
            int expPageSize, int expTotalNum ) throws Exception {
        int actPageSize = 0;
        long tmpSkip = skip;
        int totalNum = 0;
        while ( tmpSkip < userNum ) {
            ScmCursor< ScmUser > cursor = ScmFactory.User.listUsers( session,
                    filter, tmpSkip, limit );
            int count = 0;
            while ( cursor.hasNext() ) {
                ScmUser info = cursor.getNext();
                try {
                    Assert.assertEquals(
                            info.getUsername().contains( userNamePrefix ),
                            true );
                    Assert.assertEquals( info.hasRole( roleName ), true );
                    Assert.assertEquals( info.getPasswordType(),
                            ScmUserPasswordType.LOCAL );
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
        Assert.assertEquals( totalNum, expTotalNum, "filter = " + filter );
        Assert.assertEquals( actPageSize, expPageSize,
                "filter = " + filter + ",totalNum = " + totalNum );
        actSuccessTestCount.getAndIncrement();
    }

    @Test
    private void testInvalid() throws Exception {
        try {
            ScmFactory.User.listUsers( session, new BasicBSONObject(), -1, 0 );
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
            ScmFactory.User.listUsers( null, new BasicBSONObject(), 1, 0 );
            Assert.fail( "exp failed but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        ScmSession scmSession = TestScmTools.createSession( site );
        scmSession.close();
        try {
            ScmFactory.User.listUsers( scmSession, new BasicBSONObject(), 0,
                    1 );
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
                for ( String userName : userNames ) {
                    ScmFactory.User.deleteUser( session, userName );
                }
                ScmFactory.Role.deleteRole( session, roleName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
