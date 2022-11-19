package com.sequoiacm.infrastructure.slowlog.filter;

import com.sequoiacm.infrastructure.slowlog.SlowLogContext;
import com.sequoiacm.infrastructure.slowlog.SlowLogContextImpl;
import com.sequoiacm.infrastructure.slowlog.SlowLogManager;
import com.sequoiacm.infrastructure.slowlog.appender.SlowLogAppender;
import com.sequoiacm.infrastructure.slowlog.config.SlowLogConfig;
import com.sequoiacm.infrastructure.slowlog.module.OperationStatistics;
import com.sequoiacm.infrastructure.slowlog.util.SlowLogPrintWriterWrapper;
import com.sequoiacm.infrastructure.slowlog.util.SlowLogServletInputStreamWrapper;
import com.sequoiacm.infrastructure.slowlog.util.SlowLogServletOutputStreamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.instrument.web.TraceFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

// AutoConfig by resources/META-INF/spring.factories
@Component
@Order(ScmSlowLogFilter.ORDER)
@EnableConfigurationProperties(SlowLogConfig.class)
public class ScmSlowLogFilter extends OncePerRequestFilter {

    public static final int ORDER = TraceFilter.ORDER + 1;

    private static final Logger logger = LoggerFactory.getLogger(ScmSlowLogFilter.class);

    private static final String USER_AUTH = "x-auth-token";

    private static final ThreadLocal<SimpleDateFormat> dataFormatLocal = new ThreadLocal<>();

