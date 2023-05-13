package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.service.QuotaStatisticsService;
import com.sequoiacm.contentserver.service.QuotaSyncMsgNotifyService;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalQuotaController {

    @Autowired
    private QuotaSyncMsgNotifyService quotaSyncMsgNotifyService;

    @Autowired
    private QuotaStatisticsService quotaStatisticsService;

    @Autowired
    private BucketQuotaManager bucketQuotaManager;

    @PostMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_BEGIN_SYNC)
    public BSONObject beginSync(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_EXPIRE_TIME) long expireTime)
            throws ScmServerException {
        long currentTime = quotaSyncMsgNotifyService.notifyBeginSync(type, name, syncRoundNumber,
                quotaRoundNumber, expireTime);
        return new BasicBSONObject(CommonDefine.RestArg.QUOTA_SYNC_CURRENT_TIME, currentTime);
    }

    @PostMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_SET_AGREEMENT_TIME)
    public void setAgreementTime(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_AGREEMENT_TIME) long agreementTime,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber)
            throws ScmServerException {
        quotaSyncMsgNotifyService.notifySetAgreementTimeMsg(type, name, agreementTime,
                syncRoundNumber, quotaRoundNumber);
    }

    @PostMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_CANCEL_SYNC)
    public void cancelSync(@PathVariable("type") String type, @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber)
            throws ScmServerException {
        quotaSyncMsgNotifyService.notifyCancelSync(type, name, syncRoundNumber, quotaRoundNumber);
    }

    @PostMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_FINISH_SYNC)
    public void finishSync(@PathVariable("type") String type, @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber,
            @RequestParam(CommonDefine.RestArg.QUOTA_ROUND_NUMBER) int quotaRoundNumber)
            throws ScmServerException {
        quotaSyncMsgNotifyService.notifyFinishSync(type, name, syncRoundNumber, quotaRoundNumber);
    }

    @PostMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_STATISTICS)
    public void quotaStatistics(@PathVariable("type") String type,
            @PathVariable("name") String name,
            @RequestParam(CommonDefine.RestArg.QUOTA_SYNC_ROUND_NUMBER) int syncRoundNumber)
            throws ScmServerException {
        quotaStatisticsService.doStatistics(type, name, syncRoundNumber);
    }

    @GetMapping(value = "/quotas/{type}/{name}", params = "action="
            + CommonDefine.RestArg.QUOTA_ACTION_GET_QUOTA_INNER_DETAIL)
    public BSONObject getQuotaInnerDetail(@PathVariable("type") String type,
            @PathVariable("name") String name) throws ScmServerException {
        return bucketQuotaManager.getQuotaLimiterInfo(type, name);
    }

}
