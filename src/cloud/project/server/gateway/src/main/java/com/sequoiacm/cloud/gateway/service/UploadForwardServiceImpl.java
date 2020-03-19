package com.sequoiacm.cloud.gateway.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;

import com.sequoiacm.cloud.gateway.config.UploadForwardConfig;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.security.auth.RestField;

@Service
public class UploadForwardServiceImpl implements UploadForwardService {
    private static final Logger logger = LoggerFactory.getLogger(UploadForwardServiceImpl.class);

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    private PoolingHttpClientConnectionManager connectionManager;
    private ScmTimer connectionManagerTimer;
    private CloseableHttpClient httpClient;

    @Autowired
    public UploadForwardServiceImpl(UploadForwardConfig config) {
        connectionManager = new PoolingHttpClientConnectionManager(config.getConnectionTimeToLive(),
                TimeUnit.SECONDS);
        connectionManager.setMaxTotal(config.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxPerRouteConnections());

        connectionManagerTimer = ScmTimerFactory.createScmTimer();
        connectionManagerTimer.schedule(new UploadForwardConnectionCleaner(connectionManager),
                30000, config.getConnectionCleanerRepeatInterval());

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(config.getConnectTimeout())
                .setSocketTimeout(config.getSocketTimeout())
                .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                .setRedirectsEnabled(false).build();
        httpClient = HttpClientBuilder.create().disableContentCompression()
                .disableCookieManagement().useSystemProperties()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setConnectionManager(connectionManager).build();
        InputStremEntityWithLargeBuffer.resetBufferSize(config.getBufferSize());
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
            }
            catch (Exception e) {
            }
            httpClient = null;
        }
    }

    @Override
    public void forward(String targetService, String targetApi, HttpServletRequest clientReq,
            HttpServletResponse clientResp, boolean chunked) throws IOException {
        ServiceInstance instance = loadBalancerClient.choose(targetService);
        if (instance == null) {
            throw new IllegalArgumentException("unknown service:" + targetService);
        }
        HttpHost targetInstance = new HttpHost(instance.getHost(), instance.getPort());

        CloseableHttpResponse forwardResp = null;
        ServletInputStream clientInputstream = null;
        boolean clientInputstreamWasConsumed = false;
        try {
            // build forward request
            clientInputstream = clientReq.getInputStream();
            HttpRequest forwardReq = buildForwardRequest(targetApi, clientInputstream, clientReq,
                    chunked);
            forwardReq.addHeader("x-forwarded-prefix", "/" + targetService);
            logger.debug("forward upload request:instance={},req={}", targetInstance, forwardReq);

            // forward
            forwardResp = httpClient.execute(targetInstance, forwardReq);

            clientInputstreamWasConsumed = true;
            logger.debug("forward upload response:instance={},resp={}", targetInstance,
                    forwardResp);

            logIfError(targetService, targetInstance, clientReq, forwardReq, forwardResp);

            // send response to client.
            sendResponseToClient(clientResp, forwardResp);

            // consume entity, release connection to pool
            EntityUtils.consume(forwardResp.getEntity());
        }
        catch (Exception e) {
            // if forward occur exception, just close the connection to
            // the target service.
            closeResource(forwardResp);
            throw e;
        }
        finally {
            if (!clientInputstreamWasConsumed) {
                // make sure clientInputstream was consumed, so that we can send
                // exception to client.
                consume(clientInputstream);
            }
            closeResource(clientInputstream);
        }
    }

    private void logIfError(String targetService, HttpHost httpHost, HttpServletRequest clientReq,
            HttpRequest forwardReq, CloseableHttpResponse forwardResp) {
        int statusCode = forwardResp.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        logger.error("proxy={}", targetService);
        String method = forwardReq.getRequestLine().getMethod();
        String uri = "/" + targetService + forwardReq.getRequestLine().getUri();
        String clientAddr = clientReq.getRemoteHost() + ":" + clientReq.getRemotePort();
        String toServiceAddr = httpHost.getHostName() + ":" + httpHost.getPort();
        String session = clientReq.getHeader(RestField.SESSION_ATTRIBUTE);
        logger.error("send {} request {} from {} to {} with session {} failed(status={})", method,
                uri, clientAddr, toServiceAddr, session, statusCode);
    }

    private HttpRequest buildForwardRequest(String targetServiceApi, InputStream clientBody,
            HttpServletRequest clientReq, boolean chunked) {
        RequestBuilder forwardReqBuilder = RequestBuilder.create(clientReq.getMethod());
        forwardReqBuilder.setUri(targetServiceApi);

        // copy header from client request
        String contentType = null;
        Enumeration<String> headerNames = clientReq.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.equalsIgnoreCase("transfer-encoding")
                    || headerName.equalsIgnoreCase("content-length")) {
                // ignore these headers, InputStreamEntity will set these
                // headers
                continue;
            }
            if (headerName.equalsIgnoreCase("content-type")) {
                contentType = clientReq.getHeader(headerName);
                continue;
            }

            Enumeration<String> headerValues = clientReq.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                forwardReqBuilder.addHeader(headerName, headerValue);
            }
        }

        // add user details.
        Object userdetails = clientReq.getAttribute(RestField.USER_ATTRIBUTE);
        if (userdetails != null) {
            forwardReqBuilder.addHeader(RestField.USER_ATTRIBUTE, (String) userdetails);
        }

        // copy parameter from client request.
        Map<String, String[]> paremeter = clientReq.getParameterMap();
        for (Entry<String, String[]> entry : paremeter.entrySet()) {
            String parameterName = entry.getKey();
            for (String parameterValue : entry.getValue()) {
                forwardReqBuilder.addParameter(parameterName, parameterValue);
            }
        }

        // add client request body.
        InputStreamEntity isEntity = new InputStremEntityWithLargeBuffer(clientBody,
                clientReq.getContentLengthLong());
        if (contentType == null) {
            isEntity.setContentType("binary/octet-stream");
        }
        else {
            isEntity.setContentType(contentType);
        }
        isEntity.setChunked(chunked);
        forwardReqBuilder.setEntity(isEntity);
        return forwardReqBuilder.build();
    }

    private void sendResponseToClient(HttpServletResponse clientResp,
            CloseableHttpResponse forwardResp) throws IOException {
        int forwardStatus = forwardResp.getStatusLine().getStatusCode();
        clientResp.setStatus(forwardStatus);

        // the caller of this method will release forwardResp.
        InputStream forwardRespBody = null;
        HttpEntity body = forwardResp.getEntity();
        if (body != null) {
            forwardRespBody = body.getContent();
        }

        // do not close clientOuputStream in finally block, if occur exception,
        // spring will take over it.
        ServletOutputStream clientOuputStream = clientResp.getOutputStream();

        String transferEncoding = null;
        Header transferEncodingHeader = forwardResp.getFirstHeader("Transfer-Encoding");
        if (transferEncodingHeader != null) {
            transferEncoding = transferEncodingHeader.getValue();
        }
        OutputStream encodeOutputStream = encode(transferEncoding, clientOuputStream);

        // copy header from forward response.
        copyHeaderFromForwardResp(clientResp, forwardResp);

        // transfer body
        if (forwardRespBody != null) {
            transfer(forwardRespBody, encodeOutputStream);
        }
        encodeOutputStream.close();
    }

    private void copyHeaderFromForwardResp(HttpServletResponse clientResp,
            CloseableHttpResponse forwardResp) {
        Header[] forwardRespHeaders = forwardResp.getAllHeaders();
        for (Header forwardRespHeader : forwardRespHeaders) {
            clientResp.addHeader(forwardRespHeader.getName(), forwardRespHeader.getValue());
        }
    }

    private OutputStream encode(String transferEncoding, OutputStream clientOuputStream)
            throws IOException {
        if (transferEncoding == null) {
            return clientOuputStream;
        }

        if (transferEncoding.equalsIgnoreCase("chunked")) {
            return new ChunkedOutputStream(clientOuputStream, 4096);
        }

        throw new UnsupportedEncodingException("Unsupported transfer-encoding:" + transferEncoding);
    }

    private void transfer(InputStream is, OutputStream os) throws IOException {
        byte[] forwardBuffer = new byte[1024];
        int len = 0;
        while (true) {
            len = is.read(forwardBuffer);
            if (len <= -1) {
                break;
            }
            os.write(forwardBuffer, 0, len);
        }
    }

    private void closeResource(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            }
            catch (Exception e) {
                logger.warn("failed to close resource", e);
            }
        }
    }

    private void consume(InputStream is) {
        if (is == null) {
            return;
        }
        byte[] garbage = new byte[1024];
        try {
            while (is.read(garbage) != -1) {

            }
        }
        catch (Exception e) {
            logger.warn("failed to consume resource", e);
        }
    }

}

