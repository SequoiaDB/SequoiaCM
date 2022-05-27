package com.sequoiacm.cloud.gateway.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UploadForwardService {
    public void forward(String targetService, String targetApi, HttpServletRequest clientReq,
            HttpServletResponse clientResp, String defaultContentType, boolean chunked) throws Exception;
}
