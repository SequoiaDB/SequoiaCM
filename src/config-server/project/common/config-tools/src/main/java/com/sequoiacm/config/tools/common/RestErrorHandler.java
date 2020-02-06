package com.sequoiacm.config.tools.common;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class RestErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        try {
            super.handleError(response);
        }
        catch (RestClientResponseException e) {
            String resp = e.getResponseBodyAsString();
            throw new RestClientException(resp, e);
        }
    }

}
