package com.sequoiacm.lifecycle;

import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;

/**
 * @descreption SCM-5749:不同用户移除全局Transition
 * @author ZhangYanan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5749 extends TestScmBase {
    private boolean runSuccess = false;
    private String user = "user5749";
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmLifeCycleConfig lifeCycleConfig = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );

        ScmAuthUtils.deleteUser( session, user );

        createUser();
        lifeCycleConfig = LifeCycleUtils.getDefaultScmLifeCycleConfig();

        LifeCycleUtils.cleanLifeCycleConfig( session );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                lifeCycleConfig );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 普通用户
        test1();

        // 管理员用户
        test2();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.deleteRole( session, user );
                ScmAuthUtils.deleteUser( session, user );
            }
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void test1() throws ScmException {
        ScmSession session = ScmSessionUtils.createSession( site, user, user );
        // 设置全局配置
        try {
            ScmSystem.LifeCycleConfig.deleteLifeCycleConfig( session );
            Assert.fail( "预期失败实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) {
                throw e;
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void test2() throws ScmException {
        // 设置全局配置
        ScmSystem.LifeCycleConfig.deleteLifeCycleConfig( session );
    }

    private void createUser() throws ScmException {
        // create user
        ScmUser scmUser = ScmFactory.User.createUser( session, user,
                ScmUserPasswordType.LOCAL, user );
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.createRole( session, user, "" );
        modifier.addRole( user );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }
}