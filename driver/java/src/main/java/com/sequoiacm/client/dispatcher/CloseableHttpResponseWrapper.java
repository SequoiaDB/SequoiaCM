package com.sequoiacm.client.dispatcher;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseableHttpResponseWrapper {
    private static final Logger logger = LoggerFactory
            .getLogger(CloseableHttpResponseWrapper.class);
    private CloseableHttpResponse resp;
    private CloseableHttpClient tmpHttpClient;

    public CloseableHttpResponseWrapper(CloseableHttpResponse resp, CloseableHttpClient tmpClient) {
        this.resp = resp;
        this.tmpHttpClient = tmpClient;
    }

    public void consumeEntity() {
        try {
            EntityUtils.consume(resp.getEntity());
        }
        catch (Exception e) {
            logger.warn("consume entity failed:resp={}", resp, e);
        }

        if (tmpHttpClient != null) {
            try {
                tmpHttpClient.close();
                logger.debug("close tempHttpClient:" + tmpHttpClient);
            }
            catch (Exception e) {
                logger.warn("tmp httpclient close failed:httpClient={}", tmpHttpClient, e);
            }
        }
    }

    public void closeResponse() {
        try {
            resp.close();
        }
        catch (Exception e) {
            logger.warn("response close failed:resp={}", resp, e);
        }

        if (tmpHttpClient != null) {
            try {
                tmpHttpClient.close();
                logger.debug("close tempHttpClient:" + tmpHttpClient);
            }
            catch (Exception e) {
                logger.warn("tmp httpclient close failed:httpClient={}", tmpHttpClient, e);
            }
        }
    }

    public StatusLine getStatusLine() {
        return resp.getStatusLine();
    }

    public Header getFirstHeader(String keyName) {
        return resp.getFirstHeader(keyName);
    }

    public HttpEntity getEntity() {
        return resp.getEntity();
    }
}