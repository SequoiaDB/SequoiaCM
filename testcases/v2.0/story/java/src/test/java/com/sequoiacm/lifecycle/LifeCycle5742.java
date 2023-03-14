package com.sequoiacm.lifecycle;

import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.exception.ScmError;
import org.bson.BasicBSONObject;
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

import java.util.List;

/**
 * @descreption SCM-5742:添加全局Transition验证
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5742 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmLifeCycleConfig config;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagCold = LifeCycleUtils.tagCold.getName();

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
    }

    @Test
    public void test() throws ScmException {
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );

        // 添加已存在数据流
        ScmLifeCycleTransition transition = config.getTransitionConfig()
                .get( 0 );
        try {
            ScmSystem.LifeCycleConfig.addTransition( session, transition );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 添加不存在数据流
        ScmLifeCycleTransition hot_cold = LifeCycleUtils
                .initScmLifeCycleTransition( "hot_cold", tagHot, tagCold,
                        new BasicBSONObject() );
        ScmSystem.LifeCycleConfig.addTransition( session, hot_cold );

        ScmLifeCycleConfig actConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        List< ScmLifeCycleTransition > actTransitionConfig = actConfig
                .getTransitionConfig();
        List< ScmLifeCycleTransition > expTransitionConfig = config
                .getTransitionConfig();
        expTransitionConfig.add( hot_cold );
        LifeCycleUtils.checkTransitionConfigByBson( actTransitionConfig,
                expTransitionConfig );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }
}