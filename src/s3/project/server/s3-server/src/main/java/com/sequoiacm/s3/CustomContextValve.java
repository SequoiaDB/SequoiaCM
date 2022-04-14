package com.sequoiacm.s3;

import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.s3.authoriztion.S3ContextValveBase;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.dao.UploadDao;
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
    private IScmBucketService scmBucketService;

    @Autowired
    RestUtils restUtils;

    @Autowired
    UploadDao uploadDao;

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
                    ScmSession session = (ScmSession) request
                            .getAttribute(ScmSession.class.getName());
                    ScmBucket scmbucket = getBucket(session.getUser(), objectUri.getBucketName());

                    if (request.getParameter(RestParamDefine.UPLOADID) != null) {
                        long uploadId = restUtils
                                .convertUploadId(request.getParameter(RestParamDefine.UPLOADID));
                        UploadMeta upload = uploadDao.queryUpload(scmbucket.getId(),
                                objectUri.getObjectName(), uploadId);
                        if (upload == null || upload
                                .getUploadStatus() != S3CommonDefine.UploadStatus.UPLOAD_INIT) {
                            throw new S3ServerException(S3Error.PART_NO_SUCH_UPLOAD,
                                    "no such upload. uploadId:" + uploadId);
                        }
                        request.setAttribute(RestParamDefine.Attribute.S3_UPLOADID, uploadId);
                    }

                    // uploadPart
                    if (request.getParameter(RestParamDefine.PARTNUMBER) != null) {
                        int partNumber = restUtils.convertPartNumber(
                                request.getParameter(RestParamDefine.PARTNUMBER));
                        if (partNumber < RestParamDefine.PART_NUMBER_MIN
                                || partNumber > RestParamDefine.PART_NUMBER_MAX) {
                            throw new S3ServerException(S3Error.PART_INVALID_PARTNUMBER,
                                    "invalid partNumber:" + partNumber);
                        }
                        request.setAttribute(RestParamDefine.Attribute.S3_PARTNUMBER, partNumber);
                    }
                    else { // put object
                        if (objectUri.getObjectName().length() > RestParamDefine.KEY_LENGTH) {
                            throw new S3ServerException(S3Error.OBJECT_KEY_TOO_LONG,
                                    "ObjectName is too long. objectName:"
                                            + objectUri.getObjectName());
                        }
                        Map<String, String> requestHeaders = new HashMap<>();
                        Map<String, String> xMeta = new HashMap<>();
                        restUtils.getHeaders(request, requestHeaders, xMeta);
                        if (restUtils.getXMetaLength(xMeta) > RestParamDefine.X_AMZ_META_LENGTH) {
                            throw new S3ServerException(S3Error.OBJECT_METADATA_TOO_LARGE,
                                    "metadata headers exceed the maximum. xMeta:"
                                            + xMeta.toString());
                        }
                        request.setAttribute(RestParamDefine.Attribute.S3_HEADERS, requestHeaders);
                        request.setAttribute(RestParamDefine.Attribute.S3_XMETA, xMeta);
                    }
                    request.setAttribute(RestParamDefine.Attribute.S3_OBJECTURI, objectUri);
                    request.setAttribute(RestParamDefine.Attribute.S3_BUCKET, scmbucket);
                }
                else if (request.getParameter(RestParamDefine.UPLOADID) != null
                        || request.getParameter(RestParamDefine.PARTNUMBER) != null) {
                    throw new S3ServerException(S3Error.NEED_A_KEY, "need a key");
                }
            }
            catch (S3ServerException e) {
                handleS3Error(request, response, e);
                return;
            }
        }
        invokeNext(request, response);
    }

    private ScmBucket getBucket(ScmUser user, String name) throws S3ServerException {
        try {
            return scmBucketService.getBucket(user, name);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST, "bucket not exist:" + name,
                        e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED, "", e);
            }
            throw new S3ServerException(S3Error.BUCKET_GET_FAILED,
                    "failed to put object: bucket=" + name, e);
        }
    }
}
