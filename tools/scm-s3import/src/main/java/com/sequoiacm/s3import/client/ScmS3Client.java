package com.sequoiacm.s3import.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.net.MalformedURLException;

import com.sequoiacm.s3import.client.auth.SignerBase;
import com.sequoiacm.s3import.client.auth.SignerForAuthorizationV4;
import com.sequoiacm.s3import.client.exception.S3Error;
import com.sequoiacm.s3import.client.exception.ScmS3ClientException;
import com.sequoiacm.s3import.client.exception.ScmS3ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.*;

import static com.sequoiacm.s3import.common.CommonDefine.S3Client.*;

public class ScmS3Client implements ScmS3 {
    private static final Logger logger = LoggerFactory.getLogger(ScmS3Client.class);
    private static String contentHashStringNull = SignerBase.EMPTY_BODY_SHA256;

    private String accessKeyId;
    private String secretKeyId;
    private String endpoint;
    private RestTemplate rest;
    private HttpComponentsClientHttpRequestFactory factory;

    ScmS3Client(ScmS3ClientBuilder builder) {
        this.accessKeyId = builder.getAccessKeyId();
        this.secretKeyId = builder.getSecretKeyId();
        this.endpoint = builder.getEndpoint();
        this.factory = new HttpComponentsClientHttpRequestFactory();
        this.factory.setConnectionRequestTimeout(builder.getConnectionRequestTimeout());
        this.factory.setConnectTimeout(builder.getConnectTimeout());
        this.factory.setBufferRequestBody(builder.isRequestBody());
        this.factory.setReadTimeout(builder.getReadTimeout());
        rest = new RestTemplate(this.factory);
    }

    @Override
    public void deleteObject(String bucketName, String key, Map<String, Object> deleteConf)
            throws ScmS3ClientException {
        rejectNull(bucketName, "The bucket name must be specified when deleting an object");
        rejectNull(key, "The key must be specified when deleting an object");
        try {
            // url = http://ip:port/bucketname/object
            String url = endpoint + "/" + bucketName + "/" + key;
            URL endpointUrl = new URL(url);

            Map<String, String> headers = new HashMap<>();
            headers.put(X_AMZ_CONTENT_SHA256, contentHashStringNull);
            Map<String, String> parameters = new HashMap<>();
            SignerForAuthorizationV4 signer = new SignerForAuthorizationV4(endpointUrl,
                    HttpMethod.DELETE.name(), SERVICE_NAME, DEFAULT_REGION);
            String authorization = signer.computeSignature(headers, parameters,
                    contentHashStringNull, accessKeyId, secretKeyId);

            headers.put(AUTHORIZATION, authorization);
            if (deleteConf != null) {
                for (String k : deleteConf.keySet()) {
                    Object v = deleteConf.get(k);
                    headers.put(k, String.valueOf(v));
                }
            }

            exec(headers, null, url, HttpMethod.DELETE, String.class);
        }
        catch (HttpStatusCodeException e) {
            throw httpToException(e);
        }
        catch (RestClientException e) {
            throw new ScmS3ClientException(e.getMessage(), e);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private void rejectNull(Object parameterValue, String errorMessage) {
        if (parameterValue == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    public void shutdown() {
        try {
            this.factory.destroy();
        }
        catch (Exception e) {
            logger.error("shutdown failed.", e);
        }
    }

    private ScmS3ServiceException httpToException(HttpStatusCodeException e) {
        ScmS3ServiceException s3Exception = new ScmS3ServiceException(e.getMessage());
        s3Exception.setStatusCode(e.getStatusCode().value());

        try {
            ObjectMapper objectMapper = new XmlMapper();
            String errorMessage = e.getResponseBodyAsString();
            S3Error result = objectMapper.readValue(errorMessage, S3Error.class);
            s3Exception.setErrorCode(result.getCode());
            s3Exception.setErrorMessage(result.getMessage());
        }
        catch (Exception e2) {
            s3Exception.setErrorCode(e.getStatusCode().toString());
            s3Exception.setErrorMessage(e.getResponseBodyAsString());
            logger.warn("Parse error message failed.", e2);
        }
        return s3Exception;
    }

    private <T> ResponseEntity<T> exec(Map headers, Object body, String url, HttpMethod method,
            Class<T> responseType) {
        MultiValueMap httpHeaders = new LinkedMultiValueMap();
        Set<String> headerKeySet = headers.keySet();
        for (String key : headerKeySet) {
            httpHeaders.set(key, headers.get(key));
        }

        HttpEntity<?> requestEntity;
        if (body != null) {
            requestEntity = new HttpEntity<>(body, httpHeaders);
        }
        else {
            requestEntity = new HttpEntity<>(httpHeaders);
        }

        ResponseEntity<T> response = rest.exchange(url, method, requestEntity, responseType);
        return response;
    }
}