package com.sequoiacm.infrastructure.dispatcher;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ScmBasicRestClient implements ScmRestClient {

    private static final Logger logger = LoggerFactory.getLogger(ScmBasicRestClient.class);

    private CloseableHttpClient httpClient;

    private PoolingHttpClientConnectionManager connectionManager;
    private ScmTimer connectionManagerTimer;

    private ScmRestClientConfig config;

    public ScmBasicRestClient(ScmRestClientConfig config) {
        try {
            connectionManager = new PoolingHttpClientConnectionManager(config.getConnectionTimeToLive(),
                    TimeUnit.SECONDS);
            connectionManager.setMaxTotal(config.getMaxTotalConnections());
            connectionManager.setDefaultMaxPerRoute(config.getMaxPerRouteConnections());

            connectionManagerTimer = ScmTimerFactory.createScmTimer();
            connectionManagerTimer.schedule(new HttpClientConnectionCleaner(connectionManager), 30000,
                    config.getConnectionCleanerRepeatInterval());

            RequestConfig defaultRequestConfig = RequestConfig.custom()
                    .setConnectTimeout(config.getConnectTimeout())
                    .setSocketTimeout(config.getSocketTimeout())
                    .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                    .setRedirectsEnabled(false).build();
            httpClient = HttpClientBuilder.create().disableContentCompression()
                    .disableCookieManagement().useSystemProperties()
                    .setDefaultRequestConfig(defaultRequestConfig)
                    .setRetryHandler(new HttpRequestRetryHandler() {
                        @Override
                        public boolean retryRequest(IOException exception, int executionCount,
                                                    HttpContext context) {
                            return exception instanceof NoHttpResponseException && executionCount <= 1;
                        }
                    }).setConnectionManager(connectionManager).build();
            this.config = config;
        } catch (Exception e) {
            destroy();
            throw e;
        }
    }


    @PreDestroy
    public void destroy() {
        if (connectionManagerTimer != null) {
            connectionManagerTimer.cancel();
            connectionManagerTimer = null;
        }

        if (connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception ignored) {
            }
            httpClient = null;
        }
    }

    @Override
    public CloseableHttpResponse execute(HttpRequest httpRequest, ServiceInstance serviceInstance)
            throws Exception {
        HttpHost httpHost = new HttpHost(serviceInstance.getHost(), serviceInstance.getPort());
        return httpClient.execute(httpHost, httpRequest);
    }

    @Override
    public HttpURLConnection getHttpURLConnection(ScmURLConfig config,
            ServiceInstance serviceInstance) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(config.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(config.isDoOutput());
            connection.setDoInput(config.isDoInput());
            connection.setChunkedStreamingMode(config.getChunkedStreamingMode());
            connection.setUseCaches(config.isUseCaches());
            connection.setRequestMethod(config.getRequestMethod());
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            Map<String, String> requestProperties = config.getRequestProperties();
            if (requestProperties != null) {
                for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            connection.connect();
            return connection;
        }
        catch (Exception e) {
            if (connection != null) {
                connection.disconnect();
            }
            logger.warn("failed to connect url:{}", config.getUrl());
            throw e;
        }
    }
}

class HttpClientConnectionCleaner extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientConnectionCleaner.class);
    private PoolingHttpClientConnectionManager connectionManager;

    public HttpClientConnectionCleaner(PoolingHttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        if (logger.isDebugEnabled()) {
            Set<HttpRoute> routes = connectionManager.getRoutes();
            for (HttpRoute route : routes) {
                logger.debug("connection manager info:route={},routeState={}", route,
                        connectionManager.getStats(route));
            }
            logger.debug("connection manager total state:{}", connectionManager.getTotalStats());
        }
        connectionManager.closeExpiredConnections();
    }

}
