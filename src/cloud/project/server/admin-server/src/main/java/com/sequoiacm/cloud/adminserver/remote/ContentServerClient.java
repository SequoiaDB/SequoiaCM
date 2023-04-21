package com.sequoiacm.cloud.adminserver.remote;

import java.util.Map;

import com.sequoiacm.cloud.adminserver.model.ObjectDeltaInfo;
import com.sequoiacm.common.CommonDefine;
import org.bson.BSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping(value = "/internal/v1/buckets/{bucketName}" + "?action="
            + CommonDefine.RestArg.ACTION_GET_OBJECT_DELTA)
    public ObjectDeltaInfo getObjectDelta(@PathVariable("bucketName") String bucketName,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER, required = false) BSONObject condition)
            throws Exception;

    @PostMapping(value = "/internal/v1/quotas/{type}/{name}" + "?action="
            + CommonDefine.RestArg.QUOTA_ACTION_STATISTICS)
    public void quotaStatistics(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber)
            throws Exception;

    @GetMapping(value = "/internal/v1/quotas/{type}/{name}" + "?action="
            + CommonDefine.RestArg.QUOTA_ACTION_GET_QUOTA_INNER_DETAIL)
    public BSONObject getQuotaInnerDetail(@PathVariable("type") String type,
            @PathVariable("name") String name) throws Exception;

}
