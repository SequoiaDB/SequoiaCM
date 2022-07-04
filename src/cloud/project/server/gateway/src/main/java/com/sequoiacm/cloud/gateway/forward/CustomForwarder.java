package com.sequoiacm.cloud.gateway.forward;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface CustomForwarder {

    void forward(String targetService, String targetApi, HttpServletRequest clientReq,
                 HttpServletResponse clientResp, String defaultContentType, boolean chunked,
                 boolean setForwardPrefix) throws Exception;
}
