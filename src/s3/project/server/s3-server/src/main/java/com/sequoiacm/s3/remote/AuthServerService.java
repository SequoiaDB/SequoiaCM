package com.sequoiacm.s3.remote;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.common.SecurityRestField;

public interface AuthServerService {

    @PostMapping(value = "/api/v1/accesskey")
    public AccesskeyInfo refreshAccesskey(
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(SecurityRestField.USER_ATTRIBUTE) String user,
            @RequestParam("action") String acntion,
            @RequestParam(value = SecurityRestField.USERNAME, required = false) String username,
            @RequestParam(value = SecurityRestField.PASSWORD, required = false) String password)
            throws ScmFeignException;
}
