package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.lifecycle.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;

/**
 * @author
 * @descreption
 * @date
 * @updateUser
 * @updateDate 2023/1/16
 * @updateRemark
 */
public class LifeCycleUtils {
    public final static ScmLifeCycleStageTag tagHot = new ScmLifeCycleStageTag(
            "newHot", "热" );
    public final static ScmLifeCycleStageTag tagWarm = new ScmLifeCycleStageTag(
            "newWarm", "温" );
    public final static ScmLifeCycleStageTag tagCold = new ScmLifeCycleStageTag(
            "NewCold", "冷" );
    public final static String hotWarmName = "newHot_newWarm";
    public final static String warmColdName = "newWarm_NewCold";

    /**
     * @descreption 组装默认热-温-冷全局配置，包含热-温迁移、温-冷迁移事件流
     * @return
     */
    public static ScmLifeCycleConfig getDefaultScmLifeCycleConfig() {
        ScmLifeCycleConfig config = new ScmLifeCycleConfig();
        config.setStageTagConfig( initScmLifeCycleStageTags() );
        config.setTransitionConfig( initScmLifeCycleTransitions() );
        return config;
    }

    /**
     * @descreption 组装标签
     * @param lifeCycleStageTags
     * @return
     */
    public static List< ScmLifeCycleStageTag > buildScmLifeCycleStageTags(
            ScmLifeCycleStageTag... lifeCycleStageTags ) {
        List< ScmLifeCycleStageTag > scmLifeCycleStageTags = new ArrayList<>();
        Collections.addAll( scmLifeCycleStageTags, lifeCycleStageTags );
        return scmLifeCycleStageTags;
    }

    /**
     * @descreption 组装事件流
     * @param lifeCycleTransition
     * @return
     */
    public static List< ScmLifeCycleTransition > buildScmLifeCycleTransitions(
            ScmLifeCycleTransition... lifeCycleTransition ) {
        List< ScmLifeCycleTransition > scmLifeCycleTransitions = new ArrayList<>();
        Collections.addAll( scmLifeCycleTransitions, lifeCycleTransition );
        return scmLifeCycleTransitions;
    }

    /**
     * @descreption 组装时间触发器
     * @param scmTriggers
     * @return
     */
    public static List< ScmTrigger > buildScmTriggers(
            ScmTrigger... scmTriggers ) {
        List< ScmTrigger > triggers = new ArrayList<>();
        Collections.addAll( triggers, scmTriggers );
        return triggers;
    }

    /**
     * @descreption 构造默认热、温、冷标签
     * @return
     */
    public static List< ScmLifeCycleStageTag > initScmLifeCycleStageTags() {
        return buildScmLifeCycleStageTags( tagHot, tagWarm, tagCold );
    }

    /**
     * @descreption 构造默认热-温、温-冷事件流
     * @return
     */
    public static List< ScmLifeCycleTransition > initScmLifeCycleTransitions() {
        ScmLifeCycleTransition hot_warm = initScmLifeCycleTransition(
                hotWarmName, tagHot.getName(), tagWarm.getName(),
                new BasicBSONObject() );
        ScmLifeCycleTransition warm_cold = initScmLifeCycleTransition(
                warmColdName, tagWarm.getName(), tagCold.getName(),
                new BasicBSONObject() );
        return buildScmLifeCycleTransitions( hot_warm, warm_cold );
    }

    /**
     * @descreption 构造默认时间触发器
     * @return
     */
    public static ScmTrigger initTrigger() {
        ScmTrigger trigger = new ScmTrigger();
        trigger.setID( "1" );
        trigger.setMode( "ANY" );
        trigger.setBuildTime( "1d" );
        trigger.setCreateTime( "30d" );
        trigger.setTransitionTime( "3d" );
        trigger.setLastAccessTime( "3d" );
        trigger.setBuildTime( "30d" );
        return trigger;
    }

    /**
     * @descreption 构造自定义时间触发器
     * @param id
     *            TriggerId
     * @param mode
     *            触发模式，可选值为ANY(满足任意条件触发)、ALL（满足所有条件触发）
     * @param buildTime
     *            文件在源站点的最长创建时间，超过这个时间则触发
     * @param createTime
     *            文件的最长创建时间，超过这个时间则触发
     * @param transitionTime
     *            文件在迁移后在目标站点存在的最长停留时间，超过这个时间则触发
     * @param lastAccessTime
     *            文件在源站点的最近访问时间，超过这个时间则触发
     * @param isTransferTrigger
     *            是否构造迁移流触发器，为false则buildTime和createTime不会生效，返回清理流触发器
     * @return
     */
    public static ScmTrigger initTrigger( String id, String mode,
            String buildTime, String createTime, String transitionTime,
            String lastAccessTime, boolean isTransferTrigger ) {
        ScmTrigger trigger = new ScmTrigger();
        trigger.setID( id );
        trigger.setMode( mode );
        if ( isTransferTrigger ) {
            trigger.setBuildTime( buildTime );
            trigger.setCreateTime( createTime );
        } else {
            trigger.setTransitionTime( transitionTime );
        }
        trigger.setLastAccessTime( lastAccessTime );
        return trigger;
    }

