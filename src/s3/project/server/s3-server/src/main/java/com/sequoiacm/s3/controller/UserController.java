package com.sequoiacm.s3.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.service.UserService;

@RestController
@RequestMapping(RestParamDefine.REST_USERS)
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private UserService userService;

    @PostMapping(params = RestParamDefine.UserPara.REFRESH_ACCESSKEY)
    public AccesskeyInfo createAccessKey(
            @RequestParam(RestParamDefine.TARGET_USERNAME) String targetUser,
            @RequestParam(RestParamDefine.USERNAME) String username,
            @RequestParam(RestParamDefine.PASSWORD) String encryptPassword)
            throws S3ServerException {
        logger.info("Refresh AccessKey. user={}", username);
        return userService.refreshAccesskey(targetUser, username, encryptPassword);
    }
}
