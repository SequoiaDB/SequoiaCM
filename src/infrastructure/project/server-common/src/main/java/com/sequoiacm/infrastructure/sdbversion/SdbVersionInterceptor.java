package com.sequoiacm.infrastructure.sdbversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SdbVersionInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(SdbVersionInterceptor.class);
    @Autowired
    private SdbVersionChecker sdbVersionChecker;

    @Autowired
    private Environment env;

    // 字符串版本范围 -> 版本范围对象
    private Map<String, List<VersionRange>> cache = new ConcurrentHashMap<>();

    public SdbVersionInterceptor(SdbVersionChecker sdbVersionChecker, Environment env) {
        this.sdbVersionChecker = sdbVersionChecker;
        this.env = env;
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse, Object o) throws Exception {
        if (!(o instanceof HandlerMethod)) {
            logger.debug("not a handler method, ignore to check sdb version: {}", o);
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) o;
        Method method = handlerMethod.getMethod();
        if (method == null) {
            logger.debug("method is null, ignore to check sdb version: {}", o);
            return true;
        }
        RequireSdbVersion requireSdbVersionAnnotation = method
                .getAnnotation(RequireSdbVersion.class);
        if (requireSdbVersionAnnotation == null) {
            return true;
        }
        String requiredVersionStr = env.getProperty(requireSdbVersionAnnotation.versionProperty());
        if (requiredVersionStr == null) {
            requiredVersionStr = requireSdbVersionAnnotation.defaultVersion();
        }
        if (requiredVersionStr == null || requiredVersionStr.isEmpty()) {
            throw new IllegalArgumentException(
                    "required version is empty: " + requireSdbVersionAnnotation.versionProperty());
        }

        List<VersionRange> requireVersionRange = createSdbVersionRange(requiredVersionStr);
        if (!sdbVersionChecker.isCompatible(requireVersionRange)) {
            throw new SdbVersionIncompitableException(requireVersionRange,
                    sdbVersionChecker.getSdbVersion());
        }

        return true;
    }

    private List<VersionRange> createSdbVersionRange(String requiredVersionStr) {
        List<VersionRange> ret = cache.get(requiredVersionStr);
        if (ret != null) {
            return ret;
        }
        ret = VersionRange.parse(requiredVersionStr);
        cache.put(requiredVersionStr, ret);
        if (cache.size() > 2000) {
            // 正常一个系统不会有那么多的版本要求定义，这里只是为了防止意外内存泄漏
            logger.error("cache size is too large, clear it: {}", cache.size());
            cache.clear();
        }
        return ret;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView)
            throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
