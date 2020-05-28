package com.sequoiacm.auth.concurrent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description: SCM-1553 :: 并发创建、查询、删除不同角色
 * @author fanyu
 * @Date:2018年5月22日
 * @version:1.0
 */
public class AuthServer_CrudDiffRole1553 extends TestScmBase {
    private boolean runSuccess;
    private SiteWrapper site;
    private ScmSession session;
    private int roleNum = 3;
    private String roleName = "CrudDiffRole1553";
    private List< ScmRole > roleList = new CopyOnWriteArrayList< ScmRole >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            for ( int i = 0; i < roleNum; i++ ) {
                try {
                    ScmFactory.Role.deleteRole( session, roleName + "_" + i );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                        e.printStackTrace();
                        Assert.fail( e.getMessage() );
                    }
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        try {
            for ( int i = 0; i < roleNum; i++ ) {
                ScmRole role = ScmFactory.Role.createRole( session,
                        roleName + "_" + i, null );
                roleList.add( role );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        CreateRole cThread = new CreateRole();
        DeleteRole dThread = new DeleteRole();
        QueryRole qThread = new QueryRole();
        cThread.start();
        qThread.start();
        dThread.start();
        boolean cflag = cThread.isSuccess();
        boolean qflag = qThread.isSuccess();
        boolean dflag = dThread.isSuccess();
        Assert.assertEquals( dflag, true, dThread.getErrorMsg() );
        Assert.assertEquals( qflag, true, qThread.getErrorMsg() );
        Assert.assertEquals( cflag, true, cThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 1; i < roleNum + 1; i++ ) {
                    ScmFactory.Role.deleteRole( session, roleList.get( i ) );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class CreateRole extends TestThreadBase {
        @Override
        public void exec() {
            try {
                String rolename1 = roleName + "_" + UUID.randomUUID();
                ScmRole role = ScmFactory.Role.createRole( session, rolename1,
                        null );
                roleList.add( role );
                check( rolename1, role );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( String roleName, ScmRole role ) {
            ScmRole actRole;
            try {
                actRole = ScmFactory.Role.getRole( session, roleName );
                // Assert.assertEquals(actRole.getDescription(), role
                // .getDescription());
                Assert.assertEquals( actRole.getRoleId(), role.getRoleId() );
                Assert.assertEquals( actRole.getRoleName(),
                        role.getRoleName() );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class DeleteRole extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmFactory.Role.deleteRole( session, roleList.get( 0 ) );
                check( roleList.get( 0 ) );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( ScmRole role ) {
            try {
                ScmFactory.Role.getRole( session, role.getRoleName() );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private class QueryRole extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmRole actrole = ScmFactory.Role.getRole( session,
                        roleList.get( 1 ).getRoleName() );
                check( actrole, roleList.get( 1 ) );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( ScmRole actrole, ScmRole role ) {
            // Assert.assertEquals(actrole.getDescription(), role
            // .getDescription());
            Assert.assertEquals( actrole.getRoleId(), role.getRoleId() );
            Assert.assertEquals( actrole.getRoleName(), role.getRoleName() );
        }
    }
}