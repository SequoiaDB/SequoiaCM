package com.sequoiacm.cloud.authentication.controller;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.cloud.authentication.exception.BadRequestException;
import com.sequoiacm.cloud.authentication.exception.NotFoundException;
import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUser.ScmUserBuilder;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.infrastructure.security.sign.SignatureInfo;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

@RequestMapping()
@RestController
public class AccesskeyController {
    private static final Logger logger = LoggerFactory.getLogger(AccesskeyController.class);
    private static final String[] AccesskeyChars = { "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
    private static final String[] SecretkeyChars = { "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
            "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9" };

    @Autowired
    private ScmUserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/internal/v1/secretkey")
    public AccesskeyInfo getSecretkey(@RequestParam("accesskey") String accesskey) {
        ScmUser user = userRoleRepository.findUserByAccesskey(accesskey);
        if (user == null) {
            throw new NotFoundException("Accesskey is not found: " + accesskey);
        }
        return new AccesskeyInfo(accesskey, user.getSecretkey(), user.getUsername());
    }

    @PostMapping(value = "/api/v1/accesskey", params = "action=refresh")
    public AccesskeyInfo refreshAccesskey(Authentication auth,
            @RequestParam(value = RestField.USERNAME, required = false) String username,
            @RequestParam(value = RestField.PASSWORD, required = false) String password,
            @RequestParam(value = RestField.SIGNATURE_INFO, required = false) SignatureInfo signatureInfo)
            throws Exception {
        ScmUser currentUser = (ScmUser) auth.getPrincipal();
        ScmUser targetUser;
        if (username != null) {
            if (signatureInfo != null) {
                throw new BadRequestException(
                        "can not specify username accesskey signature_info at the same time");
            }
            targetUser = userRoleRepository.findUserByName(username);
            if (targetUser == null) {
                throw new NotFoundException("Username is not found: " + username);
            }
            // check password:
            // 1. i am not admin
            // 2. i am admin and refresh myself
            if (!currentUser.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)
                    || currentUser.getUsername().equals(username)) {
                if (password == null) {
                    throw new BadRequestException("please specify password");
                }
                String srcPassword;
                try {
                    srcPassword = ScmPasswordMgr.getInstance()
                            .decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password);
                }
                catch (Exception e) {
                    throw new BadRequestException("failed to decrypt password");
                }
                if (!passwordEncoder.matches(srcPassword, targetUser.getPassword())) {
                    throw new BadRequestException("Incorrect password for user " + username);
                }
            }
        }
        else if (signatureInfo != null) {
            if (username != null || password != null) {
                throw new BadRequestException(
                        "can not specify accesskey username signature_info at the same time");
            }
            targetUser = userRoleRepository.findUserByAccesskey(signatureInfo.getAccessKey());
            if (targetUser == null) {
                throw new NotFoundException(
                        "Accesskey is not found: " + signatureInfo.getAccessKey());
            }
            if (!currentUser.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)
                    || targetUser.getAccesskey().equals(signatureInfo.getAccessKey())) {
                if (signatureInfo.getSignature() == null) {
                    throw new BadRequestException("please specify signature");
                }

                String serverSideSignatrue = SignUtil.sign(signatureInfo.getAlgothm(),
                        signatureInfo.getSecretKeyPrefix() + targetUser.getSecretkey(),
                        signatureInfo.getStringToSign());
                if (!serverSideSignatrue.equals(signatureInfo.getSignature())) {
                    logger.error("incorrect signature:{}", signatureInfo);
                    throw new BadRequestException("Incorrect signature");
                }
            }
        }
        else {
            throw new BadRequestException(
                    "please sepcify a set of valid parameters: username + password or accesskey + secretkey");
        }

        ScmUserBuilder userBuilder = ScmUser.copyFrom(targetUser);
        userBuilder.accesskey(genRandomKey(AccesskeyChars, 20));
        userBuilder.secretkey(genRandomKey(SecretkeyChars, 40));
        ScmUser refreshedUser = userBuilder.build();
        try {
            userRoleRepository.updateUser(refreshedUser, null);
            return new AccesskeyInfo(refreshedUser.getAccesskey(), refreshedUser.getSecretkey(),
                    username);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                userBuilder.accesskey(genRandomKey(AccesskeyChars, 20));
                refreshedUser = userBuilder.build();
                userRoleRepository.updateUser(refreshedUser, null);
                return new AccesskeyInfo(refreshedUser.getAccesskey(), refreshedUser.getSecretkey(),
                        username);
            }
            throw e;
        }
    }

    private String genRandomKey(String[] source, int keyLen) {
        Random random = new Random();
        StringBuffer key = new StringBuffer();
        for (int index = 0; index < keyLen; index++) {
            int number = random.nextInt(source.length);
            key.append(source[number]);
        }
        return key.toString();
    }
}
