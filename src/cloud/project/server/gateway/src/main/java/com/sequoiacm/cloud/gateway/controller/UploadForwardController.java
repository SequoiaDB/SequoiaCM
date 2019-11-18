package com.sequoiacm.cloud.gateway.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
            HttpServletRequest clientReq, HttpServletResponse clientResp) throws IOException {
        long before = System.currentTimeMillis();
        try {
            String uri = clientReq.getRequestURI();
            String targetApi = uri.substring(("/zuul/" + targetService).length());
            logger.debug("forward file upload:targetService={},targetApi={}", targetService,
                    targetApi);
            service.forward(targetService, targetApi, clientReq, clientResp);
        }
        finally {
            ReqRecorder.getInstance().addRecord(System.currentTimeMillis() - before);
        }
    }

}
