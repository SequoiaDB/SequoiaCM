package com.sequoiacm.s3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sequoiacm.s3.model.VersioningConfiguration;
import com.sequoiacm.s3.model.VersioningConfigurationBase;
import com.sequoiacm.s3.service.BucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@S3Controller
public class BucketVersioningController {
    @Autowired
    private BucketService bucketService;

    @PutMapping(value = "/{bucketname:.+}", params = RestParamDefine.VERSIONING, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity putBucketVersioning(@PathVariable("bucketname") String bucketName,
            ScmSession session, HttpServletRequest servletRequest) throws S3ServerException {
        String status = getVersioningStatus(servletRequest);
        bucketService.setBucketVersionStatus(session, bucketName, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{bucketname:.+}", params = RestParamDefine.VERSIONING, produces = MediaType.APPLICATION_XML_VALUE)
    public VersioningConfigurationBase getBucketVersioning(
            @PathVariable("bucketname") String bucketName, ScmSession session)
            throws S3ServerException {
        return bucketService.getBucketVersionStatus(session, bucketName);
    }

    private String getVersioningStatus(HttpServletRequest httpServletRequest)
            throws S3ServerException {
        int ONCE_READ_BYTES = 1024;
        try {
            ServletInputStream inputStream = httpServletRequest.getInputStream();
            byte[] b = new byte[ONCE_READ_BYTES];
            StringBuilder stringBuilder = new StringBuilder();
            int len = inputStream.read(b, 0, ONCE_READ_BYTES);
            while (len > 0) {
                stringBuilder.append(new String(b, 0, len));
                len = inputStream.read(b, 0, ONCE_READ_BYTES);
            }

            String content = stringBuilder.toString();
            if (0 == content.length()) {
                throw new S3ServerException(S3Error.MALFORMED_XML, "no body");
            }

            ObjectMapper objectMapper = new XmlMapper();
            VersioningConfiguration versioningCfg = objectMapper.readValue(content,
                    VersioningConfiguration.class);

            return versioningCfg.getStatus();
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (IOException e) {
            throw new S3ServerException(S3Error.MALFORMED_XML, "parse versioning status failed", e);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.MALFORMED_XML, "get versioning status failed", e);
        }
    }
}
