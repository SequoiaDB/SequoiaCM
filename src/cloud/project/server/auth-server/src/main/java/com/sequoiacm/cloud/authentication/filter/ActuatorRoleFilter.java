package com.sequoiacm.cloud.authentication.filter;

import com.sequoiacm.infrastructure.security.auth.ScmAuthenticationHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ActuatorRoleFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken newAuth = ScmAuthenticationHelper
                    .newAuthWithActuatorRole((UsernamePasswordAuthenticationToken) authentication);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
        filterChain.doFilter(request, response);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
