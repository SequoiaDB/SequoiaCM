package com.sequoiacm.config.server.remote;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmServiceUpdateConfigResult;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@RequestMapping("/internal/v1")
public interface ScmConfClient {

    @PostMapping("/notify/{businessType}")
    public void notifyInstance(@PathVariable("businessType") String businessType,
            @RequestParam(ScmRestArgDefine.EVENT_TYPE) String eventType,
            @RequestParam(ScmRestArgDefine.OPTION) BSONObject option,
            @RequestParam(ScmRestArgDefine.IS_ASYNC_NOTIFY) boolean isAsyncNotify)
                    throws ScmConfigException;

    @PutMapping("/config-props")
    public ScmServiceUpdateConfigResult updateConfigProps(
            @RequestParam(ScmRestArgDefine.CONF_PROPS_UPDATE_PROPERTIES) String updatePropsStr,
            @RequestParam(ScmRestArgDefine.CONF_PROPS_DELETE_PROPERTIES) String deletePropsStr,
            @RequestParam(ScmRestArgDefine.CONF_PROPS_ACCEPT_UNKNOWN_PROPS) boolean acceptUnknownProps)
                    throws ScmConfigException;

}

