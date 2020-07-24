package com.sequoiacm.infrastructure.tool.common;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

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
