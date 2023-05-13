package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.msg.BeginSyncMsg;
import com.sequoiacm.contentserver.quota.msg.CancelSyncMsg;
import com.sequoiacm.contentserver.quota.msg.FinishSyncMsg;
import com.sequoiacm.contentserver.quota.msg.SetAgreementTimeMsg;
import com.sequoiacm.contentserver.service.QuotaSyncMsgNotifyService;
import com.sequoiacm.exception.ScmServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuotaSyncMsgNotifyServiceImpl implements QuotaSyncMsgNotifyService {
    @Autowired
    private BucketQuotaManager quotaManager;

    @Override
    public long notifyBeginSync(String type, String name, int syncRoundNumber, int quotaRoundNumber,
            long expireTime) throws ScmServerException {
        checkType(type);
        quotaManager.handleMsg(
                new BeginSyncMsg(type, name, syncRoundNumber, quotaRoundNumber, expireTime));
        return System.currentTimeMillis();
    }

    @Override
    public void notifySetAgreementTimeMsg(String type, String name, long agreementTime,
            int syncRoundNumber, int quotaRoundNumber) throws ScmServerException {
        checkType(type);
        long currentTime = System.currentTimeMillis();
        if (currentTime < agreementTime) {
            throw new ScmInvalidArgumentException(
                    "currentTime is less than agreementTime:currentTime=" + currentTime
                            + ", agreementTime=" + agreementTime);
        }
        quotaManager.handleMsg(new SetAgreementTimeMsg(type, name, syncRoundNumber,
                quotaRoundNumber, agreementTime));
    }

    @Override
    public void notifyCancelSync(String type, String name, int syncRoundNumber,
            int quotaRoundNumber) throws ScmServerException {
        checkType(type);
        quotaManager.handleMsg(new CancelSyncMsg(type, name, syncRoundNumber, quotaRoundNumber));
    }

    @Override
    public void notifyFinishSync(String type, String name, int syncRoundNumber,
            int quotaRoundNumber) throws ScmServerException {
        checkType(type);
        quotaManager.handleMsg(new FinishSyncMsg(type, name, syncRoundNumber, quotaRoundNumber));
    }

    private void checkType(String type) throws ScmInvalidArgumentException {
        if (!BucketQuotaManager.QUOTA_TYPE.equals(type)) {
            throw new ScmInvalidArgumentException("type is not supported: type=" + type);
        }
    }

}
