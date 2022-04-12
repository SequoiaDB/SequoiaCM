package com.sequoiacm.s3.authoriztion;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.security.auth.ScmUserWrapper;
import com.sequoiacm.infrastructure.security.sign.EnableSignClient;
import com.sequoiacm.infrastructure.security.sign.SignClient;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.infrastructure.security.sign.SignatureInfo;
import com.sequoiacm.s3.config.ScmSessionConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

@EnableSignClient
@Component
public class ScmSessionMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmSessionMgr.class);
    @Autowired
    private SignClient signClient;

    // accesskey map session

    private Map<String, ScmSession> sessions = new ConcurrentHashMap<>();

    private ScmTimer timer = ScmTimerFactory.createScmTimer();

    @Autowired
    public ScmSessionMgr(ScmSessionConfig config) {
        timer.schedule(new SessionCleaner(this), 0, config.getCheckInterval());
    }

    public ScmSession createSessionByUsername(String username, String password)
            throws S3ServerException {
        try {
            String sessionId = signClient.loginWithUsername(username, password);
            ScmUserWrapper userDetail = signClient.getUserDetail(sessionId);
            return new ScmSession(null, sessionId, userDetail);
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.SCM_AUTH_FAILED, "login failed", e);
        }
    }

    public ScmSession getSession(S3Authorization authorization) throws S3ServerException {
        ScmSession session = sessions.get(authorization.getAccesskey());
        if (session != null) {
            checkAuthorization(session, authorization);
            return session;
        }
        ScmSession newSession = createSession(authorization);
        session = sessions.putIfAbsent(authorization.getAccesskey(), newSession);
        if (session == null) {
            return newSession;
        }
        logoutSession(newSession);
        return session;
    }

    public void logoutSession(ScmSession ss) {
        if (ss == null) {
            return;
        }
        try {
            signClient.logout(ss.getSessionId());
        }
        catch (Exception e) {
            logger.warn("failed to logout:{}", ss.getSessionId(), e);
        }
    }

    private ScmSession createSession(S3Authorization authorization) throws S3ServerException {
        String sessionId, secretKey = null;
        try {
            secretKey = signClient.getSecretKey(authorization.getAccesskey());
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND.value()) {
                throw new S3ServerException(S3Error.INVALID_ACCESSKEYID,
                        "accesskey not found:" + authorization.getAccesskey(), e);
            }
            throw new S3ServerException(S3Error.SCM_AUTH_FAILED,
                    "failed to get secretKey from auth server, accesskey:" +authorization.getAccesskey(), e);
        }
        try {
            sessionId = signClient.loginWithSignature(
                    new SignatureInfo(authorization.getAlgorithm(), authorization.getAccesskey(),
                            authorization.getSecretkeyPrefix(), authorization.getSignature(),
                            authorization.getSignatureEncoder(), authorization.getStringToSign()));
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
                throw new S3ServerException(S3Error.SIGNATURE_NOT_MATCH, "singnature not match", e);
            }
            throw new S3ServerException(S3Error.SCM_AUTH_FAILED, "failed to login", e);
        }
        try {
            ScmUserWrapper userDetail = signClient.getUserDetail(sessionId);
            return new ScmSession(secretKey, sessionId, userDetail);
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.SCM_AUTH_FAILED,
                    "failed to get session detail from auth server", e);
        }

    }

    private void checkAuthorization(ScmSession session, S3Authorization authentication)
            throws S3ServerException {
        String signatureServerSide = SignUtil.sign(authentication.getAlgorithm(),
                authentication.getSecretkeyPrefix() + session.getSecretkey(),
                authentication.getStringToSign(), authentication.getSignatureEncoder());
        if (signatureServerSide.equals(authentication.getSignature())) {
            return;
        }
        logger.error("signature does not match, authorization=" + authentication
                + ", signatureServerSide=" + signatureServerSide);
        throw new S3ServerException(S3Error.SIGNATURE_NOT_MATCH, "Signature does not match.");
    }

    void cleanInvalidSession() {
        Iterator<Entry<String, ScmSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, ScmSession> entry = it.next();
            ScmSession session = entry.getValue();
            try {
                signClient.getUserDetail(session.getSessionId());
            }
            catch (Exception e) {
                logger.info(
                        "assume session is not exist in auth-server, remove it from local cache:accessKey={}, sessionId={}",
                        entry.getKey(), session.getSessionId(), e);
                logoutSession(session);
                it.remove();
            }
        }
    }

    @PreDestroy
    void destory() {
        timer.cancel();
        for (ScmSession session : sessions.values()) {
            logoutSession(session);
        }
    }

}

class SessionCleaner extends ScmTimerTask {
    private ScmSessionMgr sessionMgr;

    public SessionCleaner(ScmSessionMgr sessionMgr) {
        this.sessionMgr = sessionMgr;
    }

    @Override
    public void run() {
        sessionMgr.cleanInvalidSession();
    }

}
