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

    @PostMapping("/subscribe/{config_name}")
    public void subscribe(@PathVariable("config_name") String configName,
            @RequestParam(ScmRestArgDefine.SERVICE_NAME) String serviceName)
                    throws ScmConfigException;

    @DeleteMapping("/subscribe/{config_name}")
    public void unsubscribe(@PathVariable("config_name") String configName,
            @RequestParam(ScmRestArgDefine.SERVICE_NAME) String serviceName)
                    throws ScmConfigException;

    @PostMapping("/config/{conf_name}")
    public BSONObject createConfV1(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.CONFIG) BSONObject config,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @PostMapping(value = "/config/{conf_name}")
    public BSONObject createConfV2(@PathVariable("conf_name") String confName,
            @RequestBody BSONObject configBody,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @DeleteMapping("/config/{conf_name}")
    public BSONObject deleteConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @GetMapping("/config/{conf_name}")
    public BSONObject getConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;

    @GetMapping("/config/{conf_name}?action=list_conf")
    public Response listConf(@PathVariable("conf_name") String confName,
                             @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;

    @GetMapping("/config/{conf_name}?action=count_conf")
    public Response countConf(@PathVariable("conf_name") String confName,
                             @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;


    @PutMapping("/config/{conf_name}")
    public BSONObject updateConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.CONFIG) BSONObject config,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException;

    @GetMapping("/config/{conf_name}/version")
    public BSONObject getConfVersion(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject filter) throws ScmConfigException;

    @GetMapping("/globalConfig")
    public Map<String, String> getGlobalConf(
            @RequestParam(ScmRestArgDefine.GLOBAL_CONF_NAME) String confName)
            throws ScmConfigException;
}
