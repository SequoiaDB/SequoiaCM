package com.sequoiacm.client.dispatcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.exception.ScmError;

final class RestClient {
    private RestClient() {
    }

    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
    private static final String AUTHORIZATION = "x-auth-token";
    private static final String ERROR_ATTRIBUTE = "X-SCM-ERROR";
    private static final String NO_AUTH_SESSION_ID = "-1";
    private static final String CHARSET_UTF8 = "utf-8";

    public static void sendRequest(CloseableHttpClient client, String sessionId,
            HttpRequestBase request) throws ScmException {
        CloseableHttpResponseWrapper response = sendRequestWithHttpResponse(client, sessionId,
                request);
        consumeResp(response);
    }

    public static void sendRequest(CloseableHttpClient client, String sessionId,
            HttpEntityEnclosingRequestBase request, List<NameValuePair> params)
            throws ScmException {
        CloseableHttpResponseWrapper response = sendRequestWithHttpResponse(client, sessionId,
                request, params);
        consumeResp(response);
    }

    public static String sendRequestWithHeaderResponse(CloseableHttpClient client, String sessionId,
            HttpRequestBase request, String keyName) throws ScmException {
        CloseableHttpResponseWrapper response = sendRequestWithHttpResponse(client, sessionId,
                request);
        try {
            String value = response.getFirstHeader(keyName).getValue();
            return URLDecoder.decode(value, CHARSET_UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmSystemException(CHARSET_UTF8, e);
        }
        finally {
            consumeResp(response);
        }
    }

    public static String sendRequestWithHeaderResponse(CloseableHttpClient client, String sessionId,
            HttpEntityEnclosingRequestBase request, List<NameValuePair> params, String keyName)
            throws ScmException {
        CloseableHttpResponseWrapper response = sendRequestWithHttpResponse(client, sessionId,
                request, params);
        try {
            String value = response.getFirstHeader(keyName).getValue();
            return URLDecoder.decode(value, CHARSET_UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmSystemException(CHARSET_UTF8, e);
        }
        finally {
            consumeResp(response);
        }
    }

    public static BSONObject sendRequestWithJsonResponse(CloseableHttpClient client,
            String sessionId, HttpRequestBase request) throws ScmException {
        CloseableHttpResponseWrapper response = sendRequestWithHttpResponse(client, sessionId,
                request);
        try {
            String resp = EntityUtils.toString(response.getEntity(), CHARSET_UTF8);
            return (BSONObject) JSON.parse(resp);
        }
        catch (IOException e) {
            throw new ScmException(ScmError.NETWORK_IO,
                    "an error occurs during get the http entity", e);
        }
        finally {
            consumeResp(response);
        }
    }

    public static BSONObject sendRequestWithJsonResponse(CloseableHttpClient client,
            String sessionId, HttpEntityEnclosingRequestBase request, List<NameValuePair> params)
            throws ScmException {
        CloseableHttpResponseWrapper response = sendRequestWithHttpResponse(client, sessionId,
                request, params);
        try {
            String resp = EntityUtils.toString(response.getEntity(), CHARSET_UTF8);
            return (BSONObject) JSON.parse(resp);
        }
        catch (IOException e) {
            throw new ScmException(ScmError.NETWORK_IO,
                    "an error occurs during get the http entity", e);
        }
        finally {
            consumeResp(response);
        }
    }

    public static BsonReader sendRequestWithBsonReaderResponse(CloseableHttpClient client,
            String sessionId, HttpRequestBase request) throws ScmException {
        CloseableHttpResponseWrapper response = sendRequestWithHttpResponse(client, sessionId,
                request);
        try {
            CloseableHttpResponseInputStream is = new CloseableHttpResponseInputStream(response);
            return new RestBsonReader(is);
        }
        catch (ScmException e) {
            response.closeResponse();
            throw e;
        }
        catch (Exception e) {
            response.closeResponse();
            throw new ScmSystemException("sendRequestWithBsonReaderResponse failed", e);
        }

    }

    public static CloseableHttpResponseWrapper sendRequestWithHttpResponse(
            CloseableHttpClient client, String sessionId, HttpRequestBase request)
            throws ScmException {
        if (!NO_AUTH_SESSION_ID.equals(sessionId)) {
            request.setHeader(AUTHORIZATION, sessionId);
        }

        return sendRequest(client, request);
    }

    public static CloseableHttpResponseWrapper sendRequestWithHttpResponse(
            CloseableHttpClient client, String sessionId, HttpEntityEnclosingRequestBase request,
            List<NameValuePair> params) throws ScmException {
        if (!NO_AUTH_SESSION_ID.equals(sessionId)) {
            request.setHeader(AUTHORIZATION, sessionId);
        }

        if (params != null) {
            HttpEntity entity;
            try {
                entity = new UrlEncodedFormEntity(params, CHARSET_UTF8);
            }
            catch (UnsupportedEncodingException e) {
                throw new ScmSystemException(CHARSET_UTF8, e);
            }
            request.setEntity(entity);
        }

        return sendRequest(client, request);
    }

    private static CloseableHttpResponseWrapper sendRequest(CloseableHttpClient client,
            HttpRequestBase request) throws ScmException {
        try {
            CloseableHttpResponse response = client.execute(request);
            CloseableHttpResponseWrapper respWrapper = new CloseableHttpResponseWrapper(response,
                    null);
            handleException(respWrapper);
            return respWrapper;
        }
        catch (ConnectionPoolTimeoutException e) {
            logger.warn("lease connection timeout, create a temp http client to send this request",
                    e);
            return resendRequest(request);
        }
        catch (ScmException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmException(ScmError.NETWORK_IO,
                    "an error occurs during the http connection", e);
        }
    }

    private static CloseableHttpResponseWrapper resendRequest(HttpRequestBase request)
            throws ScmException {
        CloseableHttpClient c = HttpClients.createDefault();
        logger.debug("create tempHttpClient:" + c);
        try {
            CloseableHttpResponse resp = c.execute(request);
            CloseableHttpResponseWrapper respWrapper = new CloseableHttpResponseWrapper(resp, c);
            handleException(respWrapper);
            return respWrapper;
        }
        catch (ScmException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmException(ScmError.NETWORK_IO,
                    "an error occurs during the http connection", e);
        }
    }

    private static void handleException(CloseableHttpResponseWrapper respWrapper)
            throws ScmException {
        int httpStatusCode = respWrapper.getStatusLine().getStatusCode();

        // 2xx Success
        if (httpStatusCode >= 200 && httpStatusCode < 300) {
            return;
        }

        try {
            int errcode = httpStatusCode;
            String message = null;

            String resp = getErrorResponse(respWrapper);
            if (Strings.hasText(resp)) {
                BSONObject error = (BSONObject) JSON.parse(resp);
                if (error.containsField("status")) {
                    errcode = BsonUtils.getNumber(error, "status").intValue();
                }
                if (error.containsField("message")) {
                    message = BsonUtils.getString(error, "message");
                }
            }

            throw new ScmException(errcode, message);
        }
        finally {
            consumeResp(respWrapper);
        }
    }

    private static String getErrorResponse(CloseableHttpResponseWrapper respWrapper)
            throws ScmException {
        String error = null;

        HttpEntity entity = respWrapper.getEntity();
        if (null != entity) {
            try {
                error = EntityUtils.toString(entity);
            }
            catch (IOException e) {
                throw new ScmException(ScmError.NETWORK_IO, "an error occurs when read http entity",
                        e);
            }
        }
        else {
            Header header = respWrapper.getFirstHeader(ERROR_ATTRIBUTE);
            if (header != null) {
                error = header.getValue();
            }
        }

        return error;
    }

    public static void consumeResp(CloseableHttpResponseWrapper response) {
        if (response != null) {
            try {
                response.consumeEntity();
            }
            catch (Exception e) {
                logger.warn("close HttpResponse failed:resp={}", response, e);
            }
        }
    }
}
