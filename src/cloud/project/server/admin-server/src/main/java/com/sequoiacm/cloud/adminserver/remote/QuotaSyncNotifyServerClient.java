package com.sequoiacm.cloud.adminserver.remote;

import com.sequoiacm.common.CommonDefine;
import org.bson.BSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/internal/v1")
public interface QuotaSyncNotifyServerClient {

    @PostMapping(value = "/quotas/{type}/{name}" + "?action="
            + CommonDefine.RestArg.QUOTA_ACTION_BEGIN_SYNC)
    public BSONObject beginSync(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_EXPIRE_TIME) long expireTime) throws Exception;

    @PostMapping(value = "/quotas/{type}/{name}" + "?action="
            + CommonDefine.RestArg.QUOTA_ACTION_SET_AGREEMENT_TIME)
    public void setAgreementTime(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_AGREEMENT_TIME) long agreementTime,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber)
            throws Exception;

    @PostMapping(value = "/quotas/{type}/{name}" + "?action="
            + CommonDefine.RestArg.QUOTA_ACTION_CANCEL_SYNC)
    public void cancelSync(@PathVariable("type") String type, @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber)
            throws Exception;

    @PostMapping(value = "/quotas/{type}/{name}" + "?action="
            + CommonDefine.RestArg.QUOTA_ACTION_FINISH_SYNC)
    public void finishSync(@PathVariable("type") String type, @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber)
            throws Exception;

    @PostMapping(value = "/quotas/{type}/{name}" + "?action="
            + CommonDefine.RestArg.QUOTA_FLUSH_CACHE)
    void flushQuotaCache(@PathVariable("type") String type, @PathVariable("name") String name);
}
