package com.sequoiacm.cloud.adminserver.remote;

import java.util.Map;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.cloud.adminserver.common.RestCommonDefine;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;

public interface ContentServerClient {

    // ***************metrics***************//
    @GetMapping(value = "/metrics")
    public Map<String, Object> metrics() throws Exception;

    // ***************file****************//
    @RequestMapping(value = "/internal/v1/files", method = RequestMethod.HEAD)
    public FileDeltaInfo getFileDelta(
            @RequestParam(RestCommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = RestCommonDefine.RestArg.QUERY_FILTER, required = false) BSONObject condition,
            @RequestParam(value = RestCommonDefine.RestArg.FILE_LIST_SCOPE, required = false) int scope)
            throws Exception;

}
