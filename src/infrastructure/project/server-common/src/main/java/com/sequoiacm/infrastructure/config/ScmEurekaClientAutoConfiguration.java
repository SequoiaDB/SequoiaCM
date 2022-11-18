package com.sequoiacm.infrastructure.config;

import com.google.common.base.Joiner;
import com.sequoiacm.infrastructure.common.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * 对服务注册时提供的节点信息进行自定义：
 * 
 * @see ScmEurekaClientAutoConfiguration#customize(EurekaInstanceConfigBean,
 *      String, String)
 */
@Configuration
@ConditionalOnClass({ Endpoint.class, EurekaInstanceConfigBean.class })
@ConditionalOnProperty(prefix = "eureka.client", name = "register-with-eureka", havingValue = "true")
public class ScmEurekaClientAutoConfiguration {

    private static final Logger logger = LoggerFactory
            .getLogger(ScmEurekaClientAutoConfiguration.class);

    private static final String METADATA_IP_LIST = "ipList";

    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils,
            ManagementMetadataProvider managementMetadataProvider, ConfigurableEnvironment env)
            throws MalformedURLException {
        PropertyResolver environmentPropertyResolver = new RelaxedPropertyResolver(env);
        PropertyResolver eurekaPropertyResolver = new RelaxedPropertyResolver(env,
                "eureka.instance.");
        String hostname = eurekaPropertyResolver.getProperty("hostname");

        boolean preferIpAddress = Boolean
                .parseBoolean(eurekaPropertyResolver.getProperty("preferIpAddress"));
        String ipAddress = eurekaPropertyResolver.getProperty("ipAddress");
        boolean isSecurePortEnabled = Boolean
                .parseBoolean(eurekaPropertyResolver.getProperty("securePortEnabled"));
        String serverContextPath = environmentPropertyResolver.getProperty("server.contextPath",
                "/");
        int serverPort = Integer.parseInt(environmentPropertyResolver.getProperty("server.port",
                environmentPropertyResolver.getProperty("port", "8080")));

        Integer managementPort = environmentPropertyResolver.getProperty("management.port",
                Integer.class);// nullable. should be wrapped into optional
        String managementContextPath = environmentPropertyResolver
                .getProperty("management.contextPath");// nullable. should be wrapped into optional
        Integer jmxPort = environmentPropertyResolver
                .getProperty("com.sun.management.jmxremote.port", Integer.class);// nullable
        EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);

        instance.setNonSecurePort(serverPort);

        instance.setPreferIpAddress(preferIpAddress);
        instance.setSecurePortEnabled(isSecurePortEnabled);
        if (isSecurePortEnabled) {
            instance.setSecurePort(serverPort);
        }

        // custom begin
        customize(instance, hostname, ipAddress);
        // custom end

        String statusPageUrlPath = eurekaPropertyResolver.getProperty("statusPageUrlPath");
        String healthCheckUrlPath = eurekaPropertyResolver.getProperty("healthCheckUrlPath");

        if (StringUtils.hasText(statusPageUrlPath)) {
            instance.setStatusPageUrlPath(statusPageUrlPath);
        }
        if (StringUtils.hasText(healthCheckUrlPath)) {
            instance.setHealthCheckUrlPath(healthCheckUrlPath);
        }

        ManagementMetadata metadata = managementMetadataProvider.get(instance, serverPort,
                serverContextPath, managementContextPath, managementPort);

        if (metadata != null) {
            instance.setStatusPageUrl(metadata.getStatusPageUrl());
            instance.setHealthCheckUrl(metadata.getHealthCheckUrl());
            if (instance.isSecurePortEnabled()) {
                instance.setSecureHealthCheckUrl(metadata.getSecureHealthCheckUrl());
            }
            Map<String, String> metadataMap = instance.getMetadataMap();
            if (metadataMap.get("management.port") == null) {
                metadataMap.put("management.port", String.valueOf(metadata.getManagementPort()));
            }
        }

        instance.setInstanceId(getDefaultInstanceId(instance, env));

        setupJmxPort(instance, jmxPort);

        return instance;
    }

    private void customize(EurekaInstanceConfigBean instance, String hostname, String ipAddress) {
        // 自定义1：添加所有网卡IP到 metadata
        List<String> ipList = NetUtil.getAllNetworkInterfaceIp();
        if (ipList.size() > 0) {
            instance.getMetadataMap().put(METADATA_IP_LIST, Joiner.on(",").join(ipList));
        }

        // 自定义2：优先使用本机的主机名、IP进行注册
        // 2.1 先检查用户在配置文件中是否指定了主机信息(eureka.instance.hostname)，如果指定了，优先使用配置文件里的
        checkHostnameAndIp(hostname, ipAddress);
        if (StringUtils.hasText(hostname) && StringUtils.hasText(ipAddress)) {
            instance.setHostname(hostname);
            instance.setIpAddress(ipAddress);
            return;
        }

        // 2.1 如果没有指定，并且用户正确配置了 /etc/hosts，则使用 /etc/hosts 中的主机信息
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            instance.setHostname(localHost.getHostName());
            instance.setIpAddress(localHost.getHostAddress());
            return;
        }
        catch (UnknownHostException e) {
            logger.warn("failed to get localhost", e);
        }

        // 2.3 否则不进行设置，即使用 eureka 从网卡中提取出的主机信息

    }

    private void checkHostnameAndIp(String hostname, String ipAddress) {
        // 主机名和ip要么都配，要么都不配
        if ((hostname == null && ipAddress != null) || (hostname != null && ipAddress == null)) {
            throw new IllegalArgumentException("eureka.instance.hostname and eureka.instance.ipAddress must exist at the same time");
        }
    }

    private String getDefaultInstanceId(EurekaInstanceConfigBean instance,
            ConfigurableEnvironment env) {
        return instance.getHostname() + ":" + env.getProperty("spring.application.name") + ":"
                + env.getProperty("server.port");
    }

    private void setupJmxPort(EurekaInstanceConfigBean instance, Integer jmxPort) {
        Map<String, String> metadataMap = instance.getMetadataMap();
        if (metadataMap.get("jmx.port") == null && jmxPort != null) {
            metadataMap.put("jmx.port", String.valueOf(jmxPort));
        }
    }

}
