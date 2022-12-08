package com.sequoiacm.infrastructure.ribbon;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.loadbalancer.IRule;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 自定义负载均衡规则
 *
 * @see RibbonClientConfiguration
 */
@Configuration
public class ScmRibbonClientConfiguration {

    public static final IClientConfigKey<List<ScmServersFilter>> scmServersFilterListKey = new CommonClientConfigKey<List<ScmServersFilter>>(
            "scmServersFilterList") {
    };

    /**
     * 使用自定义的负载均衡规则代替原来的 ZoneAvoidanceRule
     */
    @Bean
    public IRule rule(IClientConfig config, List<ScmServersFilter> scmServersFilterList) {
        config.set(scmServersFilterListKey, scmServersFilterList);
        ScmLoadBalancerRule scmLoadBalancerRule = new ScmLoadBalancerRule();
        scmLoadBalancerRule.initWithNiwsConfig(config);
        return scmLoadBalancerRule;
    }

    @Bean
    public ScmLoadBalancerRuleConfig scmLoadBalanceRuleConfig(
            EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        return new ScmLoadBalancerRuleConfig(eurekaInstanceConfigBean);
    }

    @Bean
    @Order(1)
    public ScmServersFilter scmAcrossGroupAccessServersFilter(
            ScmLoadBalancerRuleConfig scmLoadBalancerRuleConfig) {
        return new ScmAcrossGroupAccessServersFilter(scmLoadBalancerRuleConfig);
    }

    @Bean
    @Order(1)
    public ScmServersFilter scmAlongGroupAccessServersFilter(
            ScmLoadBalancerRuleConfig scmLoadBalancerRuleConfig) {
        return new ScmAlongGroupAccessServersFilter(scmLoadBalancerRuleConfig);
    }

    @Bean
    @Order(2)
    public ScmServersFilter scmHostNameMatchServersFilter(
            ScmLoadBalancerRuleConfig scmLoadBalancerRuleConfig) {
        return new ScmHostNameMatchServersFilter(scmLoadBalancerRuleConfig);
    }

}
