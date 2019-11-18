package com.sequoiacm.deploy.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestTools {
    private static final Logger logger = LoggerFactory.getLogger(RestTools.class);

    private static void checkEurekaCache(String url, List<String> expetedServices)
            throws Exception {
        HttpGet get = new HttpGet(url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(get);
            int httpStatusCode = response.getStatusLine().getStatusCode();

            if (httpStatusCode >= 200 && httpStatusCode < 300) {
                if (expetedServices == null) {
                    return;
                }
                BSONObject bson = (BSONObject) JSON
                        .parse(EntityUtils.toString(response.getEntity()));
                bson = BsonUtils.getBSONObjectChecked(bson, "discoveryComposite");
                bson = BsonUtils.getBSONObjectChecked(bson, "discoveryClient");
                BasicBSONList services = BsonUtils.getArrayChecked(bson, "services");
                if (services.containsAll(expetedServices)) {
                    return;
                }
                throw new Exception(
                        "excepted services not in eureka client cache:url=" + url + ", service="
                                + services.toString() + ", expectedService=" + expetedServices);
            }
            throw new IOException("requeset get error response:req=" + url + ", resp=" + response
                    + ", respBody=" + EntityUtils.toString(response.getEntity()));
        }
        finally {
            consume(response);
            CommonUtils.closeResource(httpClient);
        }
    }

    private static void consume(CloseableHttpResponse response) {
        if (response == null) {
            return;
        }
        try {
            EntityUtils.consume(response.getEntity());
        }
        catch (Exception e) {
            logger.warn("failed to release hhtp response:{}", response);
        }
    }

    public static void waitDependentServiceReady(String url, int waitTimeout,
            String... eurekaClientCacheServices) throws Exception {
        logger.info("Waiting for dependent services({}) be ready, timeout={}ms",
                Arrays.toString(eurekaClientCacheServices), waitTimeout);
        long startTime = System.currentTimeMillis();
        Exception lastException = null;
        while (true) {
            try {
                checkEurekaCache("http://" + url + "/health",
                        Arrays.asList(eurekaClientCacheServices));
                break;
            }
            catch (Exception e) {
                lastException = e;
            }
            if (System.currentTimeMillis() - startTime > waitTimeout) {
                throw new Exception("failed to wait service be ready:serviceUrl=" + url,
                        lastException);
            }
            Thread.sleep(1000);
        }

    }
}
