package com.sequoiacm.cloud.gateway;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.DefaultHttpFirewall;

import com.sequoiacm.infrastructure.common.SecurityRestField;

public class GatewayHttpFireWall extends DefaultHttpFirewall {
    private static final Logger logger = LoggerFactory.getLogger(GatewayHttpFireWall.class);

    @Override
    public FirewalledRequest getFirewalledRequest(HttpServletRequest request) {
        try {
            return super.getFirewalledRequest(request);
        }
        catch (RequestRejectedException e) {
            String sessionId = request.getHeader(SecurityRestField.SESSION_ATTRIBUTE);
            String target = "unknown host";
            logger.error("send {} request {} from {}:{} to {} with session {} failed(status={})",
                    request.getMethod(), request.getRequestURI(), request.getRemoteHost(),
                    request.getRemotePort(), target, sessionId, HttpStatus.INTERNAL_SERVER_ERROR);
            throw e;
        }
    }
}
