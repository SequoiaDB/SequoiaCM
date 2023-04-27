package com.sequoiacm.cloud.gateway.forward;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.infrastructure.security.common.AuthCommonTools;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;

import com.sequoiacm.cloud.gateway.config.CustomForwardConfig;
import com.sequoiacm.cloud.gateway.statistics.commom.ScmStatisticsDefaultExtraGenerator;
import com.sequoiacm.cloud.gateway.statistics.decider.ScmStatisticsDeciderGroup;
import com.sequoiacm.cloud.gateway.statistics.decider.ScmStatisticsDecisionResult;
import com.sequoiacm.infrastructure.common.UriUtil;
import com.sequoiacm.infrastructure.dispatcher.ScmRestClient;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.security.auth.ScmUserWrapper;
import com.sequoiacm.infrastructure.statistics.client.ScmStatisticsRawDataReporter;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;

@Service
public class CustomForwarderImpl implements CustomForwarder {
    private static final Logger logger = LoggerFactory.getLogger(CustomForwarderImpl.class);

    private static final List<String> IGNORE_RESP_HEADERS = Arrays.asList("Transfer-Encoding",
            ScmStatisticsDefine.STATISTICS_EXTRA_HEADER);

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private ScmStatisticsDeciderGroup statisticsDecider;

    @Autowired
    private ScmStatisticsRawDataReporter statisticsRawDataReporter;

    @Autowired
    private ScmRestClient scmRestClient;

    private CustomForwardConfig config;

    @Autowired
    public CustomForwarderImpl(CustomForwardConfig config) {
        this.config = config;
    }

