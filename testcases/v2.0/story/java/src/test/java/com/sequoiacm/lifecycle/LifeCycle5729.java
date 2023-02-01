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
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5729:设置生命周期全局配置与流信息验证
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5729 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmLifeCycleConfig config = new ScmLifeCycleConfig();
    private ScmLifeCycleConfig invalidConfig = new ScmLifeCycleConfig();

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        createLifeCycleConfig();
    }

    @Test
    public void test() throws ScmException {
        // 非法的流
        try {
            ScmSystem.LifeCycleConfig.setLifeCycleConfig( session,
                    invalidConfig );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_BAD_REQUEST ) ) {
                throw e;
            }
        }

        // 正确的流
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmLifeCycleConfig scmLifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( scmLifeCycleConfig,
                config );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

    private void createLifeCycleConfig() {
        // 构造热、温、冷三个标签
        List< ScmLifeCycleStageTag > scmLifeCycleStageTags = LifeCycleUtils
                .initScmLifeCycleStageTags();
        // 创建有效数据全局配置
        ScmLifeCycleTransition hot_warm = LifeCycleUtils
                .initScmLifeCycleTransition( "newHot_newWarm",
                        LifeCycleUtils.tagHot.getName(),
                        LifeCycleUtils.tagWarm.getName(),
                        new BasicBSONObject() );
        config.setStageTagConfig( scmLifeCycleStageTags );
        config.setTransitionConfig(
                LifeCycleUtils.buildScmLifeCycleTransitions( hot_warm ) );
        // 创建无效效数据流全局配置
        ScmLifeCycleTransition test = LifeCycleUtils.initScmLifeCycleTransition(
                "test_test", "test1", "test2", new BasicBSONObject() );
        invalidConfig.setStageTagConfig( scmLifeCycleStageTags );
        invalidConfig.setTransitionConfig(
                LifeCycleUtils.buildScmLifeCycleTransitions( test ) );
    }
}