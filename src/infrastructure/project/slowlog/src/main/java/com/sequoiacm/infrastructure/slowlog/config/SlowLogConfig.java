package com.sequoiacm.infrastructure.slowlog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.AntPathMatcher;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

@ConfigurationProperties(prefix = "scm.slowlog")
@RefreshScope
public class SlowLogConfig {

    private static final Logger logger = LoggerFactory.getLogger(SlowLogConfig.class);
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private boolean enabled = false;

    private Long allRequest = -1L;

    private Long allOperation = -1L;

    private Map<String, Long> request = new HashMap<>();

    private Map<String, Long> operation = new HashMap<>();

    private List<SlowLogRequest> parsedRequestList = Collections.emptyList();

    public boolean isEnabled() {
        return enabled;
    }

    @PostConstruct
    public void init() {
        this.parsedRequestList = parseRequest();
        parseOperation();
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

    private List<SlowLogRequest> parseRequest() {
        if (request != null && request.size() > 0) {
            List<SlowLogRequest> list = new ArrayList<>(request.size());
            for (Map.Entry<String, Long> reqEntry : request.entrySet()) {
                String methodAndPattern = reqEntry.getKey();
                int i = methodAndPattern.indexOf("/");
                if (i == -1 || reqEntry.getValue() == null) {
                    logger.warn("unrecognized slowlog request config: key={}, value={}",
                            reqEntry.getKey(), reqEntry.getValue());
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
