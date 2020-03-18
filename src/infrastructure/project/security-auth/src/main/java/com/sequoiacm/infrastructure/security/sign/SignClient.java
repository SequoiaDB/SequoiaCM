package com.sequoiacm.infrastructure.security.sign;

import java.util.Collection;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.security.auth.RestField;

import feign.Response;

@Component
public class SignClient {

    private SignService signService;

    @Autowired
    public SignClient(ScmFeignClient feignClient) {
        signService = feignClient.builder().serviceTarget(SignService.class, "auth-server");
    }

    public String loginWithSignature(SignatureInfo signature) throws ScmFeignException {
        Response resp = signService.login(signature.toBSON());
        if (resp.status() >= 200 && resp.status() < 300) {
            Collection<String> sessionIdCl = resp.headers().get(RestField.SESSION_ATTRIBUTE);
            if (sessionIdCl == null || sessionIdCl.size() <= 0) {
                throw new RuntimeException("failed to decode login resp, missing header:"
                        + RestField.SESSION_ATTRIBUTE);
            }

            return sessionIdCl.iterator().next();
        }

        Exception ex = new ScmFeignErrorDecoder().decode("login with signature", resp);
        if (ex instanceof ScmFeignException) {
            throw (ScmFeignException) ex;
        }
        else {
            throw new RuntimeException(ex.getMessage(), ex);
        }

    }

    public String getSecretKey(String accesskey) throws ScmFeignException {
        BSONObject bson = signService.getSecretkey(accesskey);
        return new AccesskeyInfo(bson).getSecretkey();
    }
}
