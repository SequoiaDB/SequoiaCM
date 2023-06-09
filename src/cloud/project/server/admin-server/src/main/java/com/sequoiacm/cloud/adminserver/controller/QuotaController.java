package com.sequoiacm.cloud.adminserver.controller;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.QuotaResult;
import com.sequoiacm.cloud.adminserver.service.QuotaService;
import com.sequoiacm.cloud.adminserver.service.QuotaSyncService;
import com.sequoiacm.common.CommonDefine;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QuotaController {

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private QuotaSyncService quotaSyncService;

    @PutMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_ENABLE_QUOTA)
    public QuotaResult enableQuota(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_MAX_OBJECTS, required = false, defaultValue = "-1") long maxObjects,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_MAX_SIZE, required = false, defaultValue = "-1") long maxSize,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_USED_OBJECTS, required = false) Long usedObjects,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_USED_SIZE, required = false) Long usedSize,
            Authentication auth) throws StatisticsException {
        return quotaService.enableQuota(type, name, maxObjects, maxSize, usedObjects, usedSize,
                auth);
    }

    @PutMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_UPDATE_QUOTA)
    public QuotaResult updateQuota(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_MAX_OBJECTS, required = false, defaultValue = "-1") long maxObjects,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_MAX_SIZE, required = false, defaultValue = "-1") long maxSize,
            Authentication auth) throws StatisticsException {
        return quotaService.updateQuota(type, name, maxObjects, maxSize, auth);
    }

    @PutMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_DISABLE_QUOTA)
    public void disableQuota(@PathVariable("type") String type, @PathVariable("name") String name,
            Authentication auth) throws StatisticsException {
        quotaService.disableQuota(type, name, auth);
    }

    @PostMapping(path = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_SYNC)
    public void quotaSync(@PathVariable("type") String type, @PathVariable("name") String name,
            Authentication auth) throws StatisticsException {
        quotaSyncService.sync(type, name, false, auth);
    }

    @PostMapping(path = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_CANCEL_SYNC)
    public void cancelQuotaSync(@PathVariable("type") String type,
            @PathVariable("name") String name, Authentication auth) throws StatisticsException {
        quotaSyncService.cancelSync(type, name, auth);
    }

    @GetMapping("/quotas/{type}/{name}")
    public QuotaResult getQuota(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_FORCE_REFRESH, defaultValue = "false", required = false) boolean isForceRefresh,
            Authentication auth) throws StatisticsException {
        return quotaService.getQuota(type, name, isForceRefresh, auth);
    }

    @PutMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_UPDATE_QUOTA_USED_INFO)
    public QuotaResult updateQuotaUsedInfo(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_USED_OBJECTS, required = false) Long usedObjects,
            @RequestParam(value = CommonDefine.RestArg.QUOTA_USED_SIZE, required = false) Long usedSize,
            Authentication auth) throws StatisticsException {
        return quotaService.updateQuotaUsedInfo(type, name, usedObjects, usedSize, auth);
    }

    @GetMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_GET_QUOTA_INNER_DETAIL)
    public BSONObject getInnerDetail(@PathVariable("type") String type,
            @PathVariable("name") String name, Authentication auth) throws StatisticsException {
        return quotaSyncService.getInnerDetail(type, name, auth);
    }

}
