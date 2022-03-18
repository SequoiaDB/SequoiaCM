package com.sequoiacm.infrastructure.monitor.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ActuatorEndpointFilter extends OncePerRequestFilter {

    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=utf-8";

    private final Map<String, AbstractEndpoint<?>> urlEndpointMap = new HashMap<>();

    private final Map<String, String> paramsMap;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActuatorEndpointFilter(List<AbstractEndpoint<?>> endpoints,
            DefaultOmMonitorConfigure omMonitorConfigure) {
        this.paramsMap = omMonitorConfigure.configureEndpointUrlParams();
        if (endpoints != null) {
            for (AbstractEndpoint<?> endpoint : endpoints) {
                this.urlEndpointMap.put("/" + endpoint.getId(), endpoint);
            }
        }

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        AbstractEndpoint<?> endpoint = urlEndpointMap.get(requestURI);
        if (endpoint != null && endpoint.isEnabled() && isParamsMatch(request)) {
            response.setContentType(APPLICATION_JSON_UTF8);
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(endpoint.invoke()));
            writer.flush();
        }
        else {
            filterChain.doFilter(request, response);
        }

    }

    private boolean isParamsMatch(HttpServletRequest request) {
        if (paramsMap == null) {
            return true;
        }
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            if (!Objects.equals(request.getParameter(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
