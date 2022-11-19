package com.sequoiacm.infrastructure.dispatcher;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanTextMap;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.util.SpanNameUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ScmTraceRestClient extends ScmBasicRestClient {

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private HttpSpanInjector httpSpanInjector;

    @Autowired(required = false)
    private HttpTraceKeysInjector keysInjector;

    @Autowired(required = false)
    private ErrorParser errorParser;

    public ScmTraceRestClient(ScmRestClientConfig config) {
        super(config);
    }

    @Override
    public CloseableHttpResponse execute(HttpRequest httpRequest, ServiceInstance serviceInstance)
            throws Exception {
        Span span = null;
        if (tracer != null) {
            String url = "http://" + serviceInstance.getHost() + ":" + serviceInstance.getPort()
                    + httpRequest.getRequestLine().getUri();
            span = createSpan(url, httpRequest.getRequestLine().getMethod(),
                    new ScmHttpClientSpanTextMap(httpRequest));
        }
        try {
            return super.execute(httpRequest, serviceInstance);
        }
        catch (RuntimeException | IOException e) {
            // 只需捕获这两种异常，其它异常下游服务会填充异常信息
            recordError(e, span);
            throw e;
        }
        finally {
            clientReceived(span);
            closeSpan(span);
        }

    }

    @Override
    public HttpURLConnection getHttpURLConnection(ScmURLConfig config,
            ServiceInstance serviceInstance) throws Exception {
        Span span = null;
        if (tracer != null) {
            span = createSpan(config.getUrl(), config.getRequestMethod(),
                    new ScmUrlConfigSpanTextMap(config));
        }
        try {
            return super.getHttpURLConnection(config, serviceInstance);
        }
        catch (RuntimeException | IOException e) {
            recordError(e, span);
            throw e;
        }
        finally {
            clientReceived(span);
            closeSpan(span);
        }

    }

    private void closeSpan(Span span) {
        if (span != null && tracer != null) {
            tracer.close(span);
        }
    }

    private void recordError(Exception e, Span span) {
        if (span != null && span.isExportable()) {
            errorParser.parseErrorTags(span, e);
        }
    }

    private void clientReceived(Span span) {
        if (span != null && span.isExportable()) {
            span.logEvent(Span.CLIENT_RECV);
        }
    }

    private Span createSpan(String url, String method, SpanTextMap spanTextMap) {
        URI uri = URI.create(url);
        String spanName = SpanNameUtil.shorten(uriScheme(uri) + ":" + uri.getPath());
        Span span = tracer.createSpan(spanName);
        if (span.isExportable()) {
            httpSpanInjector.inject(span, spanTextMap);
            span.logEvent(Span.CLIENT_SEND);
            keysInjector.addRequestTags(uri.toString(), uri.getHost(), uri.getPath(), method);
        }
        return span;
    }

    private String uriScheme(URI uri) {
        return uri.getScheme() == null ? "http" : uri.getScheme();
    }

    static class ScmHttpClientSpanTextMap implements SpanTextMap {

        private final HttpRequest httpRequest;

        public ScmHttpClientSpanTextMap(HttpRequest httpRequest) {
            this.httpRequest = httpRequest;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            Map<String, String> map = new HashMap<>();
            for (Header header : httpRequest.getAllHeaders()) {
                map.put(header.getName(), header.getValue());
            }
            return new UnmodifiableIterator(map.entrySet().iterator());
        }

        @Override
        public void put(String key, String value) {
            httpRequest.addHeader(key, value);
        }
    }

    static class ScmUrlConfigSpanTextMap implements SpanTextMap {
        private static final Iterator<Map.Entry<String, String>> EMPTY_ITERATOR = new Iterator<Map.Entry<String, String>>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Map.Entry<String, String> next() {
                return null;
            }

            @Override
            public void remove() {
            }
        };

        private final ScmURLConfig urlConfig;

        public ScmUrlConfigSpanTextMap(ScmURLConfig urlConfig) {
            this.urlConfig = urlConfig;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            if (urlConfig.getRequestProperties() != null) {
                return new UnmodifiableIterator(
                        urlConfig.getRequestProperties().entrySet().iterator());
            }
            return EMPTY_ITERATOR;
        }

        @Override
        public void put(String key, String value) {
            if (urlConfig.getRequestProperties() == null) {
                urlConfig.setRequestProperties(new HashMap<String, String>());
            }
            urlConfig.getRequestProperties().put(key, value);
        }
    }

    static class UnmodifiableIterator implements Iterator<Map.Entry<String, String>> {
        private final Iterator<Map.Entry<String, String>> iterator;

        public UnmodifiableIterator(Iterator<Map.Entry<String, String>> iterator) {
            super();
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Map.Entry<String, String> next() {
            return iterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("remove() is not supported");
        }
    }

}
