package com.sequoiacm.auth;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description: SCM-1564 :: createRole参数校验
 * @author fanyu
 * @Date:2018年5月22日
 * @version:1.0
 */
public class AuthServer_Param_CreateRole1564 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
    }

    @Test
    private void testRoleNameExist() throws ScmException {
        String roleName = "CreateRole1564";
        ScmRole role = null;
        try {
            role = ScmFactory.Role.createRole( session, roleName, null );
            ScmFactory.Role.createRole( session, roleName, null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_BAD_REQUEST ) {
                throw e;
            }
        } finally {
            if ( role != null ) {
                ScmFactory.Role.deleteRole( session, role );
            }
        }
    }

    @Test
    private void testRoleIsNull() throws ScmException {
        String roleName = null;
        try {
            ScmFactory.Role.createRole( session, roleName, null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void test3() throws ScmException {
        String roleName = " Role1564 中文.!@#$*()_+::<>\"test";
        // 创建
        ScmFactory.Role.createRole( session, roleName, "" );

        // 获取
        ScmRole role = ScmFactory.Role.getRole( session, roleName );
        Assert.assertEquals( role.getRoleName(), "ROLE_" + roleName );

        // 删除
        ScmFactory.Role.deleteRole( session, roleName );
        try {
            ScmFactory.Role.getRole( session, roleName );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }

    @Test
    private void teste4() throws ScmException {
        String[] chars = { "/", "%", "\\", ";" };
        for ( String c : chars ) {
            try {
                ScmFactory.Role.createRole( session, "test " + c, "" );
                Assert.fail( "exp fail but act success!!! c = " + c );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                    throw e;
                }
            }
        }

        // 问题单SEQUOIACM-1003补充测试点
        String roleName = ".";
        try {
            ScmFactory.Role.createRole( session, roleName, "" );
            Assert.fail( "exp fail but act success!!! roleName = " + roleName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
