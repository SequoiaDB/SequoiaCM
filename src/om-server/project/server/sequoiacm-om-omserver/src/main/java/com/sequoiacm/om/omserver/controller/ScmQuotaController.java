package com.sequoiacm.om.omserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmBucketQuotaInfo;
import com.sequoiacm.om.omserver.service.ScmQuotaService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ScmQuotaController {

    @Autowired
    private ScmQuotaService quotaService;

    @PutMapping(value = "/quotas/bucket/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_ENABLE_QUOTA)
    public void enableBucketQuota(ScmOmSession session, @PathVariable("name") String name,
            @RequestParam(value = RestParamDefine.QUOTA_MAX_OBJECTS, required = false) Long maxObjects,
            @RequestParam(value = RestParamDefine.QUOTA_MAX_SIZE, required = false) String maxSize)
            throws ScmInternalException {
        quotaService.enableBucketQuota(session, name, maxObjects, maxSize);
    }

    @PutMapping(value = "/quotas/bucket/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_UPDATE_QUOTA)
    public void updateBucketQuota(ScmOmSession session, @PathVariable("name") String name,
            @RequestParam(value = RestParamDefine.QUOTA_MAX_OBJECTS, required = false) Long maxObjects,
            @RequestParam(value = RestParamDefine.QUOTA_MAX_SIZE, required = false) String maxSize)
            throws ScmInternalException {
        quotaService.updateBucketQuota(session, name, maxObjects, maxSize);
    }

    @PutMapping(value = "/quotas/bucket/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_DISABLE_QUOTA)
    public void disableBucketQuota(ScmOmSession session, @PathVariable("name") String name)
            throws ScmInternalException {
        quotaService.disableBucketQuota(session, name);
    }

    @PostMapping(value = "/quotas/bucket/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_SYNC)
    public void syncBucketQuota(ScmOmSession session, @PathVariable("name") String name)
            throws ScmInternalException {
        quotaService.syncBucketQuota(session, name);
    }

    @PostMapping(value = "/quotas/bucket/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_CANCEL_SYNC)
    public void cancelSyncBucketQuota(ScmOmSession session, @PathVariable("name") String name)
            throws ScmInternalException {
        quotaService.cancelSyncBucketQuota(session, name);
    }

    @GetMapping(value = "/quotas/bucket/{name}")
    public OmBucketQuotaInfo getBucketQuota(ScmOmSession session, @PathVariable("name") String name)
            throws ScmInternalException {
        return quotaService.getBucketQuota(session, name);
    }

}
