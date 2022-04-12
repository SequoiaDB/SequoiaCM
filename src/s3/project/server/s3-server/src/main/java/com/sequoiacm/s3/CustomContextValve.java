package com.sequoiacm.s3;

import com.sequoiacm.s3.authoriztion.S3ContextValveBase;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ObjectUri;
import com.sequoiacm.s3.service.BucketService;
import com.sequoiacm.s3.utils.RestUtils;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomContextValve extends S3ContextValveBase {
    private static final Logger logger = LoggerFactory.getLogger(CustomContextValve.class);
    @Autowired
    BucketService bucketService;

    @Autowired
    RestUtils restUtils;

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (request.getHeader(RestParamDefine.EXPECT) != null && request.getMethod().equals("PUT")
                && request.getContentLength() > 512 * 1024) {
            try {
                ObjectUri objectUri = null;
                try {
                    objectUri = new ObjectUri(request.getRequestURI());
                }
                catch (Exception e) {
                    // do nothing. 只对对象的put操作做检查，如果不是对象操作，不做下面的检查
                }
                if (objectUri != null) {
                    if (objectUri.getObjectName().length() > RestParamDefine.KEY_LENGTH) {
                        throw new S3ServerException(S3Error.OBJECT_KEY_TOO_LONG,
                                "ObjectName is too long. objectName:" + objectUri.getObjectName());
                    }
                    ScmSession session = (ScmSession) request
                            .getAttribute(ScmSession.class.getName());
                    bucketService.getBucket(session, objectUri.getBucketName());
                    Map<String, String> requestHeaders = new HashMap<>();
                    Map<String, String> xMeta = new HashMap<>();
                    restUtils.getHeaders(request, requestHeaders, xMeta);
                    if (restUtils.getXMetaLength(xMeta) > RestParamDefine.X_AMZ_META_LENGTH) {
                        throw new S3ServerException(S3Error.OBJECT_METADATA_TOO_LARGE,
                                "metadata headers exceed the maximum. xMeta:" + xMeta.toString());
                    }
                    request.setAttribute(RestParamDefine.Attribute.S3_HEADERS, requestHeaders);
                    request.setAttribute(RestParamDefine.Attribute.S3_XMETA, xMeta);
                    request.setAttribute(RestParamDefine.Attribute.S3_OBJECTURI, objectUri);
                }
            }
            catch (S3ServerException e) {
                handleS3Error(request, response, e);
                return;
            }
        }
        invokeNext(request, response);
    }
}
