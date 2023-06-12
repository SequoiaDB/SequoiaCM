package com.sequoiacm.infrastructure.config.client.remote;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

import feign.Response;

import java.util.Map;

@RequestMapping("/internal/v1")
public interface ScmConfFeignClient {

    @PostMapping("/subscribe/{businessType}")
    public void subscribe(@PathVariable("businessType") String configName,
            @RequestParam(ScmRestArgDefine.SERVICE_NAME) String serviceName)
                    throws ScmConfigException;

    @DeleteMapping("/subscribe/{businessType}")
    public void unsubscribe(@PathVariable("businessType") String businessType,
            @RequestParam(ScmRestArgDefine.SERVICE_NAME) String serviceName)
                    throws ScmConfigException;

    @PostMapping("/config/{businessType}")
    public BSONObject createConfV1(@PathVariable("businessType") String confName,
            @RequestParam(ScmRestArgDefine.CONFIG) BSONObject config,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @PostMapping(value = "/config/{businessType}")
    public BSONObject createConfV2(@PathVariable("businessType") String confName,
            @RequestBody BSONObject configBody,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @DeleteMapping("/config/{businessType}")
    public BSONObject deleteConf(@PathVariable("businessType") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @GetMapping("/config/{businessType}")
    public BSONObject getConf(@PathVariable("businessType") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;

    @GetMapping("/config/{businessType}?action=list_conf")
    public Response listConf(@PathVariable("businessType") String confName,
                             @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;

    @GetMapping("/config/{businessType}?action=count_conf")
    public Response countConf(@PathVariable("businessType") String confName,
                             @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;


    @PutMapping("/config/{businessType}")
    public BSONObject updateConf(@PathVariable("businessType") String confName,
            @RequestParam(ScmRestArgDefine.CONFIG) BSONObject config,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @GetMapping("/config/{businessType}/version")
    public BSONObject getConfVersion(@PathVariable("businessType") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;

    @GetMapping("/globalConfig")
    public Map<String, String> getGlobalConf(
            @RequestParam(ScmRestArgDefine.GLOBAL_CONF_NAME) String confName)
            throws ScmConfigException;
}
