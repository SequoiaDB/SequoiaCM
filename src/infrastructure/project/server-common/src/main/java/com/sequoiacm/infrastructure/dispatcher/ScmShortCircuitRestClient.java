package com.sequoiacm.infrastructure.dispatcher;

import com.sequoiacm.infrastructure.common.ScmLoadBalancerUtil;
import com.sequoiacm.infrastructure.dispatcher.retry.ScmRetryContext;
import com.sequoiacm.infrastructure.dispatcher.retry.ScmRetryPolicy;
import com.sequoiacm.infrastructure.feign.hystrix.ScmHystrixExecutor;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

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
                        if (!ScmLoadBalancerUtil.getRetryProperties().isEnabled()) {
                            return sendRequest(httpRequest, serviceInstance);
                        }
                        else {
                            RetryTemplate retryTemplate = new RetryTemplate();
                            retryTemplate.setRetryPolicy(
                                    new ScmRetryPolicy(httpRequest, serviceInstance));
                            return retryTemplate
                                    .execute(new RetryCallback<CloseableHttpResponse, Exception>() {
                                        @Override
                                        public CloseableHttpResponse doWithRetry(
                                                RetryContext context) throws Exception {
                                            ScmRetryContext scmRetryContext = (ScmRetryContext) context;
                                            return sendRequest(httpRequest,
                                                    scmRetryContext.getServiceInstance());
                                        }
                                    });
                        }
                    }
                });
    }

    private CloseableHttpResponse sendRequest(HttpRequest httpRequest,
            ServiceInstance serviceInstance) throws Exception {
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
