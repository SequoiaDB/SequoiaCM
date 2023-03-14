package com.sequoiacm.lifecycle;

import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @descreption SCM-5744:不同用户添加全局Transition SCM-5747:不同用户更新全局Transition
 * @author YiPan
 * @date 2023/1/19
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5744_5747 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmSession normalUserSession;
    private String username = "user5744";
    private String passwd = "pwd5744";
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagCold = LifeCycleUtils.tagCold.getName();
    private List< ScmLifeCycleTransition > expTransitionConfig;
    private ScmLifeCycleTransition hot_cold;
    private ScmLifeCycleTransition cold_hot;
    private String transitionName = "trans5744";
    private ScmLifeCycleConfig config;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ScmAuthUtils.createUser( session, username, passwd );
        normalUserSession = ScmSessionUtils.createSession( rootSite, username,
                passwd );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        expTransitionConfig = config.getTransitionConfig();
        LifeCycleUtils.cleanLifeCycleConfig( session );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        createScmLifeCycleTransition();
    }

    @Test
    public void test() throws ScmException {
        // 普通用户添加数据流
        try {
            ScmSystem.LifeCycleConfig.addTransition( normalUserSession,
                    hot_cold );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_UNAUTHORIZED ) ) {
                throw e;
            }
        }

        // 管理员用户添加数据流
        ScmSystem.LifeCycleConfig.addTransition( session, hot_cold );
        // 构造预期结果校验
        List< ScmLifeCycleTransition > actTransitionConfig = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session );
        expTransitionConfig.add( hot_cold );
        LifeCycleUtils.checkTransitionConfigByBson( actTransitionConfig,
                expTransitionConfig );
        expTransitionConfig.remove( hot_cold );

        // 普通用户更新数据流
        try {
            ScmSystem.LifeCycleConfig.updateTransition( normalUserSession,
                    transitionName, cold_hot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_UNAUTHORIZED ) ) {
                throw e;
            }
        }

        // 管理员用户更新数据流
        ScmSystem.LifeCycleConfig.updateTransition( session, transitionName,
                cold_hot );
        // 构造预期结果校验
        expTransitionConfig.add( cold_hot );
        actTransitionConfig = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session );
        LifeCycleUtils.checkTransitionConfigByBson( actTransitionConfig,
                expTransitionConfig );
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

    private void createScmLifeCycleTransition() {
        hot_cold = LifeCycleUtils.initScmLifeCycleTransition( transitionName,
                tagHot, tagCold, new BasicBSONObject() );
        cold_hot = LifeCycleUtils.initScmLifeCycleTransition( transitionName,
                tagCold, tagHot, new BasicBSONObject() );
    }
}