package com.sequoiacm.s3.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.CreateBucketConfiguration;
import com.sequoiacm.s3.model.GetServiceResult;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.service.BucketService;

@RestController
public class BucketController {
    private static final Logger logger = LoggerFactory.getLogger(BucketController.class);

    @Autowired
    BucketService bucketService;

    @Autowired
    ScmClientFactory clientFactory;

    @PutMapping(value = "/{bucketname:.+}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> putBucket(@PathVariable("bucketname") String bucketName,
            @RequestBody(required = false) CreateBucketConfiguration location, ScmSession session,
            HttpServletRequest httpServletRequest) throws S3ServerException {
        if (httpServletRequest.getParameterNames().hasMoreElements()) {
            throw new S3ServerException(S3Error.PARAMETER_NOT_SUPPORT,
                    "parameter " + httpServletRequest.getParameterNames().nextElement()
                            + " is not supported for bucket.");
        }

        if (!isValidBucketName(bucketName)) {
            throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                    "Invalid bucket name. bucket name = " + bucketName);
        }
        if (location == null) {
            location = new CreateBucketConfiguration();
        }

        logger.debug("Create bucket. bucketName ={}, operator={}, location={} ", bucketName,
                session.getUser().getUsername(), location.getLocationConstraint());
        bucketService.createBucket(session, bucketName, location.getLocationConstraint());
        return ResponseEntity.ok()
                .header(RestParamDefine.LOCATION, RestParamDefine.REST_DELIMITER + bucketName)
                .build();
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_XML_VALUE)
    public GetServiceResult listBuckets(ScmSession session) throws S3ServerException {
        logger.debug("list buckets. operator={}", session.getUser().getUsername());
        return bucketService.getService(session);
    }

    @DeleteMapping(value = "/{bucketname:.+}", produces = MediaType.APPLICATION_XML_VALUE)
    public void deleteBucket(@PathVariable("bucketname") String bucketName,
            HttpServletRequest httpServletRequest, ScmSession session) throws S3ServerException {
        if (httpServletRequest.getParameterNames().hasMoreElements()) {
            throw new S3ServerException(S3Error.PARAMETER_NOT_SUPPORT,
                    "parameter " + httpServletRequest.getParameterNames().nextElement()
                            + " is not supported for bucket.");
        }
        logger.debug("delete bucket. bucketName={}, operator={}", bucketName,
                session.getUser().getUsername());
        bucketService.deleteBucket(session, bucketName);
    }

    @RequestMapping(method = RequestMethod.HEAD, value = "/{bucketName:.+}")
    public ResponseEntity<?> headBucket(@PathVariable("bucketName") String bucketName,
            ScmSession session) throws S3ServerException {
        logger.debug("head bucket. bucketName={}, operator={}", bucketName,
                session.getUser().getUsername());
        Bucket bucket = bucketService.getBucket(session, bucketName);
        HttpHeaders headers = new HttpHeaders();
        if (bucket.getRegion() != null) {
            headers.add(RestParamDefine.HeadBucketResultHeader.REGION, bucket.getRegion());
        }
        return ResponseEntity.ok().headers(headers).build();
    }

    @RequestMapping(method = RequestMethod.HEAD, value = "")
    public ResponseEntity<?> headNone() throws S3ServerException {
        // restUtils.getOperatorByAuthorization(authorization);
        logger.error("Method not allowed. head none bucket.");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @RequestMapping(value = "/{bucketname:.+}", params = RestParamDefine.UPLOADID, produces = MediaType.APPLICATION_XML_VALUE)
    public void bucketRejectUploadId(ScmSession session) throws S3ServerException {
        throw new S3ServerException(S3Error.NEED_A_KEY, "need a key");
        // TODO
        // catch (Exception e) {
        // try {
        // if (httpServletRequest.getInputStream() != null) {
        // httpServletRequest.getInputStream().skip(httpServletRequest.getContentLength());
        // httpServletRequest.getInputStream().close();
        // }
        // }
        // catch (Exception e2) {
        // logger.error("skip content length fail");
        // }
        // throw e;
        // }
    }

    @RequestMapping(value = "/{bucketname:.+}", headers = RestParamDefine.CopyObjectHeader.X_AMZ_COPY_SOURCE, produces = MediaType.APPLICATION_XML_VALUE)
    public void bucketRejectCopy(ScmSession session) throws S3ServerException {
        throw new S3ServerException(S3Error.OBJECT_COPY_INVALID_DEST,
                "You can only specify a copy source header for copy requests.");
    }

    private Boolean isValidBucketName(String bucketName) {
        if (bucketName.length() < 3 || bucketName.length() > 63) {
            return false;
        }
        return ScmArgChecker.Directory.checkDirectoryName(bucketName);
    }
}
