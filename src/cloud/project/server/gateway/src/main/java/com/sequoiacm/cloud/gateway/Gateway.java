package com.sequoiacm.cloud.gateway;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.security.auth.ScmAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

import com.sequoiacm.cloud.gateway.config.ApacheHttpClientConfiguration;
import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;
import com.sequoiacm.infrastructure.statistics.client.EnableScmStatisticsClient;

@EnableZuulProxy
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableScmMonitorServer
@EnableConfClient
@ComponentScan(excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = ApacheHttpClientConfiguration.class))
@RibbonClients(defaultConfiguration = ApacheHttpClientConfiguration.class)
@EnableScmStatisticsClient
public class Gateway implements ApplicationRunner {
    @Autowired
    private ScmConfClient confClient;

    @Autowired
    private ScmAuthenticationFilter scmAuthenticationFilter;

    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        System.setProperty("org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH", "true");
        new SpringApplicationBuilder(Gateway.class).bannerMode(Banner.Mode.OFF).web(true).run(args);
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        confClient.subscribe(ScmBusinessTypeDefine.USER, new NotifyCallback() {
            @Override
            public void processNotify(EventType type, String businessName,
                    NotifyOption notification) throws Exception {
                // 用户的创建事件可以忽略，本地无相应用户缓存时会网关会找 auth-server 拿
                if (type == EventType.CREATE) {
                    return;
                }
                scmAuthenticationFilter.removeCache(businessName);
            }
        });
    }
}
