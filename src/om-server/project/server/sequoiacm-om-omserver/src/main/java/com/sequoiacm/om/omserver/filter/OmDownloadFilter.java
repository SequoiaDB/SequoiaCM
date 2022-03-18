package com.sequoiacm.om.omserver.filter;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import org.springframework.http.HttpMethod;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

@WebFilter(urlPatterns = "/api/v1/files/id/*")
public class OmDownloadFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    // 过滤文件下载请求，将参数中的 token 添加至请求头（下载文件时 token 使用参数传递）
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        OmRequestWrapper requestWrapper = new OmRequestWrapper(req);
        if (requestWrapper.getMethod().equals(HttpMethod.GET.name())) {
            String token = requestWrapper.getParameter(RestParamDefine.X_AUTH_TOKEN);
            requestWrapper.addHeader(RestParamDefine.X_AUTH_TOKEN, token);
        }
        filterChain.doFilter(requestWrapper, servletResponse);
    }

    @Override
    public void destroy() {

    }
}

class OmRequestWrapper extends HttpServletRequestWrapper {

    private Map<String, String> headerMap = new HashMap<>();

    public OmRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public void addHeader(String name, String value) {
        headerMap.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = headerMap.get(name);
        return headerValue == null ? super.getHeader(name) : headerValue;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headerNames = Collections.list(super.getHeaderNames());
        for (String name : headerMap.keySet()) {
            headerNames.add(name);
        }
        return Collections.enumeration(headerNames);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = Collections.list(super.getHeaders(name));
        if (headerMap.containsKey(name)) {
            values = Arrays.asList(headerMap.get(name));
        }
        return Collections.enumeration(values);
    }
}
