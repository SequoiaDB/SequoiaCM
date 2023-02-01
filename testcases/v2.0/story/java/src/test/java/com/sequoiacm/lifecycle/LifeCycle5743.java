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
 * @descreption SCM-5743:添加全局Transition中指定不同阶段标签
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5743 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmLifeCycleConfig config;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagCold = LifeCycleUtils.tagCold.getName();

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
    }

    @Test
    public void test() throws ScmException {
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );

        // 创建已存在标签数据流并添加
        ScmLifeCycleTransition hot_cold = LifeCycleUtils
                .initScmLifeCycleTransition( "hot_cold", tagHot, tagCold,
                        new BasicBSONObject() );
        ScmSystem.LifeCycleConfig.addTransition( session, hot_cold );

        // 校验添加成功
        ScmLifeCycleConfig actConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        List< ScmLifeCycleTransition > actTransitionConfig = actConfig
                .getTransitionConfig();
        List< ScmLifeCycleTransition > expTransitionConfig = config
                .getTransitionConfig();
        expTransitionConfig.add( hot_cold );
        LifeCycleUtils.checkTransitionConfigByBson( actTransitionConfig,
                expTransitionConfig );

        // 创建不存在标签的数据流
        String invalidTag = "invalid_5743";
        ScmLifeCycleTransition invalid = LifeCycleUtils
                .initScmLifeCycleTransition( "test_5743", invalidTag, tagCold,
                        new BasicBSONObject() );
        try {
            ScmSystem.LifeCycleConfig.addTransition( session, invalid );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_BAD_REQUEST ) ) {
                throw e;
            }
        }

        // 校验添加失败
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