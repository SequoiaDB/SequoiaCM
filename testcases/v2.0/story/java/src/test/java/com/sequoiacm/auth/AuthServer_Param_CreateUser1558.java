package com.sequoiacm.auth;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
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
 * @Description:SCM-1558 :: createUser参数校验
 * @author fanyu
 * @Date:2018年5月22日
 * @version:1.0
 */
public class AuthServer_Param_CreateUser1558 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private ScmUser user;
    private String username = "CreateUser1558";
    private String passwd = "1558";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        user = ScmFactory.User.createUser( session, username,
                ScmUserPasswordType.LOCAL, passwd );
    }

    @Test
    private void testPasswdIsNull() throws ScmException {
        try {
            ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_BAD_REQUEST ) {
                throw e;
            }
        }
    }

    @Test
    private void testPasswdType() throws ScmException {
        try {
            ScmFactory.User.createUser( session, username, null, passwd );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test
    private void test3() throws ScmException {
        String username = " User1558 中文.!@#$*()_+::<>\"test";
        // 创建用户
        ScmFactory.User.createUser( session, username,
                ScmUserPasswordType.LOCAL, passwd );

        // 获取用户
        ScmUser user = ScmFactory.User.getUser( session, username );
        Assert.assertEquals( user.getUsername(), username );

        // 删除用户
        ScmFactory.User.deleteUser( session, username );
        try {
            ScmFactory.User.getUser( session, username );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }

    @Test
    private void test4() throws ScmException {
        String[] chars = { "/", "%", "\\", ";" };
        for ( String c : chars ) {
            try {
                ScmFactory.User.createUser( session, "test1558 " + c,
                        ScmUserPasswordType.LOCAL, passwd );
                Assert.fail( "exp fail but act success!!! c = " + c );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                    throw e;
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmFactory.User.deleteUser( session, user );
        if ( session != null ) {
            session.close();
        }
    }
}
