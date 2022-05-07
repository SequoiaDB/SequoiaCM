package com.sequoiacm.infrastructure.feign;

import feign.Client;
import feign.Request;
import feign.Response;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;

@Configuration
public class ScmFeignClientConfiguration {

    @Bean
    public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
                              SpringClientFactory clientFactory) {

        return new LoadBalancerFeignClient(new ScmClient(null, null),
                cachingFactory, clientFactory);
    }

    static class ScmClient extends Client.Default {

        public ScmClient(SSLSocketFactory sslContextFactory,
                         HostnameVerifier hostnameVerifier) {
            super(sslContextFactory, hostnameVerifier);
        }

        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            try {
                return super.execute(request, options);
            }
            catch (IOException e) {
                throw new IOException("An IOException occurred while requesting:" + request.url(),
                        e);
            }

        }
    }
}