    /**
     * @descreption 构造默认Transition
     * @return
     */
    public static ScmLifeCycleTransition initScmLifeCycleTransition(
            String name, String source, String dest,
            BasicBSONObject bsonObject ) {
        ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
        transition.setName( name );
        transition.setSource( source );
        transition.setDest( dest );
        transition.setTransitionTriggers( initScmTransitionTriggers() );
        transition.setMatcher( bsonObject.toString() );
        transition.setDataCheckLevel( CommonDefine.DataCheckLevel.STRICT );
        transition.setQuickStart( true );
        transition.setRecycleSpace( true );
        transition.setScope( "CURRENT" );
        return transition;
    }

    /**
     * @descreption 构造自定义Transition
     * @param name
     *            Transition名
     * @param source
     *            数据流的源站点阶段标签信息
     * @param dest
     *            数据流的目标站点阶段标签信息
     * @param transitionTriggers
     *            数据流转触发规则
     * @param cleanTriggers
     *            延迟清理的触发规则，不指定则创建的迁移清理任务，指定则根据transitionTriggers和cleanTriggers创建对应的迁移任务和清理任务
     * @param bsonObject
     *            文件查询条件
     * @param strict
     *            文件校验模式支持指定week、strict
     * @param isQuickStart
     *            是否快启动
     * @param isRecycleSpace
     *            是否空间回收
     * @param scope
     *            指定文件版本，支持指定ALL、CURRENT、HISTORY
     * @return transition
     */
    public static ScmLifeCycleTransition initScmLifeCycleTransition(
            String name, String source, String dest,
            ScmTransitionTriggers transitionTriggers,
            ScmCleanTriggers cleanTriggers, BSONObject bsonObject,
            String strict, boolean isQuickStart, boolean isRecycleSpace,
            String scope ) {
        ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
        transition.setName( name );
        transition.setSource( source );
        transition.setDest( dest );
        transition.setTransitionTriggers( transitionTriggers );
        if ( cleanTriggers != null ) {
            transition.setCleanTriggers( cleanTriggers );
        }
        transition.setMatcher( bsonObject.toString() );
        transition.setDataCheckLevel( strict );
        transition.setQuickStart( isQuickStart );
        transition.setRecycleSpace( isRecycleSpace );
        transition.setScope( scope );
        return transition;
    }

    /**
     * @descreption 构造自定义TransitionTriggers
     * @param rule
     *            corn表达式
     * @param mode
     *            数据流的源站点阶段标签信息
     * @param maxExecTime
     *            最大执行时间，单位ms
     * @param triggers
     *            数据流转触发规则
     * @return scmTransitionTriggers
     */
    public static ScmTransitionTriggers initScmTransitionTriggers( String rule,
            String mode, long maxExecTime, List< ScmTrigger > triggers ) {
        // 创建ScmTransitionTriggers
        ScmTransitionTriggers scmTransitionTriggers = new ScmTransitionTriggers();
        scmTransitionTriggers.setRule( rule );
        scmTransitionTriggers.setMode( mode );
        scmTransitionTriggers.setMaxExecTime( maxExecTime );
        scmTransitionTriggers.setTriggerList( triggers );
        return scmTransitionTriggers;
    }

    /**
     * @descreption 构造自定义ClanTriggers
     * @param rule
     *            corn表达式
     * @param mode
     *            数据流的源站点阶段标签信息
     * @param maxExecTime
     *            最大执行时间，单位ms
     * @param triggers
     *            数据流转触发规则
     * @return scmTransitionTriggers
     */
    public static ScmCleanTriggers initScmCleanTriggers( String rule,
            String mode, long maxExecTime, List< ScmTrigger > triggers ) {
        // 创建ScmCleanTriggers
        ScmCleanTriggers scmCleanTriggers = new ScmCleanTriggers();
        scmCleanTriggers.setRule( rule );
        scmCleanTriggers.setMode( mode );
        scmCleanTriggers.setMaxExecTime( maxExecTime );
        scmCleanTriggers.setTriggerList( triggers );
        return scmCleanTriggers;
    }

    public static ScmTransitionTriggers initScmTransitionTriggers() {
        // 创建ScmTransitionTriggers
        ScmTransitionTriggers scmTransitionTriggers = new ScmTransitionTriggers();
        scmTransitionTriggers.setRule( "* * * * * ?" );
        scmTransitionTriggers.setMode( "ANY" );
        scmTransitionTriggers.setMaxExecTime( 60000 );
        scmTransitionTriggers
                .setTriggerList( buildScmTriggers( initTrigger() ) );
        return scmTransitionTriggers;
    }

