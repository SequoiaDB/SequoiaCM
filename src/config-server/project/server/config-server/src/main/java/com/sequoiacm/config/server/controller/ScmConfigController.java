package com.sequoiacm.config.server.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.server.service.ScmConfService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.Version;

@RestController
@RequestMapping("/internal/v1")
public class ScmConfigController {
    @Autowired
    ScmConfService service;

    @PostMapping("/config/{conf_name}")
    public Config createConf(@PathVariable("conf_name") String confName,
            @RequestParam(value = ScmRestArgDefine.CONFIG, required = false) BSONObject configParam,
            @RequestBody(required = false) BSONObject configBody,
            @RequestParam(value = ScmRestArgDefine.IS_ASYNC_NOTIFY, defaultValue = "true") boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject config = configBody == null ? configParam : configBody;
        return service.createConf(confName, config, isAsyncNotify);
    }

    @DeleteMapping("/config/{conf_name}")
    public Config deleteConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject config,
            @RequestParam(value = ScmRestArgDefine.IS_ASYNC_NOTIFY, defaultValue = "true") boolean isAsyncNotify)
            throws ScmConfigException {
        return service.deleteConf(confName, config, isAsyncNotify);
    }

    @PutMapping("/config/{conf_name}")
    public Config updateConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.CONFIG) BSONObject config,
            @RequestParam(value = ScmRestArgDefine.IS_ASYNC_NOTIFY, defaultValue = "true") boolean isAsyncNotify)
            throws ScmConfigException {
        return service.updateConf(confName, config, isAsyncNotify);
    }

    @GetMapping("/config/{conf_name}")
    public List<Config> getConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject confFilter)
            throws ScmConfigException {
        return service.getConf(confName, confFilter);
    }

    @GetMapping(path = "/config/{conf_name}", params = "action=count_conf")
    public ResponseEntity countConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject confFilter)
            throws ScmConfigException {
        long count = service.countConf(confName, confFilter);
        return ResponseEntity.ok().header(ScmRestArgDefine.COUNT_HEADER, count + "").build();
    }

    @GetMapping(path = "/config/{conf_name}", params = "action=list_conf")
    public void listConf(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject confFilter, HttpServletResponse resp)
            throws ScmConfigException, IOException {
        resp.setHeader("Content-Type", "application/json;charset=utf-8");
        PrintWriter writer = resp.getWriter();
        putCursorToWriter(service.listConf(confName, confFilter), writer);
    }

    @GetMapping("/config/{conf_name}/version")
    public List<Version> getConfVersion(@PathVariable("conf_name") String confName,
            @RequestParam(ScmRestArgDefine.FILTER) BSONObject versionFilter)
            throws ScmConfigException {
        return service.getConfVersion(confName, versionFilter);
    }

    @PostMapping("/subscribe/{config_name}")
    public void subscribe(@PathVariable("config_name") String configName,
            @RequestParam(ScmRestArgDefine.SERVICE_NAME) String serviceName)
            throws ScmConfigException {
        service.subscribe(configName, serviceName);
    }

    @DeleteMapping("/subscribe/{config_name}")
    public void unsubscribe(@PathVariable("config_name") String configName,
            @RequestParam(ScmRestArgDefine.SERVICE_NAME) String serviceName)
            throws ScmConfigException {
        service.unsubscribe(configName, serviceName);
    }

    @GetMapping("/subscribe")
    public List<ScmConfSubscriber> listSubscribers() throws ScmConfigException {
        return service.listSubsribers();
    }

    public static void putCursorToWriter(MetaCursor cursor, PrintWriter writer)
            throws ScmConfigException {
        int count = 0;
        try {
            writer.write("[");
            if (cursor.hasNext()) {
                while (true) {
                    writer.write(cursor.getNext().toString());
                    if (cursor.hasNext()) {
                        writer.write(",");
                    }
                    else {
                        break;
                    }
                    if (count++ == 2000) {
                        if (writer.checkError()) {
                            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                                    "failed to write response to client because of ioexception");
                        }
                        count = 0;
                    }
                }
            }
            writer.write("]");
            if (writer.checkError()) {
                throw new ScmServerException(ScmError.NETWORK_IO,
                        "failed to write response to client because of ioexception");
            }
        }
        catch (Exception e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR, "traverse cursor failed", e);
        }
        finally {
            cursor.close();
            writer.flush();
        }
    }
}
