package com.sequoiacm.om.omserver.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.core.ScmOmPasswordMgr;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.service.AuthenticationService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
public class AuthenticationController {
    @Autowired
    private AuthenticationService authSrvice;
    @Autowired
    private ScmOmPasswordMgr passwordMgr;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestParam(RestParamDefine.USERNAME) String username,
            @RequestParam(RestParamDefine.PASSWORD) String encrytedPassword,
            HttpServletResponse resp) throws ScmInternalException, ScmOmServerException {
        String decryptedPwd = passwordMgr.decrypt(encrytedPassword);
        ScmOmSession s = authSrvice.login(username, decryptedPwd);
        return ResponseEntity.ok().header(RestParamDefine.X_AUTH_TOKEN, s.getSessionId()).build();
    }

    @PostMapping("/logout")
    public void logout(ScmOmSession session) throws ScmOmServerException {
        authSrvice.logout(session);
    }
}
