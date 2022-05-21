package com.sequoiacm.s3.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.common.VersioningStatusType;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

@S3Controller
public class BucketVersioningController {

    @PutMapping(value = "/{bucketname:.+}", params = RestParamDefine.VERSIONING, produces = MediaType.APPLICATION_XML_VALUE)
    public void putBucketVersioning(@PathVariable("bucketname") String bucketName,
            ScmSession session) throws S3ServerException {
        throw new S3ServerException(S3Error.OPERATION_NOT_SUPPORTED,
                "unsupport put bucket version");
    }

    @GetMapping(value = "/{bucketname:.+}", params = RestParamDefine.VERSIONING, produces = MediaType.APPLICATION_XML_VALUE)
    public String getBucketVersioning(@PathVariable("bucketname") String bucketName,
            ScmSession session) throws S3ServerException {
        return VersioningStatusType.NONE.getName();
    }

}
