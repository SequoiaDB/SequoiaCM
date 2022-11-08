package com.sequoiacm.infrastructure.security.sign;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.security.auth.RestField;

import feign.Response;

public interface SignService {
    @PostMapping("/login")
    public Response login(@RequestParam(RestField.SIGNATURE_INFO) BSONObject signatureInfo)
            throws ScmFeignException;

    @PostMapping("/login")
    public Response login(@RequestParam(RestField.USERNAME) String username,
            @RequestParam(RestField.PASSWORD) String password) throws ScmFeignException;

    @PostMapping("/v2/localLogin")
    public Response v2localLogin(@RequestHeader(RestField.SIGNATURE_DATE) String date,
            @RequestParam(RestField.USERNAME) String username,
            @RequestParam(RestField.PASSWORD) String password) throws ScmFeignException;

    @PostMapping("/api/v2/salt")
    public BSONObject getSalt(@RequestParam(value = "username") String name)
            throws ScmFeignException;

    @GetMapping("/internal/v1/secretkey")
    public BSONObject getSecretkey(@RequestParam(RestField.ACCESSKEY) String accesskey)
            throws ScmFeignException;

    @GetMapping("/api/v1/accesskey?action=refresh")
    public BSONObject refreshAccesskey(@RequestParam(RestField.SESSION_ATTRIBUTE) String sessionID,
            @RequestParam(RestField.USERNAME) String username,
            @RequestParam(RestField.PASSWORD) String password,
            @RequestParam(RestField.SIGNATURE_INFO) BSONObject signatureInfo)
            throws ScmFeignException;

    @GetMapping("/api/v1/users/{username}")
    public BSONObject findUser(@PathVariable(value = "username") String name)
            throws ScmFeignException;

}
