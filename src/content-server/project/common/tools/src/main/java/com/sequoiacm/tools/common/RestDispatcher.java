package com.sequoiacm.tools.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.tools.element.ScmSiteConfig;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class RestDispatcher {
    private static RestDispatcher dispatcher = new RestDispatcher();
    private static final Logger logger = LoggerFactory.getLogger(RestDispatcher.class);
    private static final String URL_PREFIX = "http://";
    private static final String CONFIG_SERVER = "/config-server";
    private static final String INTERNAL_VERSION = "/internal/v1/";
    private static final String CONFIG = "config/";
    private static final String ERROR_ATTRIBUTE = "X-SCM-ERROR";
    private static final String SITE = "site";
    private static final String NODE = "node";

    public static RestDispatcher getInstance() {
        return dispatcher;
    }

    private CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
        connMgr.setMaxTotal(2);
        connMgr.setDefaultMaxPerRoute(2);

        RequestConfig reqConf = RequestConfig.custom().setConnectionRequestTimeout(1)
                .setSocketTimeout(60 * 1000).build();

        return HttpClients.custom().setConnectionManager(connMgr).setDefaultRequestConfig(reqConf)
                .setRetryHandler(new HttpRequestRetryHandler() {
                    @Override
                    public boolean retryRequest(IOException exception, int executionCount,
                            HttpContext context) {
                        if (exception instanceof NoHttpResponseException && executionCount <= 1) {
                            return true;
                        }
                        return false;
                    }
                }).build();
    }

    public void createNode(ScmSession ss, BSONObject nodeConf) throws Exception {
        String uri = URL_PREFIX + ss.getUrl() + CONFIG_SERVER + INTERNAL_VERSION + CONFIG + NODE
                + "?" + CommonDefine.RestArg.IS_ASYNC_NOTIFY + "=" + false + "&"
                + CommonDefine.RestArg.CONFIG + "=" + encode(nodeConf.toString());
        HttpPost request = new HttpPost(uri);
        request.addHeader("x-auth-token", ss.getSessionId());
        CloseableHttpClient client = null;
        CloseableHttpResponse resp = null;
        try {
            client = createHttpClient();
            resp = client.execute(request);
            handleException(resp);
        }
        catch (Exception e) {
            throw new ScmToolsException("create content node failed:" + nodeConf,
                    ScmExitCode.SCM_REQUEST_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(resp);
            ScmCommon.closeResource(client);
        }
    }

    public void deleteNode(ScmSession ss, String hostName, int port) throws ScmToolsException {
        final String NODE_PORT = "port";
        final String NODE_HOST_NAME = "host_name";
        BSONObject filter = new BasicBSONObject(NODE_PORT, port);
        filter.put(NODE_HOST_NAME, hostName);
        String uri = URL_PREFIX + ss.getUrl() + CONFIG_SERVER + INTERNAL_VERSION + CONFIG + NODE
                + "?" + CommonDefine.RestArg.IS_ASYNC_NOTIFY + "=" + false + "&"
                + CommonDefine.RestArg.FILE_FILTER + "=" + encode(filter.toString());
        HttpDelete request = new HttpDelete(uri);
        request.addHeader("x-auth-token", ss.getSessionId());
        CloseableHttpClient client = null;
        CloseableHttpResponse resp = null;
        try {
            client = createHttpClient();
            resp = client.execute(request);
            handleException(resp);
        }
        catch (Exception e) {
            throw new ScmToolsException("delete content node failed: port=" + port,
                    ScmExitCode.SCM_REQUEST_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(resp);
            ScmCommon.closeResource(client);
        }
    }

    public void deleteSite(ScmSession ss, String siteName) throws ScmToolsException {
        final String SITE_NAME = "name";
        BSONObject filter = new BasicBSONObject(SITE_NAME, siteName);
        String uri = URL_PREFIX + ss.getUrl() + CONFIG_SERVER + INTERNAL_VERSION + CONFIG + SITE
                + "?" + CommonDefine.RestArg.IS_ASYNC_NOTIFY + "=" + false + "&"
                + CommonDefine.RestArg.FILE_FILTER + "=" + encode(filter.toString());
        HttpDelete request = new HttpDelete(uri);
        request.addHeader("x-auth-token", ss.getSessionId());
        CloseableHttpClient client = null;
        CloseableHttpResponse resp = null;
        try {
            client = createHttpClient();
            resp = client.execute(request);
            handleException(resp);
        }
        catch (Exception e) {
            throw new ScmToolsException("delete site failed: sitename=" + siteName,
                    ScmExitCode.SCM_REQUEST_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(resp);
            ScmCommon.closeResource(client);
        }
    }

    public void createSite(ScmSession ss, ScmSiteConfig siteConf)
            throws ScmInvalidArgumentException, ScmToolsException {
        String uri = URL_PREFIX + ss.getUrl() + CONFIG_SERVER + INTERNAL_VERSION + CONFIG + SITE
                + "?" + CommonDefine.RestArg.IS_ASYNC_NOTIFY + "=" + false + "&"
                + CommonDefine.RestArg.CONFIG + "=" + encode(siteConf.toBsonObject().toString());
        HttpPost request = new HttpPost(uri);
        request.addHeader("x-auth-token", ss.getSessionId());
        CloseableHttpClient client = null;
        CloseableHttpResponse resp = null;
        try {
            client = createHttpClient();
            resp = client.execute(request);
            handleException(resp);
        }
        catch (Exception e) {
            throw new ScmToolsException("create site failed:" + siteConf.toBsonObject(),
                    ScmExitCode.SCM_REQUEST_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(resp);
            ScmCommon.closeResource(client);
        }

    }

    private void handleException(CloseableHttpResponse response)
            throws IOException, ScmToolsException {
        int httpStatusCode = response.getStatusLine().getStatusCode();

        // 2xx Success
        if (httpStatusCode >= 200 && httpStatusCode < 300) {
            return;
        }

        int errcode = httpStatusCode;
        String message = null;

        String respStr = getErrorResponse(response);
        if (respStr != null && respStr.length() > 0) {
            BSONObject error = (BSONObject) JSON.parse(respStr);

            if (error.containsField("status")) {
                errcode = BsonUtils.getNumber(error, "status").intValue();
            }
            if (error.containsField("message")) {
                message = BsonUtils.getString(error, "message");
            }
        }

        throw new ScmToolsException(message, errcode);
    }

    private String getErrorResponse(CloseableHttpResponse resp) throws IOException {
        String error = null;
        HttpEntity entity = resp.getEntity();
        if (null != entity) {
            error = EntityUtils.toString(entity);
        }
        else {
            Header header = resp.getFirstHeader(ERROR_ATTRIBUTE);
            if (header != null) {
                error = header.getValue();
            }
        }
        return error;
    }

    private String encode(String url) throws ScmToolsException {
        if (Strings.isEmpty(url)) {
            return "";
        }
        try {
            return URLEncoder.encode(url, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmToolsException("utf-8", ScmExitCode.PARSE_ERROR, e);
        }
    }

}
