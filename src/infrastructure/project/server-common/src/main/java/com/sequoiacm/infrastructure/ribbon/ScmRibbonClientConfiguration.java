package com.sequoiacm.infrastructure.ribbon;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.loadbalancer.IRule;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration;
import org.springframework.context.annotation.*;

/**
 * 自定义负载均衡规则
 *
 * @see RibbonClientConfiguration
 */
@Configuration
public class ScmRibbonClientConfiguration {

    public static final IClientConfigKey<String> localHostName = new CommonClientConfigKey<String>("localHostName") {
    };

    /**
     * 使用自定义的本地节点优先负载均衡规则
     */
    @Bean
    public IRule rule(IClientConfig config, EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        config.set(localHostName, eurekaInstanceConfigBean.getHostname());
        ScmLocalPreferredRule scmLocalPreferredRule = new ScmLocalPreferredRule();
        scmLocalPreferredRule.initWithNiwsConfig(config);
        return scmLocalPreferredRule;
    }


}
