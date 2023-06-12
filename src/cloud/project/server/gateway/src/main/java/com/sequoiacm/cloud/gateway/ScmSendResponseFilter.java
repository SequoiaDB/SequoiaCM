package com.sequoiacm.cloud.gateway;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTING_DEBUG_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.X_ZUUL_DEBUG_HEADER;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;

import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulHeaders;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import com.sequoiacm.infrastructure.common.SecurityRestField;

public class ScmSendResponseFilter extends ZuulFilter {

    private static final Log log = LogFactory.getLog(ScmSendResponseFilter.class);

    private boolean useServlet31 = true;
    private ZuulProperties zuulProperties;

    private ThreadLocal<byte[]> buffers;

    @Deprecated
    public ScmSendResponseFilter() {
        this(new ZuulProperties());
    }

    public ScmSendResponseFilter(final ZuulProperties zuulProperties) {
        this.zuulProperties = zuulProperties;
        // To support Servlet API 3.1 we need to check if setContentLengthLong
        // exists
        try {
            // TODO: remove in 2.0
            HttpServletResponse.class.getMethod("setContentLengthLong", long.class);
        }
        catch (NoSuchMethodException e) {
            useServlet31 = false;
        }
        buffers = new ThreadLocal<byte[]>() {
            @Override
            public byte[] get() {
                return new byte[zuulProperties.getInitialStreamBufferSize()];
            }
        };
    }

