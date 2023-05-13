package com.sequoiacm.contentserver.service;

import com.sequoiacm.exception.ScmServerException;

public interface QuotaSyncMsgNotifyService {
    long notifyBeginSync(String type, String name, int syncRoundNumber, int quotaRoundNumber,
            long expireTime) throws ScmServerException;

    void notifySetAgreementTimeMsg(String type, String name, long agreementTime,
            int syncRoundNumber, int quotaRoundNumber) throws ScmServerException;

    void notifyCancelSync(String type, String name, int syncRoundNumber, int quotaRoundNumber)
            throws ScmServerException;

    void notifyFinishSync(String type, String name, int syncRoundNumber, int quotaRoundNumber)
            throws ScmServerException;
}
