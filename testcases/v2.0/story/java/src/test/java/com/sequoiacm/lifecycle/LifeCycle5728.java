package com.sequoiacm.lifecycle;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @descreption SCM-5728:不同用户设置生命周期全局配置
 * @author YiPan
 * @date 2023/1/17
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5728 extends TestScmBase {
    private ScmSession session;
    private ScmSession normalUserSession;
    private String username = "user5728";
    private String passwd = "pwd5728";
    private SiteWrapper rootSite;
    private ScmLifeCycleConfig config;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ScmAuthUtils.createUser( session, username, passwd );
        normalUserSession = ScmSessionUtils.createSession( rootSite, username,
                passwd );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        LifeCycleUtils.cleanLifeCycleConfig( session );
    }

    @Test
    public void test() throws ScmException {
        // 普通用户设置
        try {
            ScmSystem.LifeCycleConfig.setLifeCycleConfig( normalUserSession,
                    config );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_UNAUTHORIZED ) ) {
                throw e;
            }
        }

        // 管理员用户设置
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmLifeCycleConfig lifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( lifeCycleConfig, config );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmAuthUtils.deleteUser( session, username );
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
            normalUserSession.close();
        }
    }
}