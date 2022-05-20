package com.sequoiacm.infrastructure.monitor.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
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

    private final Map<String, Endpoint<?>> urlEndpointMap = new HashMap<>();

    private final Map<String, String> paramsMap;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ManagementServerProperties managementServerProperties;

    public ActuatorEndpointFilter(List<Endpoint<?>> endpoints,
            DefaultOmMonitorConfigure omMonitorConfigure,
            ManagementServerProperties managementServerProperties) {
        this.paramsMap = omMonitorConfigure.configureEndpointUrlParams();
        if (endpoints != null) {
            for (Endpoint<?> endpoint : endpoints) {
                this.urlEndpointMap.put("/" + endpoint.getId(), endpoint);
            }
        }
        this.managementServerProperties = managementServerProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        Endpoint<?> endpoint = urlEndpointMap.get(requestURI);
        if (endpoint != null && endpoint.isEnabled() && isParamsMatch(request)) {
            if (isUserAllowAccess(endpoint)) {
                response.setContentType(APPLICATION_JSON_UTF8);
                PrintWriter writer = response.getWriter();
                writer.write(objectMapper.writeValueAsString(endpoint.invoke()));
                writer.flush();
            }
            else {
                if (request.getUserPrincipal() != null) {
                    String roles = StringUtils.collectionToDelimitedString(
                            this.managementServerProperties.getSecurity().getRoles(), " ");
                    response.sendError(HttpStatus.FORBIDDEN.value(),
                            "Access is denied. User must have one of the these roles: " + roles);
                }
                else {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(),
                            "Full authentication is required to access this resource.");
                }
            }
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

    private boolean isUserAllowAccess(Endpoint<?> endpoint) {
        if (!managementServerProperties.getSecurity().isEnabled() || !endpoint.isSensitive()) {
            return true;
        }
        for (String role : managementServerProperties.getSecurity().getRoles()) {
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }
            if (hasAuthority(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAuthority(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (authority.getAuthority().equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}
