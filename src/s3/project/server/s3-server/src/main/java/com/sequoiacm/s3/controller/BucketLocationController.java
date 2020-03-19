package com.sequoiacm.s3.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.LocationConstraint;
import com.sequoiacm.s3.service.BucketService;

@RestController
public class BucketLocationController {
    private static final Logger logger = LoggerFactory.getLogger(BucketLocationController.class);

    @Autowired
    BucketService bucketService;

    @GetMapping(value = "/{bucketname:.+}", params = RestParamDefine.LOCATION, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<LocationConstraint> getBucketLocation(@PathVariable("bucketname") String bucketName,
            ScmSession session,
            @RequestHeader(value = RestParamDefine.AUTHORIZATION, required = false) String authorization)
            throws S3ServerException {
        try {
            logger.debug("get bucket location. bucket={}, operator={}", bucketName,
                    session.getUser().getUsername());

            return ResponseEntity.ok().body(bucketService.getBucketLocation(session, bucketName));
        }
        catch (Exception e) {
            logger.error("get bucket location failed. bucketName={}", bucketName);
            throw e;
        }
    }
}
