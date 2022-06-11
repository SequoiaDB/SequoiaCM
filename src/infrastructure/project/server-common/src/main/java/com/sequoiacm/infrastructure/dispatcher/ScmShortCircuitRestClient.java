package com.sequoiacm.infrastructure.dispatcher;

import com.sequoiacm.infrastructure.common.ScmLoadBalancerUtil;
import com.sequoiacm.infrastructure.feign.hystrix.ScmHystrixExecutor;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cloud.client.ServiceInstance;

import javax.annotation.PreDestroy;
import java.net.HttpURLConnection;

/**
 * 引入 Hystrix 与 Ribbon 熔断逻辑
 */
public class ScmShortCircuitRestClient extends ScmBasicRestClient {

    public ScmShortCircuitRestClient(ScmRestClientConfig config) {
        super(config);
    }

    @Override
    public CloseableHttpResponse execute(final HttpRequest httpRequest,
            final ServiceInstance serviceInstance) throws Exception {
        String service = serviceInstance.getServiceId().toLowerCase();
        return ScmHystrixExecutor.execute(service, service,
                new ScmHystrixExecutor.Runnable<CloseableHttpResponse>() {
                    @Override
                    public CloseableHttpResponse run() throws Exception {
                        try {
                            CloseableHttpResponse httpResponse = ScmShortCircuitRestClient.super.execute(
                                    httpRequest, serviceInstance);
                            ScmLoadBalancerUtil.resetInstanceError(serviceInstance);
                            return httpResponse;
                        }
                        catch (Exception e) {
                            ScmLoadBalancerUtil.recordInstanceError(serviceInstance, e);
                            throw e;
                        }
                    }
                });
    }

    @Override
    public HttpURLConnection getHttpURLConnection(final ScmURLConfig config,
            final ServiceInstance serviceInstance) throws Exception {
        String service = serviceInstance.getServiceId().toLowerCase();
        return ScmHystrixExecutor.execute(service, service,
                new ScmHystrixExecutor.Runnable<HttpURLConnection>() {
                    @Override
                    public HttpURLConnection run() throws Exception {
                        try {
                            HttpURLConnection httpURLConnection = ScmShortCircuitRestClient.super.getHttpURLConnection(
                                    config, serviceInstance);
                            ScmLoadBalancerUtil.resetInstanceError(serviceInstance);
                            return httpURLConnection;
                        }
                        catch (Exception e) {
                            ScmLoadBalancerUtil.recordInstanceError(serviceInstance, e);
                            throw e;
                        }
                    }
                });
    }

    @Override
    @PreDestroy
    public void destroy() {
        super.destroy();
    }
}
