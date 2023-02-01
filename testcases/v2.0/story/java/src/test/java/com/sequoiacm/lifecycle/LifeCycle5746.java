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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5746:更新全局Transition中指定不同阶段标签
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5746 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmLifeCycleConfig config;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagCold = LifeCycleUtils.tagCold.getName();
    private ScmLifeCycleTransition hot_cold;
    private ScmLifeCycleTransition cold_hot;
    private ScmLifeCycleTransition invalid;
    private String transitionName = "trans5746";

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        createScmLifeCycleTransition();
    }

    @Test
    public void test() throws ScmException {
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmSystem.LifeCycleConfig.addTransition( session, hot_cold );
        // 全局阶段标签中存在
        ScmSystem.LifeCycleConfig.updateTransition( session, transitionName,
                cold_hot );

        // 校验结果
        List< ScmLifeCycleTransition > actTransitionConfig = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session );
        List< ScmLifeCycleTransition > expTransitionConfig = config
                .getTransitionConfig();
        expTransitionConfig.add( cold_hot );
        LifeCycleUtils.checkTransitionConfigByBson( actTransitionConfig,
                expTransitionConfig );

        // 全局阶段标签中不存在
        try {
            ScmSystem.LifeCycleConfig.updateTransition( session, transitionName,
                    invalid );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_BAD_REQUEST ) ) {
                throw e;
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

    private void createScmLifeCycleTransition() {
        hot_cold = LifeCycleUtils.initScmLifeCycleTransition( transitionName,
                tagHot, tagCold, new BasicBSONObject() );
        cold_hot = LifeCycleUtils.initScmLifeCycleTransition( transitionName,
                tagCold, tagHot, new BasicBSONObject() );
        invalid = LifeCycleUtils.initScmLifeCycleTransition( transitionName,
                "hot5746", "cold5746", new BasicBSONObject() );
    }
}