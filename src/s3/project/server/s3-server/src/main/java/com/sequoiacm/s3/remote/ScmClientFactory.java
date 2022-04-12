package com.sequoiacm.s3.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.s3.authoriztion.ScmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScmClientFactory {
    private static final ObjectMapper SCM_OBJMAPPER = new ObjectMapper();
    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(AccesskeyInfo.class, new AccesskeyInfoDeserializer());
        SCM_OBJMAPPER.registerModule(module);
    }

    @Autowired
    private ScmFeignClient feign;


    private volatile AuthServerService authService;



    public AuthServerClient getAuthServerClient(ScmSession session) {
        if (authService == null) {
            authService = feign.builder().objectMapper(SCM_OBJMAPPER)
                    .serviceTarget(AuthServerService.class, "auth-server");
        }
        return new AuthServerClient(session, authService);
    }

}

