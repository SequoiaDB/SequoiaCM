package com.sequoiacm.s3.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.service.RegionService;

@RestController
@RequestMapping(RestParamDefine.REST_REGION)
public class RegionController {
    private static final Logger logger = LoggerFactory.getLogger(RegionController.class);

    @Autowired
    RegionService regionService;

    @PutMapping(params = RestParamDefine.RegionPara.SET_DEFAULT_REGION)
    public ResponseEntity setDefault(
            @RequestParam(RestParamDefine.RegionPara.WORKSPACE_NAME) String ws, ScmSession session)
            throws ScmServerException {
        regionService.setDefaultRegion(ws);
        return ResponseEntity.ok().header(RestParamDefine.RegionPara.HEADER_DEFAULT_REGION, ws)
                .build();
    }

    @GetMapping(params = RestParamDefine.RegionPara.GET_DEFAULT_REGION, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDefaultRegion(ScmSession session) throws ScmServerException {
        String defaultRegion = regionService.getDefaultRegionForScm();
        return ResponseEntity.ok()
                .header(RestParamDefine.RegionPara.HEADER_DEFAULT_REGION, defaultRegion).build();
    }

}
