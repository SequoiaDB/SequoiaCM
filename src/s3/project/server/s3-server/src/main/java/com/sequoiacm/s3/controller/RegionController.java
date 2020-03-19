package com.sequoiacm.s3.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.service.RegionService;

@RestController
@RequestMapping(RestParamDefine.REST_REGION)
public class RegionController {
    private static final Logger logger = LoggerFactory.getLogger(RegionController.class);

    @Autowired
    RegionService regionService;

    @PostMapping(params = RestParamDefine.RegionPara.INIT_REGION, produces = MediaType.APPLICATION_XML_VALUE)
    public void initRegion(@RequestParam(RestParamDefine.RegionPara.REGION_NAME) String regionName,
            @RequestParam(RestParamDefine.USERNAME) String userName,
            @RequestParam(RestParamDefine.PASSWORD) String encryptPassword)
            throws S3ServerException {
        logger.info("put region. regionName:{}", regionName.toLowerCase());
        regionService.initWorkspaceS3Meta(userName, encryptPassword, regionName);
    }
    
    

}
