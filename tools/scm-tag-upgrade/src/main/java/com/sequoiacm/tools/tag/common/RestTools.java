package com.sequoiacm.tools.tag.common;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdater;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class RestTools {

    private static final String ERROR_ATTRIBUTE = "X-SCM-ERROR";
    private static final ConfigEntityTranslator translator = new ConfigEntityTranslator();
    private static String encode(String url) throws ScmToolsException {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            return URLEncoder.encode(url, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new ScmToolsException("utf-8", ScmBaseExitCode.INVALID_ARG, e);
        }
    }

    private static String getErrorResponse(CloseableHttpResponse resp) throws IOException {
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

    private static void handleException(CloseableHttpResponse response) throws Exception {
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
        throw new Exception(message + ", errorcode=" + errcode);
    }

    public static void updateWorkspace(String configServerUrl, WorkspaceUpdater updater)
            throws ScmToolsException {
        HttpPut put = new HttpPut("http://" + configServerUrl + "/internal/v1/config/workspace?"
                + ScmRestArgDefine.CONFIG + "="
                + encode(translator.toConfigUpdaterBSON(updater).toString()) + "&"
                + ScmRestArgDefine.IS_ASYNC_NOTIFY + "=false");
        CloseableHttpClient client = null;
        CloseableHttpResponse resp = null;
        try {
            client = HttpClients.createDefault();
            resp = client.execute(put);
            handleException(resp);
        }
        catch (Exception e) {
            throw new ScmToolsException("failed to update workspace:" + updater.getWsName()
                    + ", cause by:" + e.getMessage(), ScmBaseExitCode.SYSTEM_ERROR);
        }
        finally {
            IOUtils.close(resp, client);
        }
    }
}
