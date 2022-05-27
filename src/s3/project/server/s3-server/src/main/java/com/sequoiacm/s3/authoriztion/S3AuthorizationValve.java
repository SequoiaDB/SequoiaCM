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

import com.sequoiacm.s3.exception.S3ServerException;

@Component
public class S3AuthorizationValve extends S3ContextValveBase {
    private static final Logger logger = LoggerFactory.getLogger(S3AuthorizationValve.class);
    @Autowired
    private ScmSessionMgr sessionMgr;

    @Autowired
    S3AuthorizationFactory s3AuthorizationFactory;

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            S3Authorization auth = s3AuthorizationFactory.createAuthentication(request);
            logger.debug("s3 authorization: {}", auth);
            if (auth == null) {
                invokeNext(request, response);
                return;
            }

            auth.checkExpires(new Date());

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
