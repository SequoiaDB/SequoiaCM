package com.sequoiacm.infrastructure.config.client.controller;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.infrastructure.config.client.service.ScmConfigNotifyService;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@RestController()
@RequestMapping("/internal/v1")
public class ScmConfigNotifyController {

    @Autowired
    ScmConfigNotifyService notifyService;

    @RequestMapping("/notify/{config_name}")
    public void nofify(@PathVariable("config_name") String configName,
            @RequestParam(ScmRestArgDefine.EVENT_TYPE) String eventType,
            @RequestParam(ScmRestArgDefine.OPTION) String notifyOption,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject notifyOptionObj = (BSONObject) JSON.parse(notifyOption);
        EventType type = EventType.valueOf(eventType);
        Assert.notNull(type, "unknown event type:" + eventType);
        notifyService.notify(configName, type, notifyOptionObj, isAsyncNotify);
    }
}