    /**
     * @descreption 清理全局配置
     * @param session
     * @throws ScmException
     */
    public static void cleanLifeCycleConfig( ScmSession session )
            throws ScmException {
        List< SiteWrapper > allSites = ScmInfo.getAllSites();
        for ( SiteWrapper site : allSites ) {
            ScmFactory.Site.unsetSiteStageTag( session, site.getSiteName() );
        }
        ScmSystem.LifeCycleConfig.deleteLifeCycleConfig( session );
    }

    /**
     * @descreption 清理工作区下所有数据流
     * @param ws
     * @throws ScmException
     */
    public static void cleanWsLifeCycleConfig( ScmWorkspace ws )
            throws ScmException {
        List< ScmTransitionSchedule > scmTransitionSchedules = ws
                .listTransition();
        for ( ScmTransitionSchedule scmTransitionSchedule : scmTransitionSchedules ) {
            ws.removeTransition(
                    scmTransitionSchedule.getTransition().getName() );
        }
    }

    /**
     * @descreption 校验全局阶段标签配置
     * @param actStageTagConfig
     *            实际配置
     * @param expStageTagConfig
     *            预期配置
     */
    public static void checkStageTagConfig(
            List< ScmLifeCycleStageTag > actStageTagConfig,
            List< ScmLifeCycleStageTag > expStageTagConfig ) {
        HashSet< BSONObject > act = new HashSet<>();
        for ( ScmLifeCycleStageTag actTag : actStageTagConfig ) {
            act.add( actTag.toBSONObject() );
        }

        HashSet< BSONObject > exp = new HashSet<>();
        for ( ScmLifeCycleStageTag expTag : expStageTagConfig ) {
            exp.add( expTag.toBSONObject() );
        }
        // 预期结果添加内置阶段标签
        exp.add( new ScmLifeCycleStageTag( "Hot", "Hot" ).toBSONObject() );
        exp.add( new ScmLifeCycleStageTag( "Warm", "Warm" ).toBSONObject() );
        exp.add( new ScmLifeCycleStageTag( "Cold", "Cold" ).toBSONObject() );
        Assert.assertEquals( act, exp );
    }

    /**
     * @descreption 校验Triggers
     * @param actTriggerList
     *            实际TriggersList
     * @param expTriggerList
     *            预期TriggersList
     */
    public static void checkTriggersConfig( List< ScmTrigger > actTriggerList,
            List< ScmTrigger > expTriggerList ) {
        Assert.assertEquals( actTriggerList.size(), expTriggerList.size() );
        for ( int j = 0; j < expTriggerList.size(); j++ ) {
            Assert.assertEquals( actTriggerList.get( j ).getID(),
                    expTriggerList.get( j ).getID() );
            Assert.assertEquals( actTriggerList.get( j ).getCreateTime(),
                    expTriggerList.get( j ).getCreateTime() );
            Assert.assertEquals( actTriggerList.get( j ).getBuildTime(),
                    expTriggerList.get( j ).getBuildTime() );

            Assert.assertEquals( actTriggerList.get( j ).getTransitionTime(),
                    expTriggerList.get( j ).getTransitionTime() );
            Assert.assertEquals( actTriggerList.get( j ).getMode(),
                    expTriggerList.get( j ).getMode() );
            Assert.assertEquals( actTriggerList.get( j ).getLastAccessTime(),
                    expTriggerList.get( j ).getLastAccessTime() );
        }
    }

    /**
     * @descreption 校验生命周期全局配置，可忽略内置标签和触发器配置无效时间字段的干扰
     * @param actScmLifeCycleConfig
     * @param expScmLifeCycleConfig
     */
    public static void checkScmLifeCycleConfigByBson(
            ScmLifeCycleConfig actScmLifeCycleConfig,
            ScmLifeCycleConfig expScmLifeCycleConfig ) {
        BSONObject actResult = actScmLifeCycleConfig.toBSONObject();
        BSONObject expResult = expScmLifeCycleConfig.toBSONObject();
        actResult.removeField( "stage_tag" );
        expResult.removeField( "stage_tag" );
        Assert.assertEquals( actResult, expResult );
        LifeCycleUtils.checkStageTagConfig(
                actScmLifeCycleConfig.getStageTagConfig(),
                expScmLifeCycleConfig.getStageTagConfig() );
    }

    /**
     * @descreption 通过转Bson方式校验数据流
     * @param actTransitionConfig
     * @param expTransitionConfig
     */
    public static void checkTransitionConfigByBson(
            List< ScmLifeCycleTransition > actTransitionConfig,
            List< ScmLifeCycleTransition > expTransitionConfig ) {
        HashSet< BSONObject > act = new HashSet<>();
        for ( ScmLifeCycleTransition actScmLifeCycleTransition : actTransitionConfig ) {
            act.add( actScmLifeCycleTransition.toBSONObject() );
        }

        HashSet< BSONObject > exp = new HashSet<>();
        for ( ScmLifeCycleTransition expScmLifeCycleTransition : expTransitionConfig ) {
            exp.add( expScmLifeCycleTransition.toBSONObject() );
        }
        Assert.assertEquals( act, exp );
    }
}
