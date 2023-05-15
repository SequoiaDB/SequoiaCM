package com.sequoiacm.config.server.controller;

import com.sequoiacm.infrastructure.common.KeepAlive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.config.server.common.ScmTargetType;
import com.sequoiacm.config.server.module.ScmConfPropsParam;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResultSet;
import com.sequoiacm.config.server.service.ScmConfPropsService;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;


@RestController
@RequestMapping("/api/v1")
public class ScmConfigPropsController {
    @Autowired
    private ScmConfPropsService service;


    @KeepAlive
    @PutMapping("/config-props")
    public ScmUpdateConfPropsResultSet updateConfigProps(@RequestBody ScmConfPropsParam config)
            throws Exception {
        Assert.notNull(config.getTargetType(), "missing required argument:target_type");
        Assert.isTrue(config.getUpdateProperties() != null && config.getDeleteProperties() != null,
                "missing required argument:update_properties or delete_properties");

        if (config.getTargetType() == ScmTargetType.ALL) {
            if (config.getTargets() != null && config.getTargets().size() > 0) {
                throw new IllegalArgumentException(
                        "can not specified targets when target_type is ALL");
            }
        }
        else if (config.getTargets() == null || config.getTargets().size() == 0) {
            throw new IllegalArgumentException("missing required argument:targets");
        }
        try {
            return service.updateConfProps(config.getTargetType(), config.getTargets(),
                    config.getUpdateProperties(), config.getDeleteProperties(),
                    config.isAcceptUnrecognizedProp());
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.INVALID_ARG) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            throw new Exception(e.getMessage(), e);
        }
    }
}
