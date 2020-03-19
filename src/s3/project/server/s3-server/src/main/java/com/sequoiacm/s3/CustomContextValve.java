package com.sequoiacm.s3;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.s3.authoriztion.S3ContextValveBase;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.core.ObjectMeta;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.service.BucketService;

@Component
public class CustomContextValve extends S3ContextValveBase {
    private static final Logger logger = LoggerFactory.getLogger(CustomContextValve.class);
    @Autowired
    BucketService bucketService;

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        logger.info(" expect valve.");
        if (request.getHeader(RestParamDefine.EXPECT) != null && request.getMethod().equals("PUT")
                && request.getContentLength() > 512 * 1024) {
            logger.info("get expect.");
            try {
                ObjectMeta meta = createObjectMeta(request);
                if (meta != null) {
                    ScmSession session = (ScmSession) request
                            .getAttribute(ScmSession.class.getName());
                    bucketService.getBucket(session, meta.getBucketName());

                    if (request.getParameter(RestParamDefine.PARTNUMBER) != null
                            || request.getParameter(RestParamDefine.UPLOADID) != null) {
                        throw new S3ServerException(S3Error.PARAMETER_NOT_SUPPORT,
                                "unsupport multipart upload object");
                    }

                    if (meta.getKey().length() > RestParamDefine.KEY_LENGTH) {
                        throw new S3ServerException(S3Error.OBJECT_KEY_TOO_LONG,
                                "ObjectName is too long. objectName:" + meta.getKey().length());
                    }
                    request.setAttribute(ObjectMeta.class.getName(), meta);
                }

            }
            catch (S3ServerException e) {
                handleS3Error(request, response, e);
                return;
            }
        }
        invokeNext(request, response);
    }

    private ObjectMeta createObjectMeta(Request request) throws S3ServerException {
        return new ObjectMeta(request);
    }
}
