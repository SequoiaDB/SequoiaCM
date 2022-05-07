package com.sequoiacm.cloud.gateway.config;

import java.io.IOException;

import javax.annotation.PreDestroy;

import com.netflix.zuul.context.RequestContext;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

/**
 * Copy from
 * org.springframework.cloud.netflix.ribbon.apache.HttpClientRibbonConfiguration
 */
@Configuration
public class ApacheHttpClientConfiguration {

    public static final String HTTP_CLIENT_TARGET_HOST = "http_client_target_host";
    private CloseableHttpClient httpClient;

    @Bean
    public CloseableHttpClient httpClient(ApacheHttpClientFactory httpClientFactory,
            HttpClientConnectionManager connectionManager, IClientConfig config) {
        Boolean followRedirects = config.getPropertyAsBoolean(CommonClientConfigKey.FollowRedirects,
                DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS);
        Integer connectTimeout = config.getPropertyAsInteger(CommonClientConfigKey.ConnectTimeout,
                DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout).setRedirectsEnabled(followRedirects).build();
        this.httpClient = httpClientFactory.createBuilder()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setRetryHandler(new ScmHttpClientRetryHandler())
                .setRequestExecutor(new ScmHttpRequestExecutor())
                .setConnectionManager(connectionManager).build();
        return httpClient;
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}

class ScmHttpClientRetryHandler extends DefaultHttpRequestRetryHandler {

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        boolean canRetry = super.retryRequest(exception, executionCount, context);
        if (!canRetry) {
            if (exception instanceof NoHttpResponseException
                    && executionCount <= super.getRetryCount()) {
                canRetry = true;
            }
        }
        return canRetry;
    }

}

class ScmHttpRequestExecutor extends HttpRequestExecutor {

    @Override
    public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context)
            throws IOException, HttpException {
        String target = String.valueOf(context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST));
        RequestContext currentContext = RequestContext.getCurrentContext();
        if (currentContext != null) {
            currentContext.set(ApacheHttpClientConfiguration.HTTP_CLIENT_TARGET_HOST, target);
        }
        return super.execute(request, conn, context);
    }
}
