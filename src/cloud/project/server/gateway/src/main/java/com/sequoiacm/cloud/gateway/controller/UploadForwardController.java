package com.sequoiacm.cloud.gateway.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sequoiacm.cloud.gateway.service.UploadForwardService;
import com.sequoiacm.infrastructure.monitor.ReqRecorder;

@RestController
public class UploadForwardController {
    private static final Logger logger = LoggerFactory.getLogger(UploadForwardController.class);
    @Autowired
    UploadForwardService service;

    @RequestMapping(value = "/zuul/{target_service}/**", method = { RequestMethod.POST,
            RequestMethod.PUT })
    public void forwardUpload(@PathVariable("target_service") String targetService,
            HttpServletRequest clientReq, HttpServletResponse clientResp) throws Exception {
        long before = System.currentTimeMillis();
        try {
            String uri = clientReq.getRequestURI();
            String targetApi = uri.substring(("/zuul/" + targetService).length());
            logger.debug("forward file upload:targetService={},targetApi={}", targetService,
                    targetApi);
            service.forward(targetService, targetApi, clientReq, clientResp, true);
        }
        finally {
            ReqRecorder.getInstance().addRecord(System.currentTimeMillis() - before);
        }
    }

    @RequestMapping(value = "/s3/**", method = { RequestMethod.POST, RequestMethod.GET,
            RequestMethod.HEAD, RequestMethod.DELETE, RequestMethod.PUT })
    public void forwardS3Upload(HttpServletRequest clientReq, HttpServletResponse clientResp)
            throws Exception {
        long before = System.currentTimeMillis();
        try {
            String uri = clientReq.getRequestURI();
            String targetApi = uri.substring(("/s3").length());
            logger.debug("forward s3 req:targetApi={}", targetApi);
            service.forward("s3", targetApi, clientReq, clientResp, false);
        }
        finally {
            ReqRecorder.getInstance().addRecord(System.currentTimeMillis() - before);
        }
    }

}
