package com.sequoiacm.infrastructure.security.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructure.feign.ScmFeignException;

public interface ScmSessionService {

    @GetMapping("/api/v1/sessions/{sessionId}")
    String getSession(@RequestHeader(value = RestField.SESSION_ATTRIBUTE) String token,
            @PathVariable(value = "sessionId") String sessionId,
            @RequestParam(value = RestField.USER_DETAILS, required = false) Boolean userDetails)
            throws ScmFeignException;
    
    @PostMapping("/logout")
    void logout(@RequestHeader(value = RestField.SESSION_ATTRIBUTE) String token)
            throws ScmFeignException;
}
