package com.sequoiacm.infrastructure.dispatcher;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.cloud.client.ServiceInstance;

import java.net.HttpURLConnection;

public interface ScmRestClient {

    CloseableHttpResponse execute(HttpRequest httpRequest, ServiceInstance serviceInstance) throws Exception;

    HttpURLConnection getHttpURLConnection(ScmURLConfig config, ServiceInstance serviceInstance) throws Exception;
}
