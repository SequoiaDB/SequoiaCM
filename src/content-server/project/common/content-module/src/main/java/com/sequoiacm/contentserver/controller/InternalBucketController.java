package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.model.ObjectDeltaInfo;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalBucketController {

    private static final Logger logger = LoggerFactory.getLogger(InternalBucketController.class);

    @Autowired
    private IScmBucketService bucketService;

    @GetMapping(value = "/buckets/{bucketName}", params = "action="
            + CommonDefine.RestArg.ACTION_GET_OBJECT_DELTA)
    public ObjectDeltaInfo getObjectDelta(@PathVariable("bucketName") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition)
            throws ScmServerException {
        logger.info("internal get object delta: bucketName={},filter={}", bucketName, condition);
        return bucketService.getObjectDelta(bucketName, condition);
    }
}
