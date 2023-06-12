package com.sequoiacm.config.framework.operator;

import java.util.List;

import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

/**
 * 如何新增一个配置：
 * 1. 实现 ScmConfOperator ，实现类需要注解 @Component @BusinessType，
 *    在当前工程下，按配置名规划包路径，存放该配置的实现逻辑
 *
 * 2. 实现 Config、ConfigFilter、ConfigUpdater、NotifyOption 配置实体接口，实现类需要注解 @BusinessType
 *    在 config-core 工程下，按配置名规划包路径，存放该配置的实体类。（对已定义的配置实体类，其属性不能随意变更（删除、重命名），需要考虑兼容性问题）
 *
 * 3. 若新增配置存在特殊行为：
 *      a) 配置客户端使用全局版本号进行心跳，如桶配置
 *      b) 配置客户端在节点刚启动的时候，需要高频率心跳以便快速拉取配置，如工作区配置
 *    需要在 config-core 工程下，实现 ConfigCustomizer 接口，实现类需要注解 @Component @BusinessType，
 *    通过实现类调整上述行为
 *
 * 4. 若新增配置需要被高频访问（如工作区、桶等），则可以参考 config-client 下的 BucketConfCache、QuotaConfCache 实现一个配置缓存，供业务代码使用
 *
 * 业务如何访问新增配置：
 *
 * 1. 宿主服务需要引入 config-client 工程，通过注解 @EnableConfClient 开启配置客户端功能
 *
 * 2. 基本配置增删改查通过 Spring-Bean ScmConfClient 进行操作
 *
 * 3. 订阅通知通过 ScmConfClient.subscribe 进行操作
 *
 *
 */
public interface ScmConfOperator {

    List<Config> getConf(ConfigFilter filter) throws ScmConfigException;

    List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException;

    ScmConfOperateResult updateConf(ConfigUpdater config) throws ScmConfigException;

    ScmConfOperateResult deleteConf(ConfigFilter config) throws ScmConfigException;

    ScmConfOperateResult createConf(Config config) throws ScmConfigException;

    default MetaCursor listConf(ConfigFilter filter) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.SYSTEM_ERROR, "not implemented");
    }

    default long countConf(ConfigFilter filter) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.SYSTEM_ERROR, "not implemented");
    }
}