    @Override
    public void forward(String targetService, String targetApi, HttpServletRequest clientReq,
            HttpServletResponse clientResp, String defaultContentType, boolean chunked,
            boolean setForwardPrefix) throws Exception {
        long requestStartTime = System.currentTimeMillis();
        ServiceInstance instance = loadBalancerClient.choose(targetService);
        if (instance == null) {
            throw new IllegalArgumentException("unknown service:" + targetService);
        }
        final HttpHost targetInstance = new HttpHost(instance.getHost(), instance.getPort());

        ScmStatisticsDecisionResult statisticsDecideResult = statisticsDecider.decide(clientReq);

        CloseableHttpResponse forwardResp = null;
        ServletInputStream clientInputstream = null;
        boolean clientInputstreamWasConsumed = false;
        try {
            // build forward request
            clientInputstream = clientReq.getInputStream();
            final HttpRequest forwardReq = buildForwardRequest(targetApi, clientInputstream,
                    clientReq, defaultContentType, chunked);
            if (statisticsDecideResult.isNeedStatistics()) {
                forwardReq.addHeader(ScmStatisticsDefine.STATISTICS_HEADER,
                        statisticsDecideResult.getStatisticsType());
            }
            if (setForwardPrefix) {
                UriUtil.addForwardPrefix(forwardReq, "/" + targetService);
            }
            logger.debug("forward request:instance={},req={}", targetInstance, forwardReq);

            // execute with short-circuit
            forwardResp = scmRestClient.execute(forwardReq, instance);

            clientInputstreamWasConsumed = true;
            logger.debug("forward response:instance={},resp={}", targetInstance,
                    forwardResp);
            boolean isSuccessResponse = isSuccessResponse(forwardResp);
            if (!isSuccessResponse) {
                logError(targetService, targetInstance, clientReq, forwardReq, forwardResp);
            }

            // send response to client.
            sendResponseToClient(clientResp, forwardResp);

            // consume entity, release connection to pool
            EntityUtils.consume(forwardResp.getEntity());

            long duration = System.currentTimeMillis() - requestStartTime;
            ScmUserWrapper userWrapper = (ScmUserWrapper) clientReq
                    .getAttribute(RestField.USER_INFO_WRAPPER);
            String username = userWrapper == null ? null : userWrapper.getUser().getUsername();
            if (statisticsDecideResult.isNeedStatistics() && isSuccessResponse) {
                Header extraHeader = forwardResp
                        .getFirstHeader(ScmStatisticsDefine.STATISTICS_EXTRA_HEADER);
                statisticsRawDataReporter.report(true, statisticsDecideResult.getStatisticsType(),
                        username, requestStartTime, duration, extraHeader.getValue());
            }
            else if (statisticsDecideResult.isNeedStatistics()) {
                String defaultExtra = ScmStatisticsDefaultExtraGenerator
                        .generate(statisticsDecideResult.getStatisticsType(), clientReq);
                statisticsRawDataReporter.report(false, statisticsDecideResult.getStatisticsType(),
                        username, requestStartTime, duration, defaultExtra);
            }
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

    private boolean isSuccessResponse(CloseableHttpResponse resp) {
        int statusCode = resp.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return true;
        }
        return false;
    }

    private void logError(String targetService, HttpHost httpHost, HttpServletRequest clientReq,
            HttpRequest forwardReq, CloseableHttpResponse forwardResp) {
        logger.error("proxy={}", targetService);
        String method = forwardReq.getRequestLine().getMethod();
        String uri = "/" + targetService + forwardReq.getRequestLine().getUri();
        String clientAddr = clientReq.getRemoteHost() + ":" + clientReq.getRemotePort();
        String toServiceAddr = httpHost.getHostName() + ":" + httpHost.getPort();
        String session = clientReq.getHeader(RestField.SESSION_ATTRIBUTE);
        logger.error("send {} request {} from {} to {} with session {} failed(status={})", method,
                uri, clientAddr, toServiceAddr, session,
                forwardResp.getStatusLine().getStatusCode());
    }

    private HttpRequest buildForwardRequest(String targetServiceApi, InputStream clientBody,
            HttpServletRequest clientReq, String defaultContentType, boolean chunked) {
        RequestBuilder forwardReqBuilder = RequestBuilder.create(clientReq.getMethod());
        StringBuilder uri = new StringBuilder();
        uri.append(targetServiceApi);
        if (clientReq.getQueryString() != null) {
            uri.append("?").append(clientReq.getQueryString());
        }
        forwardReqBuilder.setUri(uri.toString());

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
        if (userdetails != null && !AuthCommonTools.isBigUser((String) userdetails,
                clientReq.getCharacterEncoding())) {
            forwardReqBuilder.addHeader(RestField.USER_ATTRIBUTE, (String) userdetails);
        }

        // add client request body.
        InputStreamEntity isEntity = new ScmInputStremEntity(clientBody,
                clientReq.getContentLengthLong(), config.getBufferSize());
        if (contentType == null) {
            if (defaultContentType != null) {
                isEntity.setContentType(defaultContentType);
            }
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

        // copy header from forward response.
        copyHeaderFromForwardResp(clientResp, forwardResp);

        // do not close clientOuputStream in finally block, if occur exception,
        // spring will take over it.
        ServletOutputStream clientOuputStream = clientResp.getOutputStream();

        // transfer body
        if (forwardRespBody != null) {
            transfer(forwardRespBody, clientOuputStream);
        }
        clientOuputStream.close();
    }

    private void copyHeaderFromForwardResp(HttpServletResponse clientResp,
            CloseableHttpResponse forwardResp) {
        Header[] forwardRespHeaders = forwardResp.getAllHeaders();
        for (Header forwardRespHeader : forwardRespHeaders) {
            if (!ignoreCaseContains(IGNORE_RESP_HEADERS, forwardRespHeader.getName())) {
                clientResp.addHeader(forwardRespHeader.getName(), forwardRespHeader.getValue());
            }
        }
    }

    private boolean ignoreCaseContains(List<String> list, String dest) {
        for (String e : list) {
            if (e.equalsIgnoreCase(dest)) {
                return true;
            }
        }
        return false;
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

class ScmInputStremEntity extends InputStreamEntity {
    private int bufferSize;

    public ScmInputStremEntity(InputStream instream, long len, int bufferSize) {
        super(instream, len);
        this.bufferSize = bufferSize;
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
