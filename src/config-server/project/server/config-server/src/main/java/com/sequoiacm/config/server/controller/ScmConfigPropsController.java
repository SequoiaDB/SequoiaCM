package com.sequoiacm.config.server.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.config.server.module.ScmConfProp;
import com.sequoiacm.infrastructure.common.KeepAlive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.config.server.common.ScmTargetType;
import com.sequoiacm.config.server.module.ScmConfPropsParam;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResultSet;
import com.sequoiacm.config.server.service.ScmConfPropsService;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @KeepAlive
    @GetMapping("/config-props")
    public List<ScmConfProp> getConfigProps(
            // targetType 表示查询配置的范围，可选值 ALL、SERVICE、INSTANCE
            @RequestParam(CommonDefine.RestArg.CONFIG_PROPS_TARGET_TYPE) String targetType,

            // targets 表示查询配置的具体对象，当 targetType 为 ALL 时，targets 为空、当 targetType 为 SERVICE
            // 时，targets 为服务名、当 targetType 为 INSTANCE 时，targets 为节点地址，格式为：主机名:端口号
            @RequestParam(value = CommonDefine.RestArg.CONFIG_PROPS_TARGETS, required = false) String targetsStr,
            @RequestParam(CommonDefine.RestArg.CONFIG_PROPS_KEYS) String keys)
            throws ScmConfigException {
        List<String> targetList = new ArrayList<>();
        if (targetsStr != null && !targetsStr.isEmpty()) {
            targetList = Arrays.asList(targetsStr.split(","));
        }
        ScmTargetType scmTargetType = ScmTargetType.valueOf(targetType);
        if (scmTargetType == ScmTargetType.ALL) {
            if (targetList.size() > 0) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG,
                        String.format("can not specified targets when %s is %s",
                                CommonDefine.RestArg.CONFIG_PROPS_TARGET_TYPE, ScmTargetType.ALL));
            }
        }
        else if (targetList.size() == 0) {
            throw new ScmConfigException(ScmConfError.INVALID_ARG,
                    "missing required argument:" + CommonDefine.RestArg.CONFIG_PROPS_TARGETS);
        }
        if (keys == null || keys.isEmpty()) {
            throw new ScmConfigException(ScmConfError.INVALID_ARG,
                    "missing required argument:" + CommonDefine.RestArg.CONFIG_PROPS_KEYS);
        }
        return service.getConfProps(scmTargetType, targetList, Arrays.asList(keys.split(",")));
    }

}
