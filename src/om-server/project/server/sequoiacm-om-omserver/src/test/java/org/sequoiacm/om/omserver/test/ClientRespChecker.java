package org.sequoiacm.om.omserver.test;

import feign.Response;
import feign.codec.ErrorDecoder;

public class ClientRespChecker {
    private ErrorDecoder errorDecoder;

    public ClientRespChecker(ErrorDecoder errorDecoder) {
        this.errorDecoder = errorDecoder;
    }

    public void check(Response resp) throws Exception {
        if (resp.status() < 200 || resp.status() >= 300) {
            throw errorDecoder.decode("none", resp);
        }
    }
}
