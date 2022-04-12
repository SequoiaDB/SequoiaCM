package com.sequoiacm.s3.authoriztion;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.s3.config.AuthorizationConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

@Component
public class S3AuthorizationValve extends S3ContextValveBase {
    private static final Logger logger = LoggerFactory.getLogger(S3AuthorizationValve.class);
    @Autowired
    private ScmSessionMgr sessionMgr;

    @Autowired
    private AuthorizationConfig authConfig;

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            S3Authorization auth = S3AuthorizationFactory.createAuthentication(request);
            logger.debug("s3 authorization: {}", auth);
            if (auth == null) {
                invokeNext(request, response);
                return;
            }
            Date now = new Date();
            if (authConfig.getMaxTimeOffset() > 0
                    && Math.abs(auth.getSignDate().getTime() - now.getTime()) > authConfig
                            .getMaxTimeOffset()) {
                throw new S3ServerException(S3Error.REQUEST_TIME_TOO_SKEWED,
                        "request time too skewed:requestTime=" + auth.getSignDate()
                                + ", serverTime=" + now);
            }

            ScmSession session = sessionMgr.getSession(auth);
            request.setAttribute(ScmSession.class.getName(), session);
        }
        catch (S3ServerException e) {
            handleS3Error(request, response, e);
            return;
        }
        invokeNext(request, response);
    }
}
