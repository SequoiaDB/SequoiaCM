package com.sequoiacm.s3.controller;

import javax.servlet.ServletInputStream;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.CreateBucketConfiguration;
import com.sequoiacm.s3.model.ListBucketResult;
import com.sequoiacm.s3.model.LocationConstraint;
import com.sequoiacm.s3.service.BucketService;

@S3Controller
public class BucketController {
    private static final Logger logger = LoggerFactory.getLogger(BucketController.class);

    @Autowired
    BucketService bucketService;

    @PutMapping(value = "/{bucketname:.+}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> putBucket(@PathVariable("bucketname") String bucketName, ScmSession session,
            HttpServletRequest httpServletRequest) throws S3ServerException {
        if (httpServletRequest.getParameterNames().hasMoreElements()) {
            throw new S3ServerException(S3Error.PARAMETER_NOT_SUPPORT,
                    "parameter " + httpServletRequest.getParameterNames().nextElement()
                            + " is not supported for bucket.");
        }

        String location = getLocation(httpServletRequest);
        logger.debug("Create bucket. bucketName ={}, operator={}, location={} ", bucketName,
                session.getUser().getUsername(), location);
        bucketService.createBucket(session, bucketName, location);
        return ResponseEntity.ok()
                .header(RestParamDefine.LOCATION, RestParamDefine.REST_DELIMITER + bucketName)
                .build();
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_XML_VALUE)
    public ListBucketResult listBuckets(ScmSession session) throws S3ServerException {
        logger.debug("list buckets. operator={}", session.getUser().getUsername());
        return bucketService.listBucket(session);
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

    @RequestMapping(value = "/{bucketname:.+}", headers = RestParamDefine.CopyObjectHeader.X_AMZ_COPY_SOURCE, produces = MediaType.APPLICATION_XML_VALUE)
    public void bucketRejectCopy(ScmSession session) throws S3ServerException {
        throw new S3ServerException(S3Error.OBJECT_COPY_INVALID_DEST,
                "You can only specify a copy source header for copy requests.");
    }

    @GetMapping(value = "/{bucketname:.+}", params = RestParamDefine.LOCATION, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<LocationConstraint> getBucketLocation(@PathVariable("bucketname") String bucketName,
                                                                ScmSession session,
                                                                @RequestHeader(value = RestParamDefine.AUTHORIZATION, required = false) String authorization)
            throws S3ServerException {
        try {
            logger.debug("get bucket location. bucketName={}, operator={}", bucketName,
                    session.getUser().getUsername());
            return ResponseEntity.ok().body(bucketService.getBucketLocation(session, bucketName));
        }
        catch (Exception e) {
            logger.error("get bucket location failed. bucketName={}", bucketName);
            throw e;
        }
    }

    private String getLocation(HttpServletRequest httpServletRequest) throws S3ServerException {
        int ONCE_READ_BYTES = 1024;
        try {
            ServletInputStream inputStream = httpServletRequest.getInputStream();
            StringBuilder stringBuilder = new StringBuilder();
            byte[] b = new byte[ONCE_READ_BYTES];
            int len = inputStream.readLine(b, 0, ONCE_READ_BYTES);
            while (len > 0) {
                stringBuilder.append(new String(b, 0, len));
                len = inputStream.readLine(b, 0, ONCE_READ_BYTES);
            }
            String content = stringBuilder.toString();
            if (content.length() > 0) {
                ObjectMapper objectMapper = new XmlMapper();
                return objectMapper.readValue(content, CreateBucketConfiguration.class).getLocationConstraint();
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.MALFORMED_XML, "get location failed", e);
        }
    }
}