    /* for testing */ boolean isUseServlet31() {
        return useServlet31;
    }

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        return context.getThrowable() == null && (!context.getZuulResponseHeaders().isEmpty()
                || context.getResponseDataStream() != null || context.getResponseBody() != null);
    }

    @Override
    public Object run() {
        try {
            addResponseHeaders();
            writeResponse();
        }
        catch (Exception ex) {
            rethrowRuntimeException(ex);
        }
        return null;
    }

    private void rethrowRuntimeException(Exception ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        String serviceId;
        URI gateWayRequestUri;
        String sessionId;
        try {
            RequestContext context = RequestContext.getCurrentContext();
            sessionId = context.getRequest().getHeader(SecurityRestField.SESSION_ATTRIBUTE);
            serviceId = (String) context.get(SERVICE_ID_KEY);
            RibbonApacheHttpResponse response = (RibbonApacheHttpResponse) context
                    .get("ribbonResponse");
            gateWayRequestUri = response.getRequestedURI();
        }
        catch (Throwable e) {
            log.error("failed to rethrow exception", e);
            // throw origin error message
            throw new UndeclaredThrowableException(ex, ex.getMessage());
        }

        throw new UndeclaredThrowableException(ex,
                "gateway failed to forward response to client:sessionId=" + sessionId
                        + ",gatewayRequestService=" + serviceId + ", gatewayRequestUrl="
                        + gateWayRequestUri.toString() + ", causeBy=" + ex.getMessage());
    }

    private void writeResponse() throws Exception {
        RequestContext context = RequestContext.getCurrentContext();
        // there is no body to send
        if (context.getResponseBody() == null && context.getResponseDataStream() == null) {
            return;
        }

        HttpServletResponse servletResponse = context.getResponse();
        if (servletResponse.getCharacterEncoding() == null) { // only set if not
                                                              // set
            servletResponse.setCharacterEncoding("UTF-8");
        }
        OutputStream outStream = servletResponse.getOutputStream();
        InputStream is = null;
        try {
            if (RequestContext.getCurrentContext().getResponseBody() != null) {
                String body = RequestContext.getCurrentContext().getResponseBody();
                writeResponse(
                        new ByteArrayInputStream(
                                body.getBytes(servletResponse.getCharacterEncoding())),
                        outStream, null);
                return;
            }
            boolean isGzipRequested = false;
            final String requestEncoding = context.getRequest()
                    .getHeader(ZuulHeaders.ACCEPT_ENCODING);

            if (requestEncoding != null
                    && HTTPRequestUtils.getInstance().isGzipped(requestEncoding)) {
                isGzipRequested = true;
            }
            is = context.getResponseDataStream();
            InputStream inputStream = is;

            if (is != null) {
                if (context.sendZuulResponse()) {
                    // if origin response is gzipped, and client has not
                    // requested gzip,
                    // decompress stream
                    // before sending to client
                    // else, stream gzip directly to client
                    if (context.getResponseGZipped() && !isGzipRequested) {
                        // If origin tell it's GZipped but the content is
                        // ZERO bytes,
                        // don't try to uncompress
                        final Long len = context.getOriginContentLength();
                        if (len == null || len > 0) {
                            try {
                                inputStream = new GZIPInputStream(is);
                            }
                            catch (java.util.zip.ZipException ex) {
                                log.debug("gzip expected but not "
                                        + "received assuming unencoded response "
                                        + RequestContext.getCurrentContext().getRequest()
                                                .getRequestURL().toString());
                                inputStream = is;
                            }
                        }
                        else {
                            // Already done : inputStream = is;
                        }
                    }
                    else if (context.getResponseGZipped() && isGzipRequested) {
                        servletResponse.setHeader(ZuulHeaders.CONTENT_ENCODING, "gzip");
                    }
                    writeResponse(inputStream, outStream, context);
                }
            }
        }
        finally {
            /**
             * We must ensure that the InputStream provided by our upstream
             * pooling mechanism is ALWAYS closed even in the case of wrapped
             * streams, which are supplied by pooled sources such as Apache's
             * PoolingHttpClientConnectionManager. In that particular case, the
             * underlying HTTP connection will be returned back to the
             * connection pool iif either close() is explicitly called, a read
             * error occurs, or the end of the underlying stream is reached. If,
             * however a write error occurs, we will end up leaking a connection
             * from the pool without an explicit close()
             *
             * @author Johannes Edmeier
             */
            if (is != null) {
                try {
                    is.close();
                }
                catch (Exception ex) {
                    log.warn("Error while closing upstream input stream", ex);
                }
            }

            try {
                Object zuulResponse = RequestContext.getCurrentContext().get("zuulResponse");
                if (zuulResponse instanceof Closeable) {
                    ((Closeable) zuulResponse).close();
                }
                outStream.flush();
                // The container will close the stream for us
            }
            catch (Exception ex) {
                log.warn("Error while sending response to client: " + ex.getMessage());
            }
        }
    }

    private void writeResponse(InputStream zin, OutputStream out, RequestContext context)
            throws Exception {
        byte[] bytes = buffers.get();
        int bytesRead = -1;
        while ((bytesRead = zin.read(bytes)) != -1) {
            try {
                out.write(bytes, 0, bytesRead);
            }
            catch (Exception e) {
                if (context != null) {
                    handleExceptionWhenWriteToClient(context);
                }
                throw e;
            }
        }
    }

    private void handleExceptionWhenWriteToClient(RequestContext context) throws Exception {
        RibbonApacheHttpResponse response = (RibbonApacheHttpResponse) context
                .get("ribbonResponse");
        if (response != null) {
            log.info("close ribbonResponse when write response to client occur exception");
            try {
                HttpResponse httpResp = (HttpResponse) getValue(response, "httpResponse");
                if (httpResp instanceof CloseableHttpResponse) {
                    ((CloseableHttpResponse) httpResp).close();
                }
                else {
                    log.warn("not CloseableHttpResponse, failed to close it");
                }
            }
            catch (Exception e1) {
                log.error("failed to close ribbonResponse", e1);
            }
        }
        else {
            log.error("ribbonResponse not exist, failed to close it");
        }
    }

    private void addResponseHeaders() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletResponse servletResponse = context.getResponse();
        if (this.zuulProperties.isIncludeDebugHeader()) {
            @SuppressWarnings("unchecked")
            List<String> rd = (List<String>) context.get(ROUTING_DEBUG_KEY);
            if (rd != null) {
                StringBuilder debugHeader = new StringBuilder();
                for (String it : rd) {
                    debugHeader.append("[[[" + it + "]]]");
                }
                servletResponse.addHeader(X_ZUUL_DEBUG_HEADER, debugHeader.toString());
            }
        }
        List<Pair<String, String>> zuulResponseHeaders = context.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> it : zuulResponseHeaders) {
                servletResponse.addHeader(it.first(), it.second());
            }
        }
        // Only inserts Content-Length if origin provides it and origin response
        // is not
        // gzipped
        if (this.zuulProperties.isSetContentLength()) {
            Long contentLength = context.getOriginContentLength();
            if (contentLength != null && !context.getResponseGZipped()) {
                if (useServlet31) {
                    servletResponse.setContentLengthLong(contentLength);
                }
                else {
                    // Try and set some kind of content length if we can safely
                    // convert the Long to an int
                    if (isLongSafe(contentLength)) {
                        servletResponse.setContentLength(contentLength.intValue());
                    }
                }
            }
        }
    }

    private boolean isLongSafe(long value) {
        return value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE;
    }

    private Object getValue(Object instance, String fieldName) throws Exception, SecurityException {
        Class<? extends Object> clazz = instance.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        try {
            return field.get(instance);
        }
        finally {
            field.setAccessible(false);
        }
    }

}
