package com.sequoiacm.lifecycle;

import java.util.List;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5730:设置生命周期全局配置与流属性字段验证
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5730 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private static String tagHot = LifeCycleUtils.tagHot.getName();
    private static String tagWarm = LifeCycleUtils.tagWarm.getName();
    private static String tagCold = LifeCycleUtils.tagCold.getName();
    private ScmLifeCycleConfig config = new ScmLifeCycleConfig();
    private static BSONObject query;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        query = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( "author5730" ).get();
    }

    @Test
    public void test() throws ScmException {
        // 创建站点标签
        List< ScmLifeCycleStageTag > scmLifeCycleStageTags = LifeCycleUtils
                .initScmLifeCycleStageTags();

        // 创建2个迁移数据流覆盖和2个延迟清理数据流，分别覆盖ALL、ANY，同时指定数据流NAME、查询条件、额外信息
        List< ScmLifeCycleTransition > scmLifeCycleTransitions = createScmLifeCycleTransitions();

        // 设置全局生命周期配置
        config.setStageTagConfig( scmLifeCycleStageTags );
        config.setTransitionConfig( scmLifeCycleTransitions );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );

        // 校验结果
        ScmLifeCycleConfig lifeCycleConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( lifeCycleConfig, config );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

    private static ScmLifeCycleTransition initScmLifeCycleTransition(
            String name, String source, String dest,
            ScmTransitionTriggers triggers, BSONObject query ) {
        ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
        transition.setName( name );
        transition.setSource( source );
        transition.setDest( dest );
        transition.setTransitionTriggers( triggers );
        transition.setMatcher( query.toString() );
        transition.setDataCheckLevel( CommonDefine.DataCheckLevel.STRICT );
        transition.setQuickStart( true );
        transition.setRecycleSpace( true );
        transition.setScope( "CURRENT" );
        return transition;
    }

    private static ScmLifeCycleTransition initScmLifeCycleTransition(
            String name, String source, String dest,
            ScmTransitionTriggers transitionTriggers,
            ScmCleanTriggers cleanTriggers, BSONObject query ) {
        ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
        transition.setName( name );
        transition.setSource( source );
        transition.setDest( dest );
        transition.setTransitionTriggers( transitionTriggers );
        transition.setCleanTriggers( cleanTriggers );
        transition.setMatcher( query.toString() );
        transition.setDataCheckLevel( CommonDefine.DataCheckLevel.STRICT );
        transition.setQuickStart( true );
        transition.setRecycleSpace( true );
        transition.setScope( "CURRENT" );
        return transition;
    }

    private static List< ScmLifeCycleTransition > createScmLifeCycleTransitions() {
        List< ScmTrigger > triggers = LifeCycleUtils
                .buildScmTriggers( LifeCycleUtils.initTrigger() );
        // 创建hot迁移并清理warm的数据流，触发器为ANY，指定name、匹配条件、额外任务信息
        ScmTransitionTriggers anyTransTriggers = LifeCycleUtils
                .initScmTransitionTriggers( "* * * * * ?", "ANY", 60000,
                        triggers );
        ScmLifeCycleTransition hot_warm = initScmLifeCycleTransition(
                "hotToWarm", tagHot, tagWarm, anyTransTriggers, query );

        // 创建warm迁移并清理cold的数据流，触发器为ALL，指定name、匹配条件、额外任务信息
        ScmTransitionTriggers allTransTriggers = LifeCycleUtils
                .initScmTransitionTriggers( "* * * * * ?", "ALL", 60000,
                        triggers );
        ScmLifeCycleTransition warm_cold = initScmLifeCycleTransition(
                "warmToCold", tagWarm, tagCold, allTransTriggers, query );

        // 创建tagHot迁移tagWarm并延迟清理的数据流，触发器为ANY，指定name、匹配条件、额外任务信息
        ScmCleanTriggers anyCleanTriggers = LifeCycleUtils
                .initScmCleanTriggers( "* * * * * ?", "ANY", 60000, triggers );
        ScmLifeCycleTransition clean_hot = initScmLifeCycleTransition(
                "cleanHot", tagHot, tagWarm, anyTransTriggers, anyCleanTriggers,
                query );

        // 创建warm迁移cold并延迟清理的数据流，触发器为ALL，指定name、匹配条件、额外任务信息
        ScmCleanTriggers allCleanTriggers = LifeCycleUtils
                .initScmCleanTriggers( "* * * * * ?", "ALL", 60000, triggers );
        ScmLifeCycleTransition clean_warm = initScmLifeCycleTransition(
                "cleanWarm", tagWarm, tagCold, allTransTriggers,
                allCleanTriggers, query );
        return LifeCycleUtils.buildScmLifeCycleTransitions( hot_warm, warm_cold,
                clean_warm, clean_hot );
    }
}