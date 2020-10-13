package com.sequoiacm.cloud.authentication.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sequoiacm.infrastructure.common.ConcurrentLruMap;

public class SCMPasswordEncoder implements PasswordEncoder {
    private static final Logger logger = LoggerFactory.getLogger(SCMPasswordEncoder.class);
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private Md5PasswordEncoder md5Encoder = new Md5PasswordEncoder();
    @Autowired
    private ConcurrentLruMap<String, String> passCache;

    public SCMPasswordEncoder() {
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();

    }

    public String encodePassword(String rawPass, Object salt) {
        if (salt == null) {
            return bCryptPasswordEncoder.encode(rawPass);
        }
        return BCrypt.hashpw(rawPass, (String) salt);
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
        UserDetails user = null;
        if (salt == null) {
            return false;
        }
        else if (salt instanceof UserDetails) {
            user = (UserDetails) salt;
        }
        else {
            logger.debug("Authentication failed: salt must be an instance of UserDetails");

            throw new IllegalArgumentException(
                    "parameter salt type error,please input an instance of UserDetails as salt");
        }

        if (encPass == null || rawPass == null) {
            return false;
        }

        String cacheKey = md5Encoder.encodePassword(user.getUsername() + rawPass, null);
        String cachePass = passCache.get(cacheKey);

        String pass1 = encPass;

        if (cachePass == null) {
            String pass2 = encodePassword(rawPass, user.getPassword());

            if (pass1.equals(pass2)) {
                passCache.put(cacheKey, pass2);
                return true;
            }
        }
        else {
            if (pass1.equals(cachePass)) {
                return true;
            }
            passCache.remove(cacheKey);
            String pass2 = encodePassword(rawPass, user.getPassword());
            if (pass1.equals(pass2)) {
                passCache.put(cacheKey, pass2);
                return true;
            }
        }
        return false;
    }
}
