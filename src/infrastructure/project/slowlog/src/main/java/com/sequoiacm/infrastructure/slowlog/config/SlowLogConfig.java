package com.sequoiacm.infrastructure.slowlog.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Import;
import org.springframework.util.AntPathMatcher;

import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;
import com.sequoiacm.infrastructure.slowlog.appender.SlowLogAppender;
import com.sequoiacm.infrastructure.slowlog.appender.SlowLogAppenderFactory;

@ConfigurationProperties(prefix = "scm.slowlog")
@RefreshScope
@Import(SlowLogAppenderFactory.class)
public class SlowLogConfig {

    private static final Logger logger = LoggerFactory.getLogger(SlowLogConfig.class);
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @ScmRefreshableConfigMarker
    private boolean enabled = false;

    @ScmRefreshableConfigMarker
    private Long allRequest = -1L;

    @ScmRefreshableConfigMarker
    private Long allOperation = -1L;

    @ScmRefreshableConfigMarker
    private Map<String, Long> request = new HashMap<>();

    @ScmRefreshableConfigMarker
    private Map<String, Long> operation = new HashMap<>();

    private List<SlowLogRequest> parsedRequestList = Collections.emptyList();

    @ScmRefreshableConfigMarker
    private String appender;

    private List<SlowLogAppender> appenderList = new ArrayList<>();

    @Autowired
    private SlowLogAppenderFactory slowLogAppenderFactory;

    public boolean isEnabled() {
        return enabled;
    }

    @PostConstruct
    public void init() {
        this.parsedRequestList = parseRequest();
        parseOperation();
        initAppender();
    }

    private void initAppender() {
        appenderList.clear();
        if (appender == null) {
            appenderList = slowLogAppenderFactory.createDefaultAppenderList();
        }
        else {
            Set<String> existAppenderSet = new HashSet<>();
            for (String appenderName : appender.split(",")) {
                SlowLogAppender slowLogAppender = slowLogAppenderFactory
                        .createAppender(appenderName);
                if (slowLogAppender == null) {
                    logger.warn("unrecognized slowlog appender name: " + appenderName);
                }
                else {
                    if (existAppenderSet.contains(slowLogAppender.getName())) {
                        continue;
                    }
                    appenderList.add(slowLogAppender);
                    existAppenderSet.add(slowLogAppender.getName());
                }
            }
        }
    }

    @PreDestroy
    public void destroy() {
        // 需要将 request、operation 重置，否则通过配置刷新功能删除这两个 map 中的元素时会无效。
        this.request = new HashMap<>();
        this.operation = new HashMap<>();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getAllRequest() {
        return allRequest;
    }

    public void setAllRequest(Long allRequest) {
        if(allRequest == null){
            logger.warn("unrecognized slowlog request config: key={}, value={}",
                    "scm.slowlog.allRequest", allRequest);
            this.allRequest = -1L;
            logger.info("The value of allRequest has been modified to the default value:-1");
        }else {
            this.allRequest = allRequest;
        }
    }

    public Long getAllOperation() {
        return allOperation;
    }

    public void setAllOperation(Long allOperation) {
        if(allOperation == null){
            logger.warn("unrecognized slowlog operation config: key={}, value={}",
                    "scm.slowlog.allOperation", allOperation);
            this.allOperation = -1L;
            logger.info("The value of allOperation has been modified to the default value:-1");
        }else {
            this.allOperation = allOperation;
        }
    }

    public Map<String, Long> getRequest() {
        return request;
    }

    public void setRequest(Map<String, Long> request) {
        this.request = request;
    }

    public Map<String, Long> getOperation() {
        return operation;
    }

    public void setOperation(Map<String, Long> operation) {
        this.operation = operation;
    }

    public long getAllRequestThreshold() {
        return allRequest < 0 ? Long.MAX_VALUE : allRequest;
    }

    public long getAllOperationThreshold() {
        return allOperation < 0 ? Long.MAX_VALUE : allOperation;
    }

    public long getOperationThreshold(String operationName) {
        Long threshold = operation.get(operationName);
        return threshold != null ? threshold : getAllOperationThreshold();
    }

    public long getRequestThreshold(String method, String uri) {
        for (SlowLogRequest req : parsedRequestList) {
            if (req.getMethod().equalsIgnoreCase(method) && req.isURIMatch(uri)) {
                return req.getThreshold();
            }
        }
        return getAllRequestThreshold();
    }

    public boolean isOperationConfigured() {
        return !operation.isEmpty() || allOperation >= 0;
    }

    public boolean isRequestConfigured() {
        return !request.isEmpty() || allRequest >= 0;
    }

    public void setAppender(String appender) {
        this.appender = appender;
    }

    public List<SlowLogAppender> getAppenderList() {
        return appenderList;
    }

    private List<SlowLogRequest> parseRequest() {
        if (request != null && request.size() > 0) {
            List<SlowLogRequest> list = new ArrayList<>(request.size());
            Iterator<Map.Entry<String, Long>> it = request.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> reqEntry = it.next();
                String methodAndPattern = reqEntry.getKey();
                int i = methodAndPattern.indexOf("/");
                if (i == -1 || reqEntry.getValue() == null) {
                    logger.warn("unrecognized slowlog request config: key={}, value={}",
                            reqEntry.getKey(), reqEntry.getValue());
                    it.remove();
                    continue;
                }
                String method = methodAndPattern.substring(0, i);
                String parttern = methodAndPattern.substring(i);
                if (!parttern.endsWith("/")) {
                    parttern += "/";
                }
                list.add(new SlowLogConfig.SlowLogRequest(method, parttern, reqEntry.getValue()));
            }
            return list;
        }
        else {
            return Collections.emptyList();
        }
    }

    public void parseOperation() {
        Set<String> keySet = operation.keySet();
        Iterator<String> keyIterator = keySet.iterator();
        while (keyIterator.hasNext()){
            String key = keyIterator.next();
            Long value = operation.get(key);
            if(value==null){
                logger.warn("unrecognized slowlog operation config: key={}, value={}",
                        key, "null");
                keyIterator.remove();
                logger.info("slowlog.operation.{} has been removed",
                        key);
            }
        }
    }

    private static class SlowLogRequest {
        private String method;
        private String pattern;
        private long threshold;

        public SlowLogRequest(String method, String pattern, long threshold) {
            this.method = method;
            this.pattern = pattern;
            this.threshold = threshold;
        }

        public long getThreshold() {
            return threshold;
        }

        public void setThreshold(long threshold) {
            this.threshold = threshold;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public boolean isURIMatch(String targetUri) {
            if (targetUri == null) {
                return false;
            }
            if (!targetUri.endsWith("/")) {
                targetUri += "/";
            }
            return antPathMatcher.match(pattern, targetUri);
        }
    }

}