class UploadForwardConnectionCleaner extends ScmTimerTask {
    private static final Logger logger = LoggerFactory
            .getLogger(UploadForwardConnectionCleaner.class);
    private PoolingHttpClientConnectionManager connectionManager;

    public UploadForwardConnectionCleaner(PoolingHttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        if (logger.isDebugEnabled()) {
            Set<HttpRoute> routes = connectionManager.getRoutes();
            for (HttpRoute route : routes) {
                logger.debug("connnection manager info:route={},routeState={}", route,
                        connectionManager.getStats(route));
            }
            logger.debug("connection manager total state:{}", connectionManager.getTotalStats());
        }
        connectionManager.closeExpiredConnections();
    }

}

class InputStremEntityWithLargeBuffer extends InputStreamEntity {
    private static int bufferSize = 1024 * 1024;

    public InputStremEntityWithLargeBuffer(InputStream instream, long len) {
        super(instream, len);
    }

    public static void resetBufferSize(int bufferSize) {
        InputStremEntityWithLargeBuffer.bufferSize = bufferSize;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        Args.notNull(outstream, "Output stream");
        final InputStream instream = getContent();
        try {
            final byte[] buffer = new byte[bufferSize];
            int l;
            if (getContentLength() < 0) {
                // consume until EOF
                while ((l = readAsMuchAsPossible(instream, buffer, 0, buffer.length)) != -1) {
                    outstream.write(buffer, 0, l);
                }
            }
            else {
                // consume no more than length
                long remaining = this.getContentLength();
                while (remaining > 0) {
                    l = readAsMuchAsPossible(instream, buffer, 0,
                            (int) Math.min(bufferSize, remaining));
                    if (l == -1) {
                        break;
                    }
                    outstream.write(buffer, 0, l);
                    remaining -= l;
                }
            }
        }
        finally {
            instream.close();
        }
    }

    private int readAsMuchAsPossible(InputStream is, byte[] buf, int offset, int length)
            throws IOException {
        if (offset < 0 || offset + length > buf.length) {
            throw new RuntimeException(
                    "bufferLength=" + buf.length + ",offset=" + offset + ",length=" + length);
        }

        if (length <= 0) {
            return 0;
        }

        int maxLength = offset + length;
        int realOffset = offset;
        int tempLength = 0;
        while (realOffset < maxLength && tempLength > -1) {
            tempLength = is.read(buf, realOffset, maxLength - realOffset);
            if (tempLength > 0) {
                realOffset += tempLength;
            }
        }

        if (realOffset > offset) {
            return realOffset - offset;
        }

        return -1;
    }

}
