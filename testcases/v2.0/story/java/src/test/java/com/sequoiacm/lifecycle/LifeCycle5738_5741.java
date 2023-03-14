package com.sequoiacm.lifecycle;

import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @descreption SCM-5738:不同用户查询生命周期全局配置 SCM-5741:不同用户移除全局阶段标签
 * @author YiPan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5738_5741 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private ScmSession normalUserSession;
    private String username = "user5738";
    private String passwd = "pwd5738";
    private String newTag = "test5738";
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
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        // 管理员用户查询全局配置
        ScmLifeCycleConfig lifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( lifeCycleConfig, config );

        // 普通用户查询全局配置
        lifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( normalUserSession );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( lifeCycleConfig, config );

        // 添加标签
        ScmSystem.LifeCycleConfig.addStageTag( session, newTag, newTag );

        // 普通用户移除标签
        try {
            ScmSystem.LifeCycleConfig.removeStageTag( normalUserSession,
                    newTag );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_UNAUTHORIZED ) ) {
                throw e;
            }
        }

        // 管理员用户移除标签
        ScmSystem.LifeCycleConfig.removeStageTag( session, newTag );
        lifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( normalUserSession );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( lifeCycleConfig, config );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
            normalUserSession.close();
        }
    }
}