package com.sequoiacm.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5748:移除全局Transition验证
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5748 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private ScmLifeCycleConfig config;
    private ScmWorkspace ws;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = ScmSessionUtils.createSession( rootSite );
        WsWrapper wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        LifeCycleUtils.cleanWsLifeCycleConfig( ws );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws ScmException {
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmLifeCycleTransition transition = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session ).get( 0 );

        // 已有过期使用
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                transition.getSource() );
        ScmFactory.Site.setSiteStageTag( session, branchSite.getSiteName(),
                transition.getDest() );
        ws.applyTransition( transition.getName() );
        try {
            ScmSystem.LifeCycleConfig.removeTransition( session,
                    transition.getName() );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }
        List< String > transitions = getAllTransition( session );
        Assert.assertTrue( transitions.contains( transition.getName() ) );

        // 没有工作区使用
        ws.removeTransition( transition.getName() );
        ScmSystem.LifeCycleConfig.removeTransition( session,
                transition.getName() );
        // 校验结果
        List< ScmLifeCycleTransition > actTransitions = ScmSystem.LifeCycleConfig
                .getTransitionConfig( session );
        List< ScmLifeCycleTransition > expTransitions = config
                .getTransitionConfig();
        actTransitions.add( transition );
        LifeCycleUtils.checkTransitionConfigByBson( actTransitions,
                expTransitions );

        // 移除不存在数据流
        try {
            ScmSystem.LifeCycleConfig.removeTransition( session,
                    transition.getName() );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanWsLifeCycleConfig( ws );
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

    private List< String > getAllTransition( ScmSession session )
            throws ScmException {
        List< ScmLifeCycleTransition > transitionConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session ).getTransitionConfig();
        List< String > transitions = new ArrayList<>();
        for ( ScmLifeCycleTransition transition : transitionConfig ) {
            transitions.add( transition.getName() );
        }
        return transitions;
    }
}