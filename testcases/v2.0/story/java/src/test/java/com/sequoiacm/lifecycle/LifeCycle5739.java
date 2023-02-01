package com.sequoiacm.lifecycle;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5739:添加全局阶段标签验证
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5739 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmLifeCycleConfig config;
    private static String hot = LifeCycleUtils.tagHot.getName();
    private static String defaultHot = "Hot";
    private static String newHot = "hot5739";

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
    }

    @Test
    public void test() throws ScmException {
        // 站点标签与内标签重复
        try {
            ScmSystem.LifeCycleConfig.addStageTag( session, defaultHot,
                    defaultHot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_BAD_REQUEST ) ) {
                throw e;
            }
        }

        // 站点标签与已有标签重复
        try {
            ScmSystem.LifeCycleConfig.addStageTag( session, hot, hot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_BAD_REQUEST ) ) {
                throw e;
            }
        }

        // 标签不存在于全局配置中
        ScmSystem.LifeCycleConfig.addStageTag( session, newHot, newHot );
        ScmLifeCycleConfig actConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );

        // 校验结果
        List< ScmLifeCycleStageTag > actStageTag = actConfig
                .getStageTagConfig();
        List< ScmLifeCycleStageTag > expStageTag = config.getStageTagConfig();
        expStageTag.add( new ScmLifeCycleStageTag( newHot, newHot ) );
        LifeCycleUtils.checkStageTagConfig( actStageTag, expStageTag );
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