    @Autowired
    private SlowLogConfig slowLogConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (slowLogConfig.isEnabled()) {
            beforeFilter(request);
            try {
                filterChain.doFilter(new SlowLogRequestWrapper(request),
                        new SlowLogResponseWrapper(response));
            }
            finally {
                afterFilter(request);
            }
        }
        else {
            filterChain.doFilter(request, response);
        }

    }

    private void beforeFilter(HttpServletRequest request) {
        SlowLogContext slowLogContext = new SlowLogContextImpl();
        SlowLogManager.setCurrentContext(slowLogContext);
        String path = request.getRequestURI();
        if (request.getQueryString() != null) {
            path = path + "?" + request.getQueryString();
        }
        try {
            slowLogContext.setPath(URLDecoder.decode(path, "utf-8"));
        }
        catch (UnsupportedEncodingException e) {
            slowLogContext.setPath(path);
        }
        slowLogContext.setMethod(request.getMethod());
        slowLogContext.setStart(new Date());
        slowLogContext.setSessionId(request.getHeader(USER_AUTH));
    }

    private void afterFilter(HttpServletRequest request) {
        SlowLogContext logContext = SlowLogManager.getCurrentContext();
        logContext.setEnd(new Date());
        SlowResult slowResult = getSlowResult(logContext, request);
        if (slowResult != null && slowResult.isSlow()) {
            logResult(slowResult, logContext);
        }
        SlowLogManager.setCurrentContext(null);
    }

    private SlowResult getSlowResult(SlowLogContext logContext, HttpServletRequest request) {
        if (!slowLogConfig.isEnabled()) {
            return null;
        }
        SlowResult slowResult = null;
        // 检查操作是否超时
        if (slowLogConfig.isOperationConfigured()) {
            slowResult = new SlowResult();
            for (OperationStatistics statistics : logContext.getOperationStatisticsData()) {
                long threshold = slowLogConfig.getOperationThreshold(statistics.getName());
                if (statistics.getSpend() >= threshold) {
                    slowResult.setSlow(true);
                    slowResult.addSlowOperation(statistics.getName());
                }
            }

            if (slowResult.isSlow()) {
                return slowResult;
            }
        }

        // 检查请求是否超时
        if (slowLogConfig.isRequestConfigured()) {
            if (slowResult == null) {
                slowResult = new SlowResult();
            }
            long threshold = slowLogConfig.getRequestThreshold(request.getMethod(),
                    request.getRequestURI());
            if (logContext.getSpend() >= threshold) {
                slowResult.setSlow(true);
            }
        }
        return slowResult;
    }

    private void logResult(SlowResult slowResult, SlowLogContext logContext) {
        if (slowLogConfig.getAppenderList().size() <= 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Request processing is too slow: spend=").append(logContext.getSpend())
                .append("ms, details=");
        sb.append("[");
        if (logContext.getSessionId() != null) {
            sb.append("sessionId=").append(logContext.getSessionId()).append(", ");
        }
        sb.append("spend=").append(logContext.getSpend()).append("ms, ");
        sb.append("method=").append(logContext.getMethod()).append(", ");
        sb.append("path=").append(logContext.getPath()).append(", ");
        sb.append("start=").append(formatDate(logContext.getStart())).append(", ");
        sb.append("end=").append(formatDate(logContext.getEnd())).append(", ");
        sb.append("readClientSpend=").append(logContext.getReadClientSpend()).append("ms, ");
        sb.append("writeResponseSpend=").append(logContext.getWriteResponseSpend()).append("ms");
        Collection<OperationStatistics> statisticsData = logContext.getOperationStatisticsData();
        if (statisticsData != null && statisticsData.size() > 0) {
            sb.append(", operations=[");
            int n = statisticsData.size();
            for (OperationStatistics statistics : statisticsData) {
                String operation = statistics.getName();
                if (slowResult.isSlowOperation(operation)) {
                    sb.append("*");
                }
                sb.append(operation);
                if (statistics.getCount() > 1) {
                    sb.append("(").append(statistics.getCount()).append(")");
                }
                sb.append("=").append(statistics.getSpend()).append("ms");
                if (--n > 0) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        Map<String, Set<Object>> extras = logContext.getExtras();
        if (extras != null && extras.size() > 0) {
            sb.append(", extras=[");
            int n = extras.size();
            for (Map.Entry<String, Set<Object>> extraEntry : extras.entrySet()) {
                sb.append(extraEntry.getKey()).append("=");
                if (extraEntry.getValue().size() == 1) {
                    sb.append(extraEntry.getValue().iterator().next());
                }
                else {
                    sb.append(extraEntry.getValue());
                }
                if (--n > 0) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        sb.append("]");
        String log = sb.toString();
        for (SlowLogAppender appender : slowLogConfig.getAppenderList()) {
            appender.log(log);
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat dateFormat = dataFormatLocal.get();
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            dataFormatLocal.set(dateFormat);
        }
        return dateFormat.format(date);
    }

    static class SlowResult {

        private List<String> slowOperations;
        private boolean slow = false;

        public SlowResult(boolean slow) {
            this.slow = slow;
        }

        public SlowResult() {
        }

        public void addSlowOperation(String operation) {
            if (slowOperations == null) {
                slowOperations = new ArrayList<>();
            }
            slowOperations.add(operation);
        }

        public boolean isSlowOperation(String operation) {
            if (slowOperations == null) {
                return false;
            }
            return slowOperations.contains(operation);
        }

        public boolean isSlow() {
            return slow;
        }

        public void setSlow(boolean slow) {
            this.slow = slow;
        }
    }

    static class SlowLogRequestWrapper extends HttpServletRequestWrapper {

        private SlowLogServletInputStreamWrapper inputStreamWrapper;

        public SlowLogRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (inputStreamWrapper == null) {
                inputStreamWrapper = new SlowLogServletInputStreamWrapper(super.getInputStream());
            }
            return inputStreamWrapper;
        }
    }

    static class SlowLogResponseWrapper extends HttpServletResponseWrapper {

        private SlowLogServletOutputStreamWrapper outputStreamWrapper;

        private SlowLogPrintWriterWrapper printWriterWrapper;

        public SlowLogResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStreamWrapper == null) {
                outputStreamWrapper = new SlowLogServletOutputStreamWrapper(
                        super.getOutputStream());
            }
            return outputStreamWrapper;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (printWriterWrapper == null) {
                printWriterWrapper = new SlowLogPrintWriterWrapper(super.getWriter());
            }
            return printWriterWrapper;
        }
    }

    public static void main(String[] args) throws Exception {
        SlowLogContext slowLogContext = new SlowLogContextImpl();
        slowLogContext.setStart(new Date());
        slowLogContext.beginOperation("test");
        Thread.sleep(100);
        slowLogContext.beginOperation("a");
        Thread.sleep(100);
        slowLogContext.endOperation();
        slowLogContext.endOperation();

        slowLogContext.setSessionId(UUID.randomUUID().toString());
        slowLogContext.setEnd(new Date());
        slowLogContext.addExtra("fileId", UUID.randomUUID().toString());
        SlowResult slowResult = new SlowResult();
        slowResult.addSlowOperation("test.a");
        new ScmSlowLogFilter().logResult(slowResult, slowLogContext);
    }

